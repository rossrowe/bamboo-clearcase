package com.atlassian.bamboo.plugins.clearcase.repository;

import com.atlassian.bamboo.author.AuthorImpl;
import com.atlassian.bamboo.commit.Commit;
import com.atlassian.bamboo.commit.CommitFile;
import com.atlassian.bamboo.commit.CommitFileImpl;
import com.atlassian.bamboo.commit.CommitImpl;
import com.atlassian.bamboo.plugins.clearcase.ant.*;
import com.atlassian.bamboo.plugins.clearcase.utils.StringSplitter;
import com.atlassian.bamboo.plugins.clearcase.utils.ValidationException;
import com.atlassian.bamboo.repository.AbstractRepository;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.BuildRepositoryChanges;
import com.atlassian.bamboo.v2.build.BuildRepositoryChangesImpl;
import com.atlassian.bamboo.variable.VariableDefinition;
import com.atlassian.bamboo.variable.VariableDefinitionManager;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides a ClearCase repository for Bamboo. It supports building ClearCase
 * UCM projects and snapshot views from base ClearCase.
 * <p/>
 * The Repository does not support the ClearCase Web Interface.
 * <p/>
 * Item required to configure are CcRepostory project are: For moment see
 * ccRepositoryEdit.ftl file
 * <p/>
 * The following are some points on assumption / restrictions for UCM project
 * that can be built with this repository:
 * <ul>
 * <li>The UCM Project must either contain a single component or a non rooted
 * component that stores all dependencies of the project. In other words a
 * single baseline identifies a build-able release of the project, refer to
 * ClearCase UCM best practices.</li>
 * <li>The build will occur in a stream that is child of the integration stream
 * and should (no enforced) be read only. It should be left to Bamboo to update
 * this stream accordingly (ie rebase).</li>
 * <li>A new build is triggered when the latest baseline on the integrations
 * stream differs from the build stream.</li>
 * <li>For base ClearCase, use a snapshot view. The view path specified in the
 * plugin UI will be concatenated with the VOB directory (also specified in the
 * UI). The resulting path will then be monitored for changes in the specified
 * branch.</li>
 * </ul>
 *
 * @author Stuart Hamill
 * @author Ross Rowe
 * @author Thobias Bergqvist
 * @author Magnus Grimsell
 */
public class CcRepository extends AbstractRepository {

    private static final long serialVersionUID = -2384490803230324481L;

    public static final String STORAGE_GLOBAL_VAR = "${bamboo.view.storage}";
    private static final String CLEARTOOL_HOME = "CLEARTOOL_HOME";
    private static final String DUMMY_HOST = "dummy-host";

    public CcRepository() {
        super();
        configure();
    }

    private static final Logger log = Logger.getLogger(CcRepository.class);

    // ---------------- Constants ---------------------------

    public static final String FMT_OWNER = "%[owner]p";

    /**
     * Get a ClearCase date time of the operation or event
     */
    public static final String FMT_CDATE = "%Nd";

    public static final String FMT_PROJ_INT_STREAM = "%[istream]Xp";

    public static final String NAME = "ClearCase";

    private static final String REPO_PREFIX = "custom.repository.cc.";

    public static final String CC_TYPE = REPO_PREFIX + "clearcaseType";
    public static final String CC_TYPE_UCM = "UCM";
    public static final String CC_TYPE_BASE = "Base";
    public static final String CC_PROJECT = REPO_PREFIX + "projectName";
    public static final String CC_INT_STREAM = REPO_PREFIX + "intStream";
    public static final String CC_BUILD_STREAM = REPO_PREFIX + "buildStream";
    public static final String CC_VIEW_LOCATION = REPO_PREFIX + "viewLocation";
    // Config params for ClearCase base
    public static final String CC_BASE_VIEW_LOCATION = REPO_PREFIX
            + "base.viewLocation";
    public static final String CC_BASE_VOB_DIR = REPO_PREFIX + "base.vobDir";
    public static final String CC_BASE_VOB_PATH = REPO_PREFIX + "base.vobPath";
    public static final String CC_BRANCH = REPO_PREFIX + "base.branch";
    public static final String CC_MAIN_COMPONENT = REPO_PREFIX
            + "mainComponent";
    public static final String CC_AUTO_CREATE = REPO_PREFIX + "autocreate";
    public static final String CC_BUILD_PREFIX = REPO_PREFIX + "buildprefix";
    public static final String CC_STORAGE_DIR = REPO_PREFIX + "storagedir";
    public static final String CC_LOAD_RULES = REPO_PREFIX + "loadrules";
    public static final String CC_COMPARE_BASELINES = REPO_PREFIX
            + "compareBaselines";
    public static final String CC_DYNAMIC_VIEW = REPO_PREFIX + "dynamicView";
    public static final String CC_DRIVE_LETTER = REPO_PREFIX + "driveLetter";
    private static final String CC_VIEW_TAG = REPO_PREFIX + "viewTag";
    private static final String CC_DISABLE_UPDATE = REPO_PREFIX + "disableUpdate";

    private static final String VER7_DIFF_START = "Differences:";

    private static final String CC_DATE_FMT = "yyyyMMdd.kkmmss";

    public static final SimpleDateFormat CC_DATE_FORMAT = new SimpleDateFormat(
            CC_DATE_FMT);

    private static DateFormat CC_LSHISTORY_DATE_FORMAT_LOCAL = new SimpleDateFormat(
            "dd-MMM-yy.HH:mm:ss");

    private static DateFormat CC_LSHISTORY_DATE_FORMAT_ENGLISH = new SimpleDateFormat(
            "dd-MMM-yy.HH:mm:ss", Locale.ENGLISH);

    private static final Set<Character> ACT_DIFF_START = new HashSet<Character>();

    private static final Set<String> ACT_DIFF_START_STRINGS = new HashSet<String>();

    private transient VariableDefinitionManager variableDefinitionManager;



    private String cleartoolHome;

    static {
        ACT_DIFF_START.add('<');
        ACT_DIFF_START.add('>');
        ACT_DIFF_START.add('-');
        ACT_DIFF_START.add(' ');
        ACT_DIFF_START_STRINGS.add("<<");
        ACT_DIFF_START_STRINGS.add(">>");
        ACT_DIFF_START_STRINGS.add("->");
        ACT_DIFF_START_STRINGS.add("<-");
    }

    // ---------------- fields ---------------------------

    private CcSelector projectName = new CcSelector(CcSelector.KIND_PROJECT);

    private CcSelector intStream = new CcSelector(CcSelector.KIND_STREAM);

    private CcSelector buildStream = new CcSelector(CcSelector.KIND_STREAM);

    /**
     * The mainComponent is the component that contains the baseline to compare
     * changes against. Normally this component will be UCM non-rooted component
     * (ie it is the baseline that identifies the baselines that make a release
     * of this project).
     */
    private String mainComponent = null;
    private String viewLocation = null;
    private boolean autoCreate = false;
    private String buildPrefix;
    private String viewStorageDir = null;

    private String type = null;
    // ClearCase base related fields
    private String branch = null;
    private String baseViewLocation = null;
    private String vobDir = null;
    private String vobPath = null;

    // ------------------- ANT related task objects --------------------
    private CcDesc streamchecker = null;
    private CcDesc desc = null;
    private CcLsproject lsproj = null;

    // Decided to remove the baseline from the change summary
    // could possibly add it back as a configuration parameter.
    private boolean addBaseline = false;

    private String loadRules;

    private boolean compareBaselines;

    private boolean dynamicView;

    private String driveLetter;

    private String viewTag;

    private boolean disableUpdate;

    /**
     * Identifies and returns the list of files that have been changed since the
     * last build. If Baseline Comparisons are being performed, then the
     * <code>lastVcsRevisionKey</code> will be the baseline that was last built,
     * otherwise it will be the date/time of the last build.
     *
     *
     * @param planKey            the key for the project being built
     * @param lastVcsRevisionKey either the date/time or the baseline that was previously built
     * @return a {@link com.atlassian.bamboo.v2.build.BuildChanges} instance
     *         that includes the list of changes
     */
    @NotNull
    public BuildRepositoryChanges collectChangesSinceLastBuild(@NotNull String planKey,
                                                               @NotNull String lastVcsRevisionKey) throws RepositoryException {

        log.debug("collectChangesSinceLastBuild, planKey: " + planKey
                + " lastVcsRevisionKey: " + lastVcsRevisionKey);

        List<Commit> commits = new ArrayList<Commit>();
        String lastBuildKey = lastVcsRevisionKey;

        if (CC_TYPE_UCM.equals(getClearCaseType()) && shouldCompareBaselines()) {
            String latestBl = getLatestIntegrationBasline();

            if (!lastVcsRevisionKey.equals(latestBl)) {

                // First changeLogEntry will always just be the baseline
                // that the build is on.
                // Perhaps it should also contain the build it can from....
                if (addBaseline) {
                    CommitImpl cl = new CommitImpl();
                    cl.setAuthor(new AuthorImpl(getDescAttribute(latestBl,
                            FMT_OWNER)));
                    cl.setComment("Updating to " + latestBl);
                    cl.setDate(getObjectDate(latestBl));
                    commits.add(cl);
                }
                // check to see if the last build was using date format
                if (lastBuildKey.matches("[0-9]+\\.[0-9]+$")) {
                    // just use latest baseline
                    lastBuildKey = latestBl;
                }

                // now add the activity / element baseline change entries
                addChangeSummary(lastBuildKey, latestBl, commits, planKey);
            }

            return new BuildRepositoryChangesImpl(latestBl, commits);
        } else {
            return compareDateChanges(lastVcsRevisionKey, commits, planKey);
        }
    }

    /**
     * Compares the changes made to the ClearCase view since the date/time
     * represented by the <code>lastVcsRevisionKey</code>.
     *
     * @param lastVcsRevisionKey The date/time the last build occurred
     * @param commits
     * @param planKey            the key for the project being built
     * @return a {@link com.atlassian.bamboo.v2.build.BuildChanges} instance
     *         that includes the list of changes
     * @throws RepositoryException if a {#link ParseException} is encountered when parsing the
     *                             <code>lastVcsRevisionKey</code>
     */
    private BuildRepositoryChanges compareDateChanges(String lastVcsRevisionKey,
                                            List<Commit> commits, String planKey) throws RepositoryException {
        Date since;
        try {
            if (lastVcsRevisionKey == null || lastVcsRevisionKey.startsWith("baseline")) {
                // the build was previously using 'Compare Baselines'
                // there doesn't seem to be an easy way to find the last built
                // date, so use today's date
                since = new Date();
            } else {
                since = CC_DATE_FORMAT.parse(lastVcsRevisionKey);
            }
        } catch (ParseException e) {
            try {
                // try to parse date with current locale
                // This should only be used when updating cc-plugin
                // old plugin used language locale for revision keys
                // new plugin use numeric representation
                since = CC_LSHISTORY_DATE_FORMAT_LOCAL
                        .parse(lastVcsRevisionKey);
            } catch (ParseException pe) {
                throw new RepositoryException("Failed to parse revision key "
                        + lastVcsRevisionKey + ": " + pe.getMessage());
            }
        }
        List<String> latestChanges = getLatestChanges(
                CC_LSHISTORY_DATE_FORMAT_ENGLISH.format(since), planKey);
        // Something has changed in the branch since last time.
        if (latestChanges.size() > 0) {
            String newRevisionKey = parseRevisionKey(latestChanges.get(0));
            addChangeSummary(latestChanges, commits);
            return new BuildRepositoryChangesImpl(newRevisionKey, commits);
        }
        // No changes...
        return new BuildRepositoryChangesImpl(lastVcsRevisionKey, commits);
    }

    /**
     * Gets the date of the latest change in the ClearCase base repository.
     *
     * @param revKey
     * @return
     * @throws RepositoryException
     */
    private String parseRevisionKey(String revKey) throws RepositoryException {
        String[] changes = revKey.split(ClearCaseUtils.LSHISTORY_DELIM);
        String latestChange = changes[ClearCaseUtils.LSHISTORY_DATE];

        log.debug("Latest change occured: " + latestChange);

        // Add 1 second to the date because otherwise the same latest change
        // in the repository will be returned again and again.
        Date date = null;
        try {
            date = CC_DATE_FORMAT.parse(latestChange);
        } catch (ParseException e) {
            log.error("Cannot parse " + latestChange);
            throw new RepositoryException("Cannot parse latest change date: "
                    + e.getMessage());
        }

        long millis = date.getTime();
        date = new Date(millis + 1000);
        return CC_DATE_FORMAT.format(date);
    }

    /**
     * Collects the latest changes in a ClearCase base view.
     *
     * @param since   String representation of the date/time to compare
     * @param planKey the key for the project being built
     * @return list of the changes
     */
    private List<String> getLatestChanges(String since, String planKey) {
        if (log.isDebugEnabled()) {
            log.debug("getLatestChanges since: " + since + " in branch: "
                    + getBranch() + " in VOB dir: " + getVobPath());
        }

        List<String> changes;
        if (StringUtils.isNotEmpty(getVobDir())) {
            changes = ClearCaseUtils.getLatestChanges(since, getBranch(),
                    getVobPath(), getDummyProject(), getCleartoolHome());
        } else {
            // process the child directories beneath the snapshot view location
            log.debug("Searching for changes in child directories of: " + getViewLocation(planKey));
            File snapshotLocation = new File(getViewLocation(planKey));
            File[] subdirs = snapshotLocation.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });
            changes = new ArrayList<String>();
            if (subdirs == null) {
                log.error("No subdirs found in directory: " + getViewLocation(planKey));
                return changes;
            }

            for (File subdirectory : subdirs) {
                try {
                    changes.addAll(ClearCaseUtils.getLatestChanges(since,
                            getBranch(), subdirectory.getAbsolutePath(),
                            getDummyProject(), getCleartoolHome()));
                } catch (BuildException e) {
                    // directory might not be under ClearCase control, attempt
                    // to continue
                    log.error("Error processing directory: "
                            + subdirectory.getAbsolutePath());
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Changes in branch [" + getBranch() + "] has count["
                    + changes.size() + "]");
        }
        return changes;
    }

    /**
     * Add all activities and file version to the change list.
     *
     * @param changeList
     * @param commits
     * @throws RepositoryException
     */
    void addChangeSummary(List<String> changeList, List<Commit> commits)
            throws RepositoryException {
        log.debug("Entering addChangeSummary for ClearCase base");
        CommitImpl commit = null;
        for (String line : changeList) {
            String[] event = line.split(ClearCaseUtils.LSHISTORY_DELIM);
            commit = new CommitImpl();
            commit.setAuthor(new AuthorImpl(
                    event[ClearCaseUtils.LSHISTORY_USER]));

            // If the size is not right, it must mean there was no comment in
            // the ClearCase
            // checkin. In that case we won't try to parse the comment.
            if (event.length == ClearCaseUtils.LSHISTORY_RESULT_SIZE) {
                commit.setComment(event[ClearCaseUtils.LSHISTORY_COMMENT]);
            }

            try {
                Date date = CC_DATE_FORMAT
                        .parse(event[ClearCaseUtils.LSHISTORY_DATE]);
                commit.setDate(date);
            } catch (ParseException e) {
                log.error("Could not parse date: " + event[0], e);
            }
            commit.addFile(new CommitFileImpl(
                    event[ClearCaseUtils.LSHISTORY_FILE]));
            commits.add(commit);
        }
        log.debug("Exiting addChangeSummary for ClearCase base");
    }

    @NotNull
    public String retrieveSourceCode(@NotNull BuildContext buildContext, @Nullable String vcsRevisionKey) throws RepositoryException {
		return retrieveSourceCode(buildContext.getPlanKey(), vcsRevisionKey);
	}

    /**
     * Performs the required processing in order to pull down the source code
     * from the ClearCase repository.
     *
     * @param planKey        the key for the project being built
     * @param vcsRevisionKey either the date/time or the baseline that was previously built
     * @return String representing either the baseline that is being built
     * @throws RepositoryException if an error occurs during the retrieval of source code
     */
    @NotNull
    public String retrieveSourceCode(@NotNull String planKey,
                                     String vcsRevisionKey) throws RepositoryException {
        log.debug("retrieveSourceCode, planKey: " + planKey
                + " vcsRevisionKey: " + vcsRevisionKey);

        if (CC_TYPE_UCM.equals(getClearCaseType())) {
            String baselineToUse = vcsRevisionKey;
            createStreamAndView(planKey);
            if (shouldCompareBaselines()) {
                baselineToUse = getLatestIntegrationBasline();
                checkLoadRules(baselineToUse, planKey);
                rebaseBuild(baselineToUse, planKey);
            } else {
                String lastChange = CC_DATE_FORMAT.format(new Date());
                if (!isDynamicView()) {
                    log.debug("Updating view...");
                    checkLoadRules(null, planKey);
                    updateView(getViewLocation(planKey));
                    log.debug("View updated.");
                }
                return lastChange;
            }
            return baselineToUse;
        } else {
            String lastChange = CC_DATE_FORMAT.format(new Date());
            if (!isDynamicView()) {
                log.debug("Updating view...");
                updateView(getBaseViewLocation());
                log.debug("View updated.");
            }
            return lastChange;

        }
    }

    /**
     * Updates the ClearCase base snapshot view.
     *
     * @param path The location of the snapshot view to update
     * @throws RepositoryException thrown if an error occurs during the update of the snapshot
     *                             view.
     */
    private void updateView(String path) throws RepositoryException {
        if (!isDisableUpdate()) {
            CcUpdateSnapshot update = new CcUpdateSnapshot();
            update.setViewPath(path);
            update.setProject(getDummyProject());
            cmdRunner(update);
        }
    }

    /**
     * Check out the latest revision of the code.
     *
     * @param planKey
     * @throws RepositoryException thrown if an error occurs during the create stream/view
     *                             operation.
     */
    private void createStreamAndView(String planKey) throws RepositoryException {
        // will create stream and view if required.
        CcStreamCreator sc = new CcStreamCreator();
        sc.setProject(getDummyProject());
        sc.setIntStream(getIntStream());
        sc.setStreamName(getBuildStreamAsSelector());
        sc.setViewLocation(getViewLocation(planKey));
        sc.setViewTag(getViewTagToUse());
        sc.setVws(getBuildViewStorageDir());
        sc.setDynamicView(isDynamicView());
        sc.setDriveLetter(getDriveLetter());
        cmdRunner(sc);
    }

    private String getViewTagToUse() {
        if (!StringUtils.isBlank(getViewTag())) {
            return getViewTag();
        } else {
            return buildStream.getName();
        }
    }

    /**
     * Returns the view location to be used for the project build.
     * <p/>
     * If the build is a dynamic view, then returns a {@link File} instance for
     * the drive letter mapping.
     *
     * @param planKey
     * @return
     */
    public String getViewLocation(String planKey) {
        if (isDynamicView()) {
            return getViewLocation();
        }

        if (isAutoCreate()) {
            File parentDirectory = new File(getViewLocation(), planKey);
            if (!parentDirectory.exists()) {
                // ensure that parent directories exist
                parentDirectory.mkdirs();
            }
            return new File(parentDirectory, buildStream.getName())
                    .getAbsolutePath();
        } else {
            return getViewLocation();
        }
    }

    /**
     * Wraps the executions of ant task an turns a BuildExpeption in Repository
     * Exception.
     *
     * @param cmd The ANT task to run.
     * @throws com.atlassian.bamboo.repository.RepositoryException
     *
     */
    private void cmdRunner(CleartoolCommand cmd) throws RepositoryException {
        try {
            String cleartoolHome = getCleartoolHome();
            if (StringUtils.isNotEmpty(cleartoolHome))
                cmd.setCleartoolHome(cleartoolHome);
            cmd.execute();
        } catch (BuildException be) {
            String msg = "Execution of " + cmd.getTaskName() + " + failed";
            log.info(msg + ", wrapping in RepositoryException", be);
            throw new RepositoryException(msg, be);
        }
    }

    private String getCleartoolHome() {
        return cleartoolHome;
    }

    /**
     * Test if build stream already exists.
     *
     * @param streamSelector the ClearCase stream selector
     * @return the fully qualified stream selected if the stream existed, may be
     *         different to the string passed.
     * @throws ValidationException if the stream is does not exist or is badly formed
     */
    private String streamExists(String streamSelector)
            throws ValidationException {

        CcSelector stream = new CcSelector(CcSelector.KIND_STREAM,
                streamSelector);
        streamchecker.setObjSelect2(stream.asSelector());
        try {
            streamchecker.execute();

            String quailifiedStream = streamchecker.getCommandOutput();
            if (StringUtils.isEmpty(quailifiedStream)) {
                throw new ValidationException(
                        "The stream did not exist, name returned was empty.");
            }
            return quailifiedStream;
        } catch (BuildException be) {
            String cmdOutput = streamchecker.getCommandOutput();
            String msg = "Stream lookup failed for [" + streamSelector
                    + "]. ClearTool Ouput [" + cmdOutput + "]";
            log.debug(msg, be);
            throw new ValidationException(msg, be);
        }
    }

    /**
     * Setup a dummy and project and CC desc command.
     */
    private void configure() {
        if (streamchecker == null) {
            streamchecker = new CcDesc();
            streamchecker.setProject(getDummyProject());
            streamchecker.setFormat("%Xn");
        }
        if (desc == null) {
            desc = new CcDesc();
            desc.setProject(getDummyProject());
        }
        if (lsproj == null) {
            lsproj = new CcLsproject();
            lsproj.setProject(getDummyProject());
        }

    }

    /**
     * @return an instance of {#link Project } to be used as part of the
     *         ClearCase operations
     */
    private Project getDummyProject() {
        return ClearCaseUtils.getAntProject();
    }

    /**
     * Ensure the build view contain load rules that includes all directories
     * required by the baseline.
     *
     * @param latestBl
     * @param planKey
     */
    private void checkLoadRules(String latestBl, String planKey) {
        if (isAutoCreate()) {
            CcUpdateLoadRules ulr = new CcUpdateLoadRules();
            ulr.setProject(getDummyProject());
            ulr.setExplicitLoadRules(getLoadRules());
            ulr.setViewPath(getViewLocation(planKey));
            ulr.setStream(getBuildStreamAsSelector());
            ulr.setBaseline(latestBl);
            ulr.setCleartoolHome(getCleartoolHome());
            ulr.execute();
        }
    }

    private String getBuildStreamAsSelector() {
        return buildStream.asSelector();
    }

    /**
     * Add all activities and file version to the change list.
     *
     * @param currentBl
     * @param latestBl
     * @param changeList
     * @param planKey
     * @throws RepositoryException
     */
    void addChangeSummary(String currentBl, String latestBl,
                          List<Commit> changeList, String planKey) throws RepositoryException {

        String diffBlText = runDiffBl(latestBl, currentBl, planKey);
        // test if using ClearCase version 7 output format
        int version7StartPos = diffBlText.indexOf(VER7_DIFF_START);
        if (version7StartPos != -1) {
            // skip over ClearCase version 7 guff at start of output
            diffBlText = diffBlText.substring(version7StartPos
                    + VER7_DIFF_START.length());
        }

        StringSplitter sp = new StringSplitter(diffBlText);
        sp.setIgnoreEmptyToken(true);
        List<String> lines = sp.getItems();

        Commit commit = null;
        for (String line : lines) {
            if (isActivity(line)) {
                commit = addCommitLogEntry(line, changeList);
            } else {
                // we are process a change version for the last activity
                if (!StringUtils.isEmpty(line)) {
                    addCommitFile(line, commit);
                }
            }
        }
    }

    /**
     * Process the activity version line from the diffbl command and add the
     * ChangeLogFile entry to the passed changeLogEntry.
     *
     * @param line   the diffbl version line
     * @param commit
     */
    private void addCommitFile(String line, Commit commit) {
        if (commit == null)
            return;
        List<CommitFile> files = getCommitFiles(commit);
        log.debug("addChangeLogFile: line " + line);
        if (commit == null) {
            log
                    .info("The changelog was null this is probably indicating a bug. line["
                            + line + "]");
        }
        line = line.trim();
        String[] parts = line.split("@@");
        if (parts.length < 2) {
            // oops must have made bad assumption about format of output.
            log.warn("Unable to process version line [" + line + "]");
            // TODO maybe setup change file with the line in it.....
            return;
        }
        String element = parts[0];
        if (element.toLowerCase().startsWith(getViewLocation().toLowerCase())) {
            element = element.substring(getViewLocation().length());
        }
        String version = parts[1];

        CommitFileImpl commitFile = new CommitFileImpl(element);
        commitFile.setRevision(version.substring(0, Math.min(version.length(), 254)));
        files.add(commitFile);
        commit.setFiles(files);
    }

    /**
     * @param commit
     * @return
     */
    private List<CommitFile> getCommitFiles(Commit commit) {
        if (commit.getFiles() == null)
            commit.setFiles(new ArrayList<CommitFile>());
        return commit.getFiles();
    }

    /**
     * Process a diffbl activity line and create the ChangeLogEntry.
     *
     * @param line       the diffbl activity line.
     * @param changeList the change list the new change log entry will be added to
     * @return the change log entry created or null if unable to process
     * @throws RepositoryException if the call to cleartool to look up activity attributes fails
     */
    private Commit addCommitLogEntry(String line, List<Commit> changeList)
            throws RepositoryException {
        log.debug("addChangeLogEntry: " + line);

        StringBuffer actId = new StringBuffer();
        StringBuffer actHeadline = new StringBuffer();
        int state = 0;
        for (char x : line.toCharArray()) {
            switch (state) {
                case 0: // ignore leading >> or << or <- >- characters
                {
                    if (ACT_DIFF_START.contains(x)) {
                        // ignore
                    } else {
                        state++;
                        actId.append(x);
                    }
                }
                break;
                case 1: // process activityId
                {
                    if (x == ' ') {
                        state++;
                    } else {
                        actId.append(x);
                    }
                }
                break;
                default: // now process the activity headline.
                {
                    actHeadline.append(x);
                }
                break;
            }
        }
        String activityId = actId.toString();
        if (activityId.indexOf("@") == -1)
            activityId = activityId + getVob();
        CcSelector actIdSel = null;
        try {
            actIdSel = new CcSelector(CcSelector.KIND_ACTIVITY, activityId);
        } catch (ValidationException e) {
            log.error("Bad  activity selector " + actId.toString());
            // TODO perhaps throw Repository exception...
            throw new RepositoryException("Bad  activity selector "
                    + actId.toString(), e);
        }

        log.debug("addChangeLogEntry: actId=" + actId + " actHead="
                + actHeadline);

        CommitImpl commit = new CommitImpl();
        commit.setComment(actId.toString());
        String owner = getDescAttribute(actIdSel.asSelector(), FMT_OWNER);
        commit.setAuthor(new AuthorImpl(owner.toLowerCase()));
        commit.setDate(getObjectDate(actIdSel.asSelector()));
        changeList.add(commit);
        return commit;
    }

    /**
     * Test if the diffbl output line is an activity. If it not an activity then
     * it change version.
     *
     * @param line the line being tested
     * @return true if it is activity entry
     */
    private boolean isActivity(String line) {
        if (line == null) {
            return false;
        }
        line = line.trim();
        return ACT_DIFF_START_STRINGS.contains(line.substring(0, 2));
    }

    /**
     * Runs a 'cleartool diffbl' command to retrieve the differences between two
     * baselines.
     *
     * @param latestBl
     * @param currentBl
     * @param planKey
     * @return
     * @throws RepositoryException
     */
    private String runDiffBl(String latestBl, String currentBl, String planKey)
            throws RepositoryException {
        CcDiffbl diffbl = new CcDiffbl();
        diffbl.setProject(getDummyProject());
        String viewPath = getViewLocation(planKey);
        diffbl.setViewPath(viewPath);
        diffbl.setSelector1(latestBl);
        diffbl.setSelector2(currentBl);
        diffbl.setActivities(true);
        diffbl.setVersions(true);
        log.debug("About to run diffbl in:" + viewPath);

        cmdRunner(diffbl);

        return diffbl.getCommandOutput();
    }

    /**
     * Rebase the stream the build stream to the specified baseline.
     *
     * @param baseline
     * @param planKey
     * @throws RepositoryException if the rebase fails
     */
    private void rebaseBuild(String baseline, String planKey)
            throws RepositoryException {
        CcRebase rebase = new CcRebase();
        rebase.setProject(getDummyProject());
        rebase.setViewPath(getViewLocation(planKey));
        rebase.setBaseline(baseline);
        rebase.setStream(getBuildStreamAsSelector());
        rebase.setView(buildStream.getName());

        cmdRunner(rebase);
        if (log.isDebugEnabled()) {
            // log the output if debug is enabled
            log.debug("Rebase to " + baseline + " output:\n"
                    + rebase.getCommandOutput());
        }
    }

    /**
     * @return
     */
    private String getLatestIntegrationBasline() throws RepositoryException {
        String rval = null;
        List<String> baselines = ClearCaseUtils.getStreamBaselines(
                getIntStream(), mainComponent, getDummyProject(),
                getCleartoolHome());
        if (log.isDebugEnabled()) {
            log.debug("Stream [" + getIntStream() + "] has count["
                    + baselines.size() + "] baselines [" + baselines + "]");
        }
        if (baselines.isEmpty())
            throw new RepositoryException(
                    "No Integration Baselines found, please ensure that a baseline has been created on the integration stream");
        rval = baselines.get(baselines.size() - 1);
        return rval;
    }



    /**
     * @return 'dummy host' - this isn't used by the ClearCase logic, but is used by the {@link com.atlassian.bamboo.v2.ww2.build.TriggerRemoteBuild}
     *         logic, which will fail if the host is set to 'unknown-host'
     */
    public String getHost() {
        return DUMMY_HOST;
    }

    /**
     * @return
     */
    @NotNull
    public String getName() {
        return NAME;
    }

    /**
     * The build view directory specified as part of the project configuration.
     */
    @NotNull
    public File getSourceCodeDirectory(@NotNull String key)
            throws RepositoryException {
        File sourceDirectory;
        if (isDynamicView()) {
            sourceDirectory = new File(getViewLocation());
        } else if (CC_TYPE_UCM.equals(getClearCaseType())) {
            sourceDirectory = new File(getViewLocation());
            if (isAutoCreate())
                sourceDirectory = new File(sourceDirectory, key);
        } else
            sourceDirectory = new File(getBaseViewLocation(), getVobDir());
        return sourceDirectory;
    }

    /**
     * Current always indicates the repository is the same.
     */
    public boolean isRepositoryDifferent(@NotNull Repository repository) {
        log.debug("isRepositoryDifferent  repository[" + repository.toString()
                + "]");
        // TODO Currently always indicates no..... perhaps this needs
        // revisiting.
        return false;
    }

    /**
     * Set defaults so:
     * <ul>
     * <li>auto create is true</li>
     * <li>build prefix is set</li>
     * <li>view storage is read from global setting.</li>
     * <li>ClearCase is UCM</li>
     * <li>
     * Auto create is true</li>
     * <li>Build prefix is set</li>
     * <li>View storage is read from global setting.</li>
     * </ul>
     */
    @Override
    public void addDefaultValues(@NotNull BuildConfiguration buildConfiguration) {
        buildConfiguration.setProperty(CC_TYPE, CC_TYPE_UCM);
        buildConfiguration.setProperty(CC_AUTO_CREATE, "true");
        buildConfiguration.setProperty(CC_BUILD_PREFIX, "build_ro_");
        buildConfiguration.setProperty(CC_STORAGE_DIR, STORAGE_GLOBAL_VAR);

        log.debug("addDefaultValues BuildConfiguration["
                + buildConfiguration.asXml() + "]");

    }

    /**
     * Setup values that are mandatory but can be derived from other settings.
     * Some of the action are the Integration Stream based based on project
     * specified.
     * <p/>
     * If Auto Creating the project then the name of the Build Stream and view
     * are always set replacing current values if they exist.
     */
    public void prepareConfigObject(
            @NotNull BuildConfiguration buildConfiguration) {
        log.debug("prepareConfigObject buildConfiguration["
                + buildConfiguration.asXml() + "]");

        String project = buildConfiguration.getString(CC_PROJECT);
        if (StringUtils.isEmpty(project)) {
            // exit as minimum setting has not been specified.
            return;
        }
        if (StringUtils.isBlank(buildConfiguration.getString(CC_INT_STREAM))) {
            String intStream = determineIntStream(project);
            log.debug("prepareConfigObject: setting integration stream to: "
                    + intStream);
            buildConfiguration.setProperty(CC_INT_STREAM, intStream);
        }

        // when auto create is true then setup build stream name
        if (buildConfiguration.getBoolean(CC_AUTO_CREATE)) {
            String prefix = buildConfiguration.getString(CC_BUILD_PREFIX);
            if (StringUtils.isNotBlank(prefix)) {
                try {
                    // setup the build stream name
                    CcSelector projSelector = new CcSelector(null, project);
                    CcSelector stream = new CcSelector(null, project);
                    stream.setKind(CcSelector.KIND_STREAM);
                    stream.setName(prefix + projSelector.getName());
                    buildConfiguration.setProperty(CC_BUILD_STREAM, stream
                            .asSelector());

                    // setup the build View Location
                    String viewLoc = getWorkingDirectory().getAbsolutePath();
                    buildConfiguration.setProperty(CC_VIEW_LOCATION, viewLoc);

                } catch (ValidationException e) {
                    log
                            .debug(
                                    "Project selector was not well formed so not configuring",
                                    e);
                }
            }
        }

    }

    /**
     * Use 'lsproject' to determine integration stream of project.
     *
     * @param project the clear case UCM project.
     * @return the integration stream of the project or null if it could not be
     *         determined.
     */
    private String determineIntStream(String project) {
        String rval = null;
        try {
            CcSelector projSelector = new CcSelector(CcSelector.KIND_PROJECT,
                    project);
            lsproj.setFormat(FMT_PROJ_INT_STREAM);
            lsproj.setObjSelect2(projSelector.asSelector());

            lsproj.execute();
            rval = lsproj.getCommandOutput();
        } catch (ValidationException ve) {
            log.debug("Failed to determine integration stream for [" + project
                    + "], ValidationExcpetion", ve);
        } catch (BuildException be) {
            log.debug("Failed to determine integration stream for [" + project
                    + "], BuildException ", be);
        }

        return rval;
    }

    /**
     * Validation entered project details to ensure the minimum entries are
     * present.
     */
    @NotNull
    @Override
    public ErrorCollection validate(
            @NotNull BuildConfiguration buildConfiguration) {
        // was throwing class cast when calling asXml().
        // log.debug("validate
        // buildConfiguration["+buildConfiguration.asXml()+"]");
        log.debug("validate ");
        ErrorCollection errorCollection = super.validate(buildConfiguration);
        String buildConfigName = null;

        if (CC_TYPE_UCM.equals(buildConfiguration.getString(CC_TYPE))) {
            // check the Project name
            String projectSelector = buildConfiguration.getString(CC_PROJECT);
            validateProject(projectSelector, CC_PROJECT, errorCollection);

            String component = buildConfiguration.getString(CC_MAIN_COMPONENT);
            if (StringUtils.isEmpty(component)) {
                errorCollection.addError(CC_MAIN_COMPONENT,
                        "You must enter a main component");
            }
            try {
                new CcSelector("component", component);
                // buildConfiguration.setProperty(CC_MAIN_COMPONENT,
                // selector.asSelector());
                // TODO check if component actually exists
            } catch (ValidationException ve) {
                log.info("The mainComponent [" + component
                        + "] failed validation", ve);
                errorCollection.addError(CC_MAIN_COMPONENT, ve.getMessage());
            }

            // validate Integration Stream has been entered and exists.
            validateStreamExists(buildConfiguration.getString(CC_INT_STREAM),
                    CC_INT_STREAM, errorCollection);

            boolean autoCreating = buildConfiguration
                    .getBoolean(CC_AUTO_CREATE);
            if (autoCreating) {
                // only check the required setting are present but the
                if (StringUtils.isBlank(buildConfiguration
                        .getString(CC_BUILD_PREFIX))) {
                    errorCollection
                            .addError(CC_BUILD_PREFIX,
                                    "The build prefix must be specified and can not be spaces.");
                }

                // must be entered exist and be accessible.
                String storageLoc = buildConfiguration
                        .getString(CC_STORAGE_DIR);
                storageLoc = substituteGlobalVariables(storageLoc);
                validateFileName(storageLoc, errorCollection, CC_STORAGE_DIR);

            } else {

                // validate build Stream
                validateStreamExists(buildConfiguration
                        .getString(CC_BUILD_STREAM), CC_BUILD_STREAM,
                        errorCollection);

                // validate the viewLocation if present.
                String viewLocation = buildConfiguration
                        .getString(CC_VIEW_LOCATION);
                if (viewLocation != null) {
                    validateFileName(viewLocation, errorCollection,
                            CC_VIEW_LOCATION);
                }
            }

        } else {

            // Validate the view location and VOB directory for ClearCase Base
            String viewLocation = buildConfiguration
                    .getString(CC_BASE_VIEW_LOCATION);
            String vobdir = buildConfiguration.getString(CC_BASE_VOB_DIR);
            buildConfigName = viewLocation;
            if (StringUtils.isBlank(viewLocation)) {
                errorCollection.addError(CC_BASE_VIEW_LOCATION,
                        "The view location must be specified");
            } else if (StringUtils.isBlank(vobdir)) {
                errorCollection.addError(CC_BASE_VOB_DIR,
                        "The VOB directory must be specified");
            } else {
                File file = new File(viewLocation);
                if (!file.exists() || !file.isDirectory()) {
                    errorCollection.addError(CC_BASE_VIEW_LOCATION,
                            "Does not exist");
                } else {
                    file = new File(viewLocation, vobdir);
                    if (!file.exists() || !file.isDirectory()) {
                        errorCollection.addError(CC_BASE_VOB_DIR,
                                "Does not exist");
                    } else {
                        try {
                            String path = file.getCanonicalPath();
                            setVobPath(path);
                            buildConfiguration.setProperty(CC_BASE_VOB_PATH,
                                    path);
                        } catch (IOException e) {
                            errorCollection.addError(CC_BASE_VOB_DIR,
                                    "Can not resolve path: " + e.getMessage());
                        }
                    }
                }
            }

            // Validate branch
            if (StringUtils.isBlank(buildConfiguration.getString(CC_BRANCH))) {
                errorCollection.addError(CC_BRANCH,
                        "The branch must be specified.");
            }
        }
        logErrors(buildConfigName, errorCollection);

        return errorCollection;
    }

    /**
     * Test if the supplied project name can be found and has been entered.
     *
     * @param projectSelector the project string to test
     * @param field           the edit page filed being validated.
     * @param errorCollection the erro collection to add errors to.
     */
    private void validateProject(String projectSelector, String field,
                                 ErrorCollection errorCollection) {
        if (StringUtils.isBlank(projectSelector)) {
            errorCollection.addError(field, "Please enter project selector");
        } else {
            lsproj.setFormat("%Xn");
            lsproj.setFailOnErr(true);
            lsproj.setObjSelect2(projectSelector);
            try {
                lsproj.execute();
            } catch (BuildException be) {
                errorCollection.addError(field,
                        "Project specified did not exist. Error: "
                                + lsproj.getCommandOutput());
            }
        }
    }

    /**
     * Validate stream exists in ClearCase repository and and record error on
     * failure.
     *
     * @param stream          the string stream to validate.
     * @param fieldKey        the field the validation applies to.
     * @param errorCollection the error collector.
     * @return true if valid, false if not.
     */
    private boolean validateStreamExists(String stream, String fieldKey,
                                         ErrorCollection errorCollection) {
        boolean rval = true;
        try {
            streamExists(stream);

        } catch (ValidationException ve) {
            errorCollection.addError(fieldKey, ve.getMessage());
            rval = false;
        }
        return rval;
    }

    /**
     * Log the validation errors if they occur.
     *
     * @param projectName     the name of project entered on the ui.
     * @param errorCollection the collection of errors.
     */
    private void logErrors(String projectName, ErrorCollection errorCollection) {
        if (errorCollection.hasAnyErrors()) {
            log.info("Validation of buildConfiguration for [" + projectName
                    + "] Errors[" + errorCollection + "]");
        }
    }

    /**
     * Does not support ClearCase Web interface.
     */
    public String getWebRepositoryUrl() {
        return null;
    }

    /**
     * Does not support ClearCase Web interface.
     */
    public String getWebRepositoryUrlForFile(CommitFile file) {
        return null;
    }

    /**
     * Does not support ClearCase Web interface.
     *
     * @return
     */
    public String getWebRepositoryUrlRepoName() {
        return null;
    }

    /**
     * Does not support ClearCase Web interface.
     *
     * @param url
     */
    public void setWebRepositoryUrl(String url) {

    }

    /**
     * Does not support ClearCase Web interface.
     *
     * @param repoName
     */
    public void setWebRepositoryUrlRepoName(String repoName) {

    }

    /**
     * Configure the ClearCaseUCM repository from the configuration passed. This
     * is called when the repository is being restored after a server restart.
     */
    @Override
    public void populateFromConfig(@NotNull HierarchicalConfiguration config) {
        log.debug("populateFromConfig config[" + config.toString() + "]");
        super.populateFromConfig(config);
        setCompareBaselines(config.containsKey(CC_COMPARE_BASELINES));
        setClearCaseType(config.getString(CC_TYPE));
        setBranch(config.getString(CC_BRANCH));
        setBaseViewLocation(config.getString(CC_BASE_VIEW_LOCATION));
        setVobDir(config.getString(CC_BASE_VOB_DIR));
        setVobPath(config.getString(CC_BASE_VOB_PATH));
        setAutoCreate(config.getBoolean(CC_AUTO_CREATE, false));
        setDynamicView(config.getBoolean(CC_DYNAMIC_VIEW, false));
        setDriveLetter(config.getString(CC_DRIVE_LETTER));
        setBuildPrefix(config.getString(CC_BUILD_PREFIX));
        setMainComponent(config.getString(CC_MAIN_COMPONENT));
        setViewLocation(config.getString(CC_VIEW_LOCATION));
        setViewStorageDir(config.getString(CC_STORAGE_DIR, STORAGE_GLOBAL_VAR));
        setLoadRules(config.getString(CC_LOAD_RULES));
        setViewTag(config.getString(CC_VIEW_TAG));
        setDisableUpdate(config.getBoolean(CC_DISABLE_UPDATE));
        try {
            setProjectName(config.getString(CC_PROJECT));
            setIntStream(config.getString(CC_INT_STREAM));
            setBuildStream(config.getString(CC_BUILD_STREAM));
        } catch (ValidationException e) {
            log
                    .error(
                            "The configure project / integration or build stream is not valid",
                            e);
        }

    }

    private void setDisableUpdate(boolean disableUpdate) {
        this.disableUpdate = disableUpdate;
    }

    public void setLoadRules(String loadRules) {
        this.loadRules = loadRules;
    }

    /**
     * Add all required configuration that must be persisted between server
     * restarts. This apprears to be called when project is created or modified.
     */
    @NotNull
    @Override
    public HierarchicalConfiguration toConfiguration() {
        log.debug("toConfiguration");
        HierarchicalConfiguration configuration = super.toConfiguration();

        configuration.setProperty(CC_TYPE, getClearCaseType());
        configuration.setProperty(CC_BASE_VIEW_LOCATION, getBaseViewLocation());
        configuration.setProperty(CC_BASE_VOB_DIR, getVobDir());
        configuration.setProperty(CC_BASE_VOB_PATH, getVobPath());
        configuration.setProperty(CC_BRANCH, getBranch());
        configuration.setProperty(CC_AUTO_CREATE, isAutoCreate());
        configuration.setProperty(CC_DYNAMIC_VIEW, isDynamicView());
        configuration.setProperty(CC_DRIVE_LETTER, getDriveLetter());
        configuration.setProperty(CC_BUILD_PREFIX, getBuildPrefix());
        configuration.setProperty(CC_PROJECT, getProjectName());
        configuration.setProperty(CC_VIEW_LOCATION, getViewLocation());
        configuration.setProperty(CC_INT_STREAM, getIntStream());
        configuration.setProperty(CC_BUILD_STREAM, getBuildStreamAsSelector());
        configuration.setProperty(CC_MAIN_COMPONENT, getMainComponent());
        configuration.setProperty(CC_STORAGE_DIR, getViewStorageDir());
        configuration.setProperty(CC_MAIN_COMPONENT, getMainComponent());
        configuration.setProperty(CC_LOAD_RULES, getLoadRules());
        configuration.setProperty(CC_VIEW_TAG, getViewTag());
        configuration.setProperty(CC_DISABLE_UPDATE, isDisableUpdate());

        return configuration;
    }

    public String getLoadRules() {
        return loadRules;
    }

    public void setProjectName(String projectName) throws ValidationException {
        this.projectName.setAsString(projectName);
    }

    public String getProjectName() {
        return projectName.asSelector();
    }

    /**
     * Get the main ClearCase component for the project.
     * <p/>
     * The mainCompionent is the component that contains the baseline to compare
     * changes against. Normally this component will be UCM non-rooted component
     * (ie it is the baseline that identifies the baselines that make a release
     * of this project).
     */
    public String getMainComponent() {
        return mainComponent;
    }

    /**
     * Setter for main component.
     *
     * @param component name of the main component.
     * @See #getMainComponent()
     */
    public void setMainComponent(String component) {
        this.mainComponent = component;
    }

    /**
     * @return the buildStream
     */
    public CcSelector getBuildStream() {
        return buildStream;
    }

    /**
     * @param buildStream the buildStream to set
     * @throws ValidationException if the selector passed is not valid
     */
    public void setBuildStream(String buildStream) throws ValidationException {
        this.buildStream.setAsString(buildStream);
    }

    /**
     * @return the intStream
     */
    public String getIntStream() {
        return intStream.asSelector();
    }

    /**
     * @param intStream the intStream to set
     * @throws ValidationException if the selector passed is not valid
     */
    public void setIntStream(String intStream) throws ValidationException {
        this.intStream.setAsString(intStream);
    }

    /**
     * @return the viewLocation
     */
    public String getViewLocation() {
        return viewLocation;
    }

    /**
     * @param viewLocation the viewLocation to set
     */
    public void setViewLocation(String viewLocation) {
        this.viewLocation = viewLocation;
    }

    /**
     * Validate a user entered filename (can be directory name) ensuring it is
     * not null and exists.
     *
     * @param fileName        the filename as string
     * @param errorCollection the error collection to add error message to on validation
     *                        failure.
     * @param fieldKey        the key of the filed for validation failures.
     */
    private void validateFileName(String fileName,
                                  ErrorCollection errorCollection, String fieldKey) {
        if (StringUtils.isBlank(fileName)) {
            errorCollection.addError(fieldKey, "Must be entered");
        } else {
            File file = new File(fileName);
            if (!file.exists()) {
                errorCollection.addError(fieldKey, "Does not exist");
            }
        }
    }

    /**
     * Determines the current baseline the build stream contains, in the case
     * the build stream this is foundation baseline, for which there should only
     * be single baseline.
     *
     * @return the foundation baseline the build stream contains
     * @throws BuildException if the the lookup of the foundation baseline fails.
     */
    public String getLastBuiltBaseline() {
        String rval = null;
        List<String> baselines = ClearCaseUtils.getFoundationBaselines(
                getBuildStreamAsSelector(), getDummyProject(),
                getCleartoolHome());
        if (baselines.size() > 0) {
            rval = baselines.get(0);
            if (baselines.size() > 1) {
                log
                        .warn("The build stream ["
                                + getBuildStreamAsSelector()
                                + "] did not contain a single baseline ["
                                + baselines
                                + "] this is probably an error but will try using the first baseline");
                // TODO if time permits perhaps make it choose baseline that is
                // assocaated with mainComponent
            }
        } else {
            String msg = "Unable to determine the build stream ["
                    + getBuildStreamAsSelector()
                    + "] foundation baselines, can not continue.";
            log.error(msg);
            throw new RuntimeException(msg);
        }
        return rval;
    }

    /**
     * Call cleartool desc command to get specified attribute using -fmt
     * options.
     */
    private String getDescAttribute(String object, String fmt)
            throws RepositoryException {
        desc.setViewPath(getViewLocation());
        desc.setFormat(fmt);
        desc.setObjSelect2(object);
        cmdRunner(desc);
        return desc.getCommandOutput();
    }

    /**
     * Uses the cleartool desc command to get the date the selected object was
     * created.
     *
     * @param objectSelector
     * @return the creation date or null if it can not be determined
     * @throws RepositoryException on failure running cleartool desc command
     */
    private Date getObjectDate(String objectSelector)
            throws RepositoryException {
        Date rval = null;
        String dateTime = getDescAttribute(objectSelector, FMT_CDATE);
        try {
            rval = CC_DATE_FORMAT.parse(dateTime);
        } catch (ParseException e) {
            // log the error but return null
            log.info("unable to determine object[" + objectSelector
                    + "] creation date[" + dateTime + "]", e);
        }
        return rval;
    }

    /**
     * @return the autoCreate
     */
    public boolean isAutoCreate() {
        return autoCreate;
    }

    /**
     * @param autoCreate the autoCreate to set
     */
    public void setAutoCreate(boolean autoCreate) {
        this.autoCreate = autoCreate;
    }

    /**
     * @return the buildPrefix
     */
    public String getBuildPrefix() {
        return buildPrefix;
    }

    /**
     * @param buildPrefix the buildPrefix to set
     */
    public void setBuildPrefix(String buildPrefix) {
        this.buildPrefix = buildPrefix;
    }

    /**
     * @return the viewStorageDir
     */
    public String getViewStorageDir() {
        return viewStorageDir;
    }

    /**
     * Get storage location for the build view but substitute global variables
     * in name first.
     *
     * @return
     */
    private String getBuildViewStorageDir() {
        return substituteGlobalVariables(getViewStorageDir() + File.separator
                + buildStream.getName() + ".vws");
    }

    /**
     * @param viewStorageDir the viewStorageDir to set
     */
    public void setViewStorageDir(String viewStorageDir) {
        this.viewStorageDir = viewStorageDir;
    }

    /**
     * Substitutes any variable named ${bamboo.<global variable>} in the values
     * passed.
     *
     * @param value the value to perform substitution on.
     * @return the substituted value or the original if unchanged.
     */
    protected String substituteGlobalVariables(String value) {
        if (value == null)
            return null;

        // Find by the pattern ${some_variable}
        String variablePattern = "\\$\\{[^\\$\\{\\}]+\\}";
        Pattern pattern = Pattern.compile(variablePattern);

        String substitutedCommand = value;

        Matcher matcher = pattern.matcher(value);

        // can have multi variables on each string in the array.
        while (matcher.find()) {
            String variable = matcher.group();
            String variableField = variable.substring(2, variable.length() - 1);

            if (variableField.indexOf("bamboo.") == 0) {
                variableField = variableField.substring(7, variableField
                        .length());

                String customValue = findGlobalVariableValueFor(variableField);
                substitutedCommand = StringUtils.replaceOnce(
                        substitutedCommand, variable, customValue);



            }
        }
        return substitutedCommand;
    }

    private String findGlobalVariableValueFor(String variableField) {
        List<VariableDefinition> variableDefinitions = variableDefinitionManager.getGlobalVariables();
        for (VariableDefinition definition : variableDefinitions) {
            if (definition.getKey().equals(variableField)) {
                return definition.getValue();
            }
        }
        return null;
    }


    /**
     * @return the ClearCase type, UCM or Base.
     */
    public String getClearCaseType() {
        return this.type;
    }

    /**
     * Sets the ClearCase type.
     *
     * @param type the ClearCase type, UCM or Base.
     */
    public void setClearCaseType(String type) {
        this.type = type;
    }

    /**
     * @return absolute path to the view location in ClearCase base.
     */
    public String getBaseViewLocation() {
        return this.baseViewLocation;
    }

    /**
     * Sets the view location for ClearCase base.
     *
     * @param baseViewLocation absolute path to the view location in ClearCase base.
     */
    public void setBaseViewLocation(String baseViewLocation) {
        this.baseViewLocation = baseViewLocation;
    }

    /**
     * @return relative path to the VOB directory from the view directory.
     */
    public String getVobDir() {
        return this.vobDir;
    }

    /**
     * Sets the VOB directory for ClearCase base.
     *
     * @param vobDir relative path to the VOB directory from the view directory.
     */
    public void setVobDir(String vobDir) {
        this.vobDir = vobDir;
    }

    /**
     * @return absolute path to the VOB directory.
     */
    public String getVobPath() {
        return this.vobPath;
    }

    /**
     * Sets the VOB directory for ClearCase base.
     *
     * @param vobPath absolute path to the VOB directory.
     */
    public void setVobPath(String vobPath) {
        this.vobPath = vobPath;
    }

    /**
     * @return the branch in ClearCase base.
     */
    public String getBranch() {
        return this.branch;
    }

    /**
     * Sets the ClearCase base branch.
     *
     * @param branch the branch used in ClearCase base.
     */
    public void setBranch(String branch) {
        this.branch = branch;
    }

    public boolean shouldCompareBaselines() {
        return compareBaselines;
    }

    public void setCompareBaselines(boolean compareBaselines) {
        this.compareBaselines = compareBaselines;
    }

    public boolean isDynamicView() {
        return dynamicView;
    }

    public void setDynamicView(boolean dynamicView) {
        this.dynamicView = dynamicView;
    }

    public String getDriveLetter() {
        return driveLetter;
    }

    public void setDriveLetter(String driveLetter) {
        this.driveLetter = driveLetter;
    }

    private String getVob() {
        return getProjectName().substring(getProjectName().indexOf("@"));
    }

    public void setCleartoolHome(String cleartoolHome) {
        this.cleartoolHome = cleartoolHome;
    }


    public String getViewTag() {
        return viewTag;
    }

    public void setViewTag(String viewTag) {
        this.viewTag = viewTag;
    }

    public boolean isDisableUpdate() {
        return disableUpdate;
    }


    public void setVariableDefinitionManager(VariableDefinitionManager variableDefinitionManager) {
        this.variableDefinitionManager = variableDefinitionManager;

        for (VariableDefinition variableDefinition : variableDefinitionManager.getGlobalVariables()) {
            if (variableDefinition.getKey().equals(CLEARTOOL_HOME)) {
                this.cleartoolHome = variableDefinition.getValue();
                break;
            }
        }

    }
}

package com.atlassian.bamboo.plugins.clearcase.ant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.listener.Log4jListener;

import com.atlassian.bamboo.plugins.clearcase.utils.AbstractTokenProcessor;
import com.atlassian.bamboo.plugins.clearcase.utils.StringSplitter;

/**
 * ClearCase UCM utilities methods that perform their operations by invoking the
 * cleartool command using ANT
 * {@link org.apache.tools.ant.taskdefs.optional.clearcase.ClearCase} class as
 * it base. Thus all method require ANT project but on can be obtained by
 * calling the {{@link #getAntProject()} method.
 */
public class ClearCaseUtils {

	public static final String FMT_INTSTREAM = "%[istream]Xp";

	/**
	 * Provide a list of baselines on separate lines.
	 */
	public static final String FMT_STREAM_BASELINES = "%Xn\\n";

	/**
	 * The Cleartool fomrat string to obtin date baseline was created.
	 */
	public static final String FMT_CREATE_DATE = "%d";

	/**
	 * The Cleartool format string used with the lshistory command. The
	 * resulting dates are formatted numerically. e.g. yyyyMMdd.HHmmss
	 */
	public static final String FMT_CREATE_DATE_NUM = "%Nd";

	/**
	 * Provide a list of baselines on separate lines.
	 */
	public static final String FMT_FOUND_BASELINES = "%[found_bls]Xp";

	public static final String DELIM_WHITESPACE = "\\s";

	private static final String DATE_DELIM = ",";

	/**
	 * Delimiter for lshistory formatted lines.
	 */
	public static final String LSHISTORY_DELIM = "#~#~#";

	/**
	 * Delimiter for lshistory result line breaks.
	 */
	public static final String LSHISTORY_END_OF_RESULT_DELIM = "@#@#@#@#@";

	/**
	 * Cleartool format string to obtain a formatted history event. Format: date
	 * user file version operation comment.
	 */
	public static final String FMT_LSHISTORY = FMT_CREATE_DATE_NUM
			+ LSHISTORY_DELIM + "%u" + LSHISTORY_DELIM + "%En"
			+ LSHISTORY_DELIM + "%Vn" + LSHISTORY_DELIM + "%o"
			+ LSHISTORY_DELIM + "%Nc" + LSHISTORY_END_OF_RESULT_DELIM;

	/**
	 * The size of the parsed response from lshistory.
	 */
	public static final int LSHISTORY_RESULT_SIZE = 6;

	/**
	 * Array index constants for command response from lshistory.
	 */
	public static final int LSHISTORY_DATE = 0;
	public static final int LSHISTORY_USER = 1;
	public static final int LSHISTORY_FILE = 2;
	public static final int LSHISTORY_VERSION = 3;
	public static final int LSHISTORY_OPERATION = 4;
	public static final int LSHISTORY_COMMENT = 5;

	/**
	 * Get a Ant project that is configured to use Log4J to capture output.
	 * <p/>
	 * Add following line log4j configuration file to turn on debug logging for
	 * ant tasks used by these utilities:
	 * <p/>
	 * <code>log4j.logger.org.apache.tools.ant=DEBUG</code>
	 * <p/>
	 * 
	 * @return
	 */
	public static Project getAntProject() {
		Project proj = new Project();
		// setup a log4j listener to get some feedback
		proj.addBuildListener(new Log4jListener());
		return proj;
	}

	/**
	 * Get the foundation that make up given stream.
	 * 
	 * @param stream
	 * @param project
	 *            the Ant project
	 * @param cleartoolHome
	 * @return the list of foundation baselines for a given stream, @NotNull
	 * @throws BuildException
	 *             if an error occurs running the ClearTool command.
	 */
	public static List<String> getFoundationBaselines(String stream,
			Project project, String cleartoolHome) {
		String data = runClearToolCommand(project,
				ClearToolListCommand.COMMAND_LSSTREAM, stream,
				FMT_FOUND_BASELINES, null, cleartoolHome);
		return getTokens(data, DELIM_WHITESPACE);
	}

	/**
	 * Give a snapshot view tag get set of current load rules for the view.
	 * 
	 * @param viewTag
	 *            the view to get rules of @Notnull
	 * @param project
	 *            an ant project to run the command under.
	 * @return the set of load rules, not null but can be empty list.
	 */
	public static Set<String> getCurrentLoadRules(String viewTag,
			Project project) {
		CcCatcs catcs = new CcCatcs();
		catcs.setProject(project);
		catcs.setViewTag(viewTag);
		catcs.execute();
		final Set<String> rval = new HashSet<String>();
		AbstractTokenProcessor tokenProcessor = new AbstractTokenProcessor(
				catcs.getCommandOutput()) {
			@Override
			protected void processToken(String token) {
				if (token.startsWith("load ")) {
					rval.add(token.substring("load ".length()));
				}
			}
		};
		tokenProcessor.process();
		return rval;
	}

	/**
	 * Gets list of baselines for a given stream. The baseline list returned
	 * will be sorted in ascending order creation date.
	 * 
	 * @param stream
	 *            the stream to list baselines on.
	 * @param baselineComp
	 *            the component storing the baseline that defines this streams
	 *            configuration normally a non-rooted component.
	 * @param cleartoolHome
	 * @return the list of baseline for the stream, never null but can contain
	 *         no items.
	 * @throws BuildException
	 *             if an error occurs running the ClearTool command.
	 */
	public static List<String> getStreamBaselines(String stream,
			String baselineComp, Project project, String cleartoolHome) {

		String data = runClearToolCommand(project,
				ClearToolListCommand.COMMAND_LSBL, "", FMT_CREATE_DATE
						+ DATE_DELIM + FMT_STREAM_BASELINES, new String[] {
						"-component", baselineComp, "-stream", stream }, null,
				cleartoolHome);
		List<String> dateSortedList = getTokens(data, null);
		List<String> rval = new ArrayList<String>();

		for (String dateBl : dateSortedList) {
			rval.add(dateBl.substring(dateBl.indexOf(DATE_DELIM)
					+ DATE_DELIM.length()));
		}
		return rval;
	}

	/**
	 * Given a snapshot view directory determine the ClearCase view tag.
	 * 
	 * @param viewDir
	 *            the snapshot view directory
	 * @param project
	 *            ANt project to run command against.
	 * @return the view tag.
	 * @throws BuildException
	 *             if the view directory is not ClearCase view or does not
	 *             exist.
	 */
	public static String getViewTag(String viewDir, Project project)
			throws BuildException {
		CcLsview lsview = new CcLsview();
		lsview.setProject(project);
		lsview.setCview(true);
		lsview.setViewPath(viewDir);
		lsview.setShort(true);
		lsview.execute();
		return lsview.getCommandOutput();
	}

	/**
	 * Split the given string into list list of strings ignoring empty tokens 7*
	 * and
	 * 
	 * @param lineData
	 *            the string to split
	 * @param tokenExp
	 *            the delimiter expression, if null then split into lines
	 * @return the list of tokens
	 */
	private static List<String> getTokens(String lineData, String tokenExp) {
		List<String> rval = new ArrayList<String>();
		if (lineData != null) {
			StringSplitter sp = new StringSplitter(lineData, tokenExp, true);
			rval = sp.getItems();
		}
		return rval;
	}

	/**
	 * Run the cleartool command on supplied object with specified format and
	 * arguments.
	 * 
	 * @param project
	 *            the ant project to run the command against.
	 * @param objectSelector
	 *            the object selector
	 * @param fmtString
	 *            the format string that conforms to fmt_ccase descriptions.
	 * @param viewDir
	 *            the view locating the object to describe, can be null if not
	 *            view is to be used.
	 * @return the output of the cleartool desc command.
	 * @throws BuildException
	 *             if the execution of desc command fails
	 */
	private static String runClearToolCommand(Project project, String command,
			String objectSelector, String fmtString, String viewDir,
			String cleartoolHome) {
		return runClearToolCommand(project, command, objectSelector, fmtString,
				null, viewDir, cleartoolHome);
	}

	/**
	 * Run the cleartool command on supplied object with specified format and
	 * arguments.
	 * 
	 * @param project
	 *            the ant project to run the command against.
	 * @param objectSelector
	 *            the object selector
	 * @param fmtString
	 *            the format string that conforms to fmt_ccase descriptions.
	 * @param extraArgs
	 *            the additional command line arguments, may be null
	 * @param viewDir
	 *            the view locating the object to describe, can be null if not
	 *            view is to be used.
	 * @return the output of the cleartool desc command.
	 * @throws BuildException
	 *             if the execution of desc command fails
	 */
	private static String runClearToolCommand(Project project, String command,
			String objectSelector, String fmtString, String[] extraArgs,
			String viewDir, String cleartoolHome) {
		ClearToolListCommand ct = new ClearToolListCommand(command, extraArgs);
		ct.setProject(project);
		ct.setFormat(fmtString);
		ct.setViewPath(viewDir);
		ct.setObjSelect2(objectSelector);
		ct.setFailOnErr(true);
		if (StringUtils.isNotEmpty(cleartoolHome))
			ct.setClearToolDir(cleartoolHome);
		ct.execute();
		return ct.getCommandOutput();
	}

	/**
	 * Collects the latest changes in a ClearCase base view.
	 * 
	 * @param since
	 *            checks for changes since this time.
	 * @param branch
	 *            the branch to check.
	 * @param vobDir
	 *            checks for changes from this directory.
	 * @param project
	 *            ant project. Not really used here.
	 * @param cleartoolHome
	 * @return a list of changes.
	 */
	public static List<String> getLatestChanges(String since, String branch,
			String vobDir, Project project, String cleartoolHome) {

		// Run the lshistory cleartool command to get the latest changes.
		// -nco is used so checked out files are ignored.
		// -r is used so the check will be made recursively from the specified
		// vobdir down.
		String[] args;
		if (StringUtils.isEmpty(branch)) {
			args = new String[] { "-nco", "-since", since, "-r" };
		} else {

			args = new String[] { "-nco", "-since", since, "-branch", branch,
					"-r" };
		}

		String data = runClearToolCommand(project,
				ClearToolListCommand.COMMAND_LSHISTORY, "", FMT_LSHISTORY,
				args, vobDir, cleartoolHome);

		StringSplitter stringSplitter = new StringSplitter(data,
				LSHISTORY_END_OF_RESULT_DELIM, false);

		return stringSplitter.getItems();
	}

}

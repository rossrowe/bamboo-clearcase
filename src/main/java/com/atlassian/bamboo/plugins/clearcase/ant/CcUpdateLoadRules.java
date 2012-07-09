package com.atlassian.bamboo.plugins.clearcase.ant;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.taskdefs.optional.clearcase.ClearCase;

/**
 * Given a snapshot view tag associated with a UCM stream, check the load rules
 * includes all required directories for the current foundation baseline. If the
 * load rules are missing then <code>cleartool update -add_loadrules<code>
 * is use to add the extra components root directories. 
 * This process will only ever add missing load rules, if a component
 * is not longer required the load rule will still remain in the view but nothing 
 * will be loaded in that directory (i.e. no cleanup occurs).
 * <p>
 * Possible parameters:
 * <ul>
 * <li><strong>viewPath</strong> - (required) a view tag associated with the snapshot view to check.</li>
 * <li><strong>viewTag</strong> - (optional) derived from viewDir if not supplied.</li>
 * <li><strong>stream</strong> - (optional) the stream to to use to determine the baseline to check,
 *      If not supplied then it is derived from the stream associated with viewTag.</li>
 * <li><strong>baseline,</strong> - (optional) the baseline to determine load rules from. If not
 *      specified then it is determined from the stream, or viewTag.</li>  
 * </ul>
 * 
 */
public class CcUpdateLoadRules extends AbstractCleartoolCmd {

	public static final String FMT_EXTENED_NAME = "%Xn";
	private String baseline;
	private String stream;
	private String viewTag;
	private String explicitLoadRules;

    public CcUpdateLoadRules() {
        super();
    }


    /**
     * Constructor specify the cleartool command to run.
     *
     * @param command the cleartool command name.
     */
    public CcUpdateLoadRules(String command) {
        super(command);
    }

    protected void setupArguments(Commandline commandLine) throws BuildException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
	 * Coordinates the running required commands to update the view load rules.
	 */
	@Override
	public void execute() throws BuildException {

		if (StringUtils.isBlank(getViewPath())) {
			throw new BuildException("'viewpath' must be specified");
		}

		Set<String> currentLoadRules = getCurrentLoadRules();
		log("Current load rules: " + currentLoadRules, Project.MSG_VERBOSE);

		String bl = determineBaseline();
		CcListBlcompRoots listBlRoots = new CcListBlcompRoots();
		listBlRoots.setProject(getProject());
		listBlRoots.setBaseline(bl);
		listBlRoots.execute();
		log("Required load rules: " + listBlRoots.getLoadRules(),
				Project.MSG_VERBOSE);
		Set<String> toAdd = new HashSet<String>();
		toAdd.addAll(listBlRoots.getLoadRules());

		String[] split = explicitLoadRules.split(System
				.getProperty("line.separator"));
		for (String string : split) {
			toAdd.add(string);
		}
		toAdd.removeAll(currentLoadRules);
		log("Rules to add to view [" + viewTag + "] : " + toAdd,
				Project.MSG_DEBUG);

		CcAddLoadRules addLoadRule = new CcAddLoadRules();
		addLoadRule.setProject(getProject());
		addLoadRule.setViewPath(getViewPath());
		addLoadRule.setOverwrite(true);
		for (String newRule : toAdd) {
			if (!StringUtils.isEmpty(newRule)) {
				addLoadRule.setLoadRule(newRule);
				addLoadRule.execute();
			}
		}

	}

	/**
	 * Used lsview to determine the viewTag if viewTag was not specified.
	 * 
	 * @return the view tag.
	 */
	private String determineViewTag() {
		if (viewTag == null) {
			viewTag = ClearCaseUtils.getViewTag(getViewPath(), getProject());
		}
		return viewTag;
	}

	/**
	 * Get the baseline to add add required load rules for. If the baseline
	 * parameter is not specified then this is derived from the specified stream
	 * which in turn is could be derived from the specified viewTag. <p> The
	 * lookup process will only occur once.
	 * 
	 * @return the baseline to test load rules of.
	 */
	private String determineBaseline() {
		if (baseline == null) {
			String strm = determineStream();
			log("Determining baseline from stream [" + stream + "]",
					Project.MSG_VERBOSE);
			List<String> foundation = ClearCaseUtils.getFoundationBaselines(
					strm, getProject(), getCleartoolHome());
			if (foundation.size() > 0) {
				// always take first baseline as a build stream should only have
				// one.
				baseline = foundation.get(0);
				log("Using derived baseline [" + baseline + "]",
						Project.MSG_DEBUG);
			} else {
				throw new BuildException(
						"Unable to determine baseline to check against. ViewTag["
								+ getViewTag() + "]", getLocation());
			}
		}
		return baseline;
	}

	/**
	 * Get the stream that is to be used to determine the baseline (foundation)
	 * to add load rules to match. If <em>stream</em> was not specified this is
	 * determined from stream associated with <em>viewTag</em>
	 * 
	 * @return the extend stream name (i.e. fully specified ClearCase selector)
	 */
	private String determineStream() {
		if (stream == null) {
			CcLsstream lsstream = new CcLsstream();
			lsstream.setProject(getProject());
			lsstream.setFormat(FMT_EXTENED_NAME);
			lsstream.setViewTag(determineViewTag());
			lsstream.execute();
			stream = lsstream.getCommandOutput();
			log("Using derived stream [" + stream + "]", Project.MSG_VERBOSE);
		}
		return stream;
	}

	/**
	 * Get the current loaded component directories for the {@link #viewTag}.
	 * 
	 * @return the current load rules for the view, never null but can be empty.
	 * 
	 * @throws BuildException
	 *             when ANT task used fails.
	 */
	private Set<String> getCurrentLoadRules() throws BuildException {
		return ClearCaseUtils.getCurrentLoadRules(determineViewTag(),
				getProject());
	}

	/**
	 * Simple getter.
	 */
	public String getViewTag() {
		return viewTag;
	}

	/**
	 * Simple setter.
	 * 
	 * @param viewTag
	 *            the new viewTag.
	 */
	public void setViewTag(String viewTag) {
		this.viewTag = viewTag;
	}

	/**
	 * Simple getter.
	 * 
	 * @return the baseline
	 */
	public String getBaseline() {
		return baseline;
	}

	/**
	 * Simple setter.
	 * 
	 * @param baseline
	 *            the baseline to set
	 */
	public void setBaseline(String baseline) {
		this.baseline = baseline;
	}

	/**
	 * @return the stream
	 */
	public String getStream() {
		return stream;
	}

	/**
	 * @param stream
	 *            the stream to set
	 */
	public void setStream(String stream) {
		this.stream = stream;
	}

	public void setExplicitLoadRules(String loadRules) {
		this.explicitLoadRules = loadRules;

	}

}

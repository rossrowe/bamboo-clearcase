package com.atlassian.bamboo.plugins.clearcase.ant;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;

/**
 * Run the 'setcs' command to set the config spec of a snapshot view.
 *  Parameters:
 * <ul>
 * <li>viewtag - (optional) the view tag to display, if not specified the 
 *     viewPath must be set to directory contain a view.</li>
 * <li>viewpath - (optional) the directory contain a view to display config spec of,
 *     Ignored if viewtag is set.</li>
 * <li>configfile - name of file containg the config spec to set.</li>
 * </ul>
 */
public class CcSetcs extends ClearToolListCommand {

	private static final long serialVersionUID = -3586535148379349404L;

	private static final String COMMNAD_SETCS = "setcs";

	private static final String FLAG_VIEW_TAG = "-tag";
	
	private String viewTag;
	private String configFile;
	
	
	/**
	 * @return the viewTag
	 */
	public String getViewTag() {
		return viewTag;
	}

	/**
	 * @param viewTag the viewTag to set
	 */
	public void setViewTag(String viewTag) {
		this.viewTag = viewTag;
	}

	/**
	 * default constructor setting up command.
	 */
	public CcSetcs() {
		super(COMMNAD_SETCS);
	}

	/**
	 * If not view tag is specified then it assumes viewPath is set so working
	 * directory will find view.
	 * 
	 * @see com.atlassian.bamboo.plugins.clearcase.ant.ClearToolListCommand#setupArguments(org.apache.tools.ant.types.Commandline)
	 */
	@Override
	protected void setupArguments(Commandline cmd) {
		if(StringUtils.isNotBlank(getViewTag())) {
			cmd.createArgument().setValue(FLAG_VIEW_TAG);
			cmd.createArgument().setValue(getViewTag());
		}
		
		if(StringUtils.isBlank(getConfigFile()))
		{
			throw new BuildException("The configfile must be specified.");
		}
		cmd.createArgument().setValue(getConfigFile());
	}

	/**
	 * @return the configFile
	 */
	public String getConfigFile() {
		return configFile;
	}

	/**
	 * @param configFile the configFile to set
	 */
	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}
}

package com.atlassian.bamboo.plugins.clearcase.ant;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.types.Commandline;

/**
 * Run the 'catcs' command to display the config spec of the view.
 *  Parameters:
 * <ul>
 * <li>viewtag - (optional) the view tag to display, if not specified the 
 *     viewPath must be set to directory contain a view.</li>
 * <li>viewpath - (optional) the directory contain a view to display config spec of,
 *     Ignored if viewtag is set.</li>
 * </ul>
 *
 */
public class CcCatcs extends ClearToolListCommand {

	private static final long serialVersionUID = 2395631196696363281L;

	private static final String FLAG_VIEW_TAG = "-tag";
	
	private String viewTag;
	
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

	public CcCatcs() {
		super("catcs");
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
	}
}

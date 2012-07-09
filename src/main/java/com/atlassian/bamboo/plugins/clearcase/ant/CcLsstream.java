package com.atlassian.bamboo.plugins.clearcase.ant;

import org.apache.tools.ant.types.Commandline;

/**
 * Run the 'lsstream' cleartool command.
 * 
 * Addtional aguements supported over those specified in {@link ClearToolListCommand}
 * are:
 * <ul>
 * <li>viewTag - (optiional) Displays information for the stream connected to the specified view.</li>
 * </ul>
 */
public class CcLsstream extends ClearToolListCommand {

	private static final long serialVersionUID = -7261770152521777704L;

	private String viewTag;
	
	public static final String COMMAND_LSSTREAM = "lsstream";

	/**
	 * Default constructor specifying options.
	 * @param command
	 */
	public CcLsstream() {
		super(COMMAND_LSSTREAM);
	}

	/**
	 * Add
	 * @see com.atlassian.bamboo.plugins.clearcase.ant.ClearToolListCommand#addExtraArgs(org.apache.tools.ant.types.Commandline)
	 */
	@Override
	protected void addExtraArgs(Commandline cmd) {
		// TODO Auto-generated method stub
		super.addExtraArgs(cmd);
	}

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

}

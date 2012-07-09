package com.atlassian.bamboo.plugins.clearcase.ant;

/**
 * Used to run cleartool 'lsproject' where  {@link objeSelector2 #setObjSelect2(String)}
 * is a clearcase project selector.
 */
public class CcLsproject extends ClearToolListCommand {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8993624119477869950L;
	/**
	 * The 'describe' command
	 */
	public static final String COMMAND_LSPROJECT = "lsproject";
	
	/**
	 * @param command
	 */
	public CcLsproject() {
		super(COMMAND_LSPROJECT);
	}

}

package com.atlassian.bamboo.plugins.clearcase.ant;

/**
 * Run cleartool lsbl command.
 * <ul>
 * <li> format- the format string for the command</li>
 * <li> <strong>selObject2</strong> - the baseline to list details of </li>
 * </ul>
 */
public class CcLsbl extends ClearToolListCommand {

	private static final long serialVersionUID = 6402585570005732985L;

	public CcLsbl() {
		super("lsbl");
	}
	
}

package com.atlassian.bamboo.plugins.clearcase.ant;

/**
 * Run cleartool lscomp command.
 * <ul>
 * <li> <strong>format</strong> - the format string for the command</li>
 * <li> <strong>selObject2</strong> - the baseline to list details of </li>
 * </ul>
*/
public class CcLscomp extends ClearToolListCommand {
	
	private static final long serialVersionUID = -4828423441148185148L;

	public CcLscomp()
	{
		super("lscomp");
	}

}

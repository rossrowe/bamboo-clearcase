/**
 * 
 */
package com.atlassian.bamboo.plugins.clearcase.ant;

import org.apache.tools.ant.types.Commandline;

/**
 * @author exitr6
 *
 */
public class CcUpdateSnapshot extends ClearToolListCommand {
	
	private static final long serialVersionUID = 5137023050815540644L;
	/**
	 * The 'describe' command
	 */
	public static final String COMMAND_UPDATE = "update";
	
	public CcUpdateSnapshot() {
		super(COMMAND_UPDATE);
	}
	
	/**
	 * Setup the rebase command line options
	 * 
	 * @see au.gov.dva.ant.cc.tasks.ClearToolListCommand#setupArguments(org.apache.tools.ant.types.Commandline)
	 */
	@Override
	protected void setupArguments(Commandline cmd) {
		cmd.createArgument().setValue(getViewPath());
	}

}

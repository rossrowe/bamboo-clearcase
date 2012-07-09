package com.atlassian.bamboo.plugins.clearcase.ant;

import org.apache.tools.ant.types.Commandline;

/**
 * Run the cleartool 'lsview' command.
 */
public class CcLsview extends ClearToolListCommand {

	private static final long serialVersionUID = -5085951877960740865L;

	public static final String COMMAND_LSVIEW = "lsview";

	public static final String FLAG_CVIEW = "-cview";

	private boolean cview;

	/**
	 * @param command
	 */
	public CcLsview() {
		super(COMMAND_LSVIEW);
	}

	/**
	 * Add the cview argument if set.
	 * @see com.atlassian.bamboo.plugins.clearcase.ant.ClearToolListCommand#setupArguments(org.apache.tools.ant.types.Commandline)
	 */
	@Override
	protected void setupArguments(Commandline cmd) {
		if(cview) {
			cmd.createArgument().setValue(FLAG_CVIEW);
		}
		super.setupArguments(cmd);
	}
	
	/**
	 * Ant style setter.
	 * @param cview
	 */
	public void setCview(boolean cview)
	{
		this.cview = cview;
	}
	
	/**
	 * Simple getter.
	 * @return
	 */
	public boolean getCview()
	{
		return this.cview;
	}
	
	
	

	
}

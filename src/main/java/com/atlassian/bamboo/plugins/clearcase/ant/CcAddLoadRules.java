package com.atlassian.bamboo.plugins.clearcase.ant;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;

/**
 * Add the specified <em>loadRule</em> to the snapshot view specified by
 * <em>viewPath</em>.  The load rule is added by running 
 * <code> cleartool update -add_loadrules {options} ruleToAdd</code>.
 */
public class CcAddLoadRules extends AbstractCleartoolCmd {

	private static final long serialVersionUID = -4169188227948709660L;
	public static final String FLAG_OVERWRITE = "-overwrite";
	private String loadRule;
	private boolean overwrite = true;
	private boolean force = true;
	
	/**
	 * Specify the update command.
	 */
	public CcAddLoadRules() {
		super(COMMAND_UPDATE);
	}

	/**
	 * Add the loadrule argument
	 * @see com.atlassian.bamboo.plugins.clearcase.ant.AbstractCleartoolCmd#setupArguments(org.apache.tools.ant.types.Commandline)
	 */
	@Override
	protected void setupArguments(Commandline commandLine)
			throws BuildException {
		if(StringUtils.isBlank(loadRule))
		{
			throw new BuildException("loadrule was not specified",getLocation());
		}
		if(StringUtils.isBlank(getViewPath()))
		{
			throw new BuildException("viewpath was not specified",getLocation());
			
		}
		commandLine.createArgument().setValue("-add_loadrules");
		
		if(force)
		{
			commandLine.createArgument().setValue(FLAG_FORCE);
		}

		if(overwrite)
		{
			commandLine.createArgument().setValue(FLAG_OVERWRITE);
		}
		
		// fully qualified path of load rule to add.
		commandLine.createArgument().setValue(getViewPath()+File.separator+loadRule);
	}

	/**
	 * @return the loadRule
	 */
	public String getLoadRule() {
		return loadRule;
	}

	/**
	 * @param loadRule the loadRule to set
	 */
	public void setLoadRule(String loadRule) {
		this.loadRule = loadRule;
	}

	/**
	 * @param overwrite the overwrite to set
	 */
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	/**
	 * Ant task style getter.
	 * 
	 * @return the overwrite value
	 */
	public boolean getOverwrite()
	{
		return overwrite;
	}
	
	/**
	 * @param force the force to set
	 */
	public void setForce(boolean force) {
		this.force = force;
	}

	public boolean getForce()
	{
		return force;
	}
}

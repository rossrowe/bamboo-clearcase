package com.atlassian.bamboo.plugins.clearcase.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;

/**
 * Run ClearCase diffbl command to list differences between 2 streams or 
 * baselines, refer to <code>cleartool man diffbl</code> for more information.
 * This is most commonly used to determine if build stream is up to date. 
 *<p>
 * You must specify a 'ViewPath' if 'version' attribute is true.
 */
public class CcDiffbl extends AbstractCleartoolCmd {


	private static final long serialVersionUID = 4757129766960644416L;

	public static final String COMMAND_DIFFBL = "diffbl";
	
	private String selector1 = null;
	private String selector2 = null;
	private boolean versions = false;
	private boolean activities = true;

	/**
	 * Default constructor.
	 */
	public CcDiffbl() {
		super(COMMAND_DIFFBL);
	}
	
	/**
	 * Add the diffbl arguments.
	 * @see AbstractCleartoolCmd#setupArguments(Commandline)
	 */
	@Override
	protected void setupArguments(Commandline cmd) throws BuildException {
		if(activities)
		{
			cmd.createArgument().setValue("-act");
		}
		if(versions)
		{
			if(getViewPath() == null || getViewPath().trim().length() <= 0 )
			{
				String msg = "ccdiff: Missing 'ViewPath' attribute. The version attribute cannot be specified without setting 'ViewPath'";
				getProject().log(msg, Project.MSG_ERR);
				throw new BuildException(msg);
			}
			cmd.createArgument().setValue("-ver");
		}
		
		cmd.createArgument().setValue(selector1);
		cmd.createArgument().setValue(selector2);
	}
	
	public void setVersions(boolean versions)
	{
		this.versions = versions;
	}
	
	public boolean getVersions()
	{
		return this.versions;
	}
	
	public void setActivities(boolean activities)
	{
		this.activities = activities;
	}
	
	public boolean getActivities()
	{
		return this.activities;
	}

	/**
	 * @return the selector1
	 */
	public String getSelector1() {
		return selector1;
	}

	/**
	 * @param selector1 the selector1 to set
	 */
	public void setSelector1(String selector1) {
		this.selector1 = selector1;
	}

	/**
	 * @return the selector2
	 */
	public String getSelector2() {
		return selector2;
	}

	/**
	 * @param selector2 the selector2 to set
	 */
	public void setSelector2(String selector2) {
		this.selector2 = selector2;
	}

}

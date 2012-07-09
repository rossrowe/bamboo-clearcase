package com.atlassian.bamboo.plugins.clearcase.ant;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;

/**
 * Use rmview to remove a snapshot view. Only attribute required is
 * 'view'. 'viewpath' must not be set to view directory to remove 
 * as otherwise command run in this directory and fails.
 */
public class CcRmview extends AbstractCleartoolCmd {

	private static final long serialVersionUID = 7171232547775006171L;

	private static final String COMMAND_RMVIEW = "rmview";
	
	private String view;

	/**
	 * Default constructor setting the command argument
	 */
	public CcRmview() {
		super(COMMAND_RMVIEW);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see com.atlassian.bamboo.plugins.clearcase.ant.AbstractCleartoolCmd#setupArguments(org.apache.tools.ant.types.Commandline)
	 */
	@Override
	protected void setupArguments(Commandline commandLine)
			throws BuildException {
		if (StringUtils.isBlank(getView())) {
			throw new BuildException("Missing 'view' attribute.", getLocation());
		}
		
		commandLine.createArgument().setValue(FLAG_FORCE);
		commandLine.createArgument().setValue(getView());
	}

	/**
	 * @return the view
	 */
	public String getView() {
		return view;
	}

	/**
	 * @param view the view to set
	 */
	public void setView(String view) {
		this.view = view;
	}

}

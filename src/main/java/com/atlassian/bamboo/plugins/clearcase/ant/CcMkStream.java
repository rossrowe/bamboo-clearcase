package com.atlassian.bamboo.plugins.clearcase.ant;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;

/**
 * Run cleartool 'mkstream' to create new stream.
 * 
 * Parameters:
 * <table border="1pt">
 * <tr><th>Attribute</th><th>Description</th><th>Required</th></tr>
 * <tr><td>objSelect2</td>
 * <td>Name to the stream to create</td><td>Yes</td></tr>
 * <tr><td>parentStream</td>
 * <td>Parent of the stream to create</td>
 * <td rowspan="2">Yes, one of parentStream or projectSelector must be specified
 *  but not both</td></tr>
 * <tr><td>projectSelector</td>
 * <td>Name of the project to create a development stream for</td></tr>
 * <tr><td>readOnly</td>
 * <td>Boolean indicating if stream is read only</td><td>No, default is true</td></tr>
 * </table>
 *
 */
public class CcMkStream extends AbstractCleartoolCmd {

	private static final long serialVersionUID = 1630207023872240299L;
	public static final String COMMAND_MKSTREAM = "mkstream";
	private static final String FLAG_READONLY = "-readonly";
	public static final String FLAG_IN = "-in";
	private String parentStream;
	private String projectSelector;
	private boolean readOnly = true;
	
	/**
	 * Default construct specify the command only.
	 */
	public CcMkStream() {
		super(COMMAND_MKSTREAM);
	}

	/**
	 * Setup the mkstream command line.
	 * 
	 * @see com.atlassian.bamboo.plugins.clearcase.ant.AbstractCleartoolCmd#setupArguments(org.apache.tools.ant.types.Commandline)
	 */
	@Override
	protected void setupArguments(Commandline commandLine)
			throws BuildException {
		if(StringUtils.isBlank(getProjectSelector()) && StringUtils.isBlank(getParentStream()))	{
			throw new BuildException("Must specify on of parentStream or projectSelector",getLocation());
		}
		if(StringUtils.isBlank(getObjSelect2())) {
			throw new BuildException("The name of stream to create was not specified via 'objSelect2' attribute");
		}
			
		commandLine.createArgument().setValue(FLAG_IN);
		if(StringUtils.isNotBlank(getParentStream()))	{
			commandLine.createArgument().setValue(getParentStream());
		} else {
			commandLine.createArgument().setValue(getProjectSelector());
		}
		
		if(readOnly)
		{
			commandLine.createArgument().setValue(FLAG_READONLY);
		}
		commandLine.createArgument().setValue(getObjSelect2());

	}

	/**
	 * @return the parentStream
	 */
	public String getParentStream() {
		return parentStream;
	}

	/**
	 * @param parentStream the parentStream to set
	 */
	public void setParentStream(String parentStream) {
		this.parentStream = parentStream;
	}

	/**
	 * @return the projectSelector
	 */
	public String getProjectSelector() {
		return projectSelector;
	}

	/**
	 * @param projectSelector the projectSelector to set
	 */
	public void setProjectSelector(String projectSelector) {
		this.projectSelector = projectSelector;
	}

	/**
	 * Ant style attribute getter.
	 * @return the readOnly
	 */
	public boolean getReadOnly() {
		return readOnly;
	}

	/**
	 * @param readOnly the readOnly to set
	 */
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}


}

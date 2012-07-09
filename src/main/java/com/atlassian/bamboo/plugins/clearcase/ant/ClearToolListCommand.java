package com.atlassian.bamboo.plugins.clearcase.ant;

import org.apache.tools.ant.types.Commandline;

/**
 * Used to run a cleartool command in form below and 
 * capture the output produced in a string {@link #getCommandOutput()}.
 * <pre>
 *   <code>cleartool cmd {-fmt fmt_string | -l} {extra_args} {objSelect2}</code>
 * </pre>
 * <p>
 * The -fmt option can be set to -l or not specified, if not supplied then this option is
 * not included.
 * <p> 
 * The <em>extra_arg</em> are not added if they are null.
 * <p>
 * The <em>ObjSelect2</em> - is not added if null, allowed as some command do not
 * require the selector but rely on other argument or working directory to determine
 * the value ot act on.
 * <p>
 * The command working directory is determined by <em>ViewPath</em> if the view 
 * path specified exists.
 * <p> 
 * This pattern is relevant to many of the cleartool list type commands that do not perfrom updates
 * eg, describe, lsstream, lsbl, etc.
 * <p>
 * Parameter supported by this class are: <table border="1">
 * <tr>
 * <th>Attribute</th>
 * <th>Values</th>
 * <th>Required</th>
 * </tr>
 * <tr>
 * <td>viewpath</td>
 * <td>The location of the view with the object to describe. IF specified then
 * the view path is appended before objectsel2 </td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>objselect2</td>
 * <td>The ClearCase object to describe. If the viewPath is specified it is
 * appended before the this name to the command.<br/>
 * <strong>Note:</strong> this would have been objselect but because getObjSelect()
 * was added a final method in ClearCase ANt optional class post version 1.6 to be 
 *  able to use this class with ANT prior to 1.6 this need a different name</td>
 * <td>Yes</td>
 * </tr>
 * <tr>
 * <td>format</td>
 * <td>The -fmt string to pass to the describe command.</td>
 * <td>No</td>
 * <tr>
 * <tr>
 * <td>formatLong</td>
 * <td>If true set -l ie long format. if false then no format is used (ie use command default)</td>
 * <td>No</td>
 * </tr>
 * <td>short</td>
 * <td>If true set -s ie short format. if false then no format is used (ie use command default)</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>output</td>
 * <td>Name of the property to store command output into.</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>appendPath</td>
 * <td>If true, the viewPath is append before the object name. Default, false</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>failonerr</td>
 * <td>Throw an exception if the command fails. Default is true</td>
 * <td>No</td>
 * <tr> </table>

 */
public class ClearToolListCommand extends AbstractCleartoolCmd {
	
	private static final long serialVersionUID = 7076309855282122497L;

	private static final String FLAG_FMT = "-fmt";

	public static final String FLAG_LONG = "-l";

	public static final String FLAG_SHORT = "-s";

	private String[] extraArgs = null;

	/**
	 * The string the conforms to fmt_ccase (see cleartool man fmt_ccase) passed
	 * with -fmt option. *
	 */
	private String format = null;

	/** this option is only used if {@link format} is not supplied. * */
	private boolean formatLong = false;
	private boolean formatShort = false;

	private boolean appendPath = false;

	/**
	 * @param appendPath the appendPath to set
	 */
	public void setAppendPath(boolean appendPath) {
		this.appendPath = appendPath;
	}

	public boolean getAppendPath()
	{
		return this.appendPath;
	}
	
	public ClearToolListCommand(String command)
	{
		super(command);
	}

	public ClearToolListCommand(String command, String[] extraArgs)
	{
		super(command);
		this.extraArgs = extraArgs;
	}

	public void setExtraArguments(String[] args)
	{
		extraArgs = args;
	}
	
	/**
	 * Setup the command line arguments relevant to this command.  The command 
	 * line passed will already have the {{@link #command} set as the first argument.
	 * <p>
	 * The default process here is 
	 * <ol>
	 * <li> -fmt or -l output format or none if neither are specified (ie use command default</li>
	 * <li> extraArg if any are present</li>
	 * <li>The object select qualified by the view path if set. If the view path does not end with '\' or '/'
	 * then the file separator is appended between viewPath and ObjSelect. </li>
	 * <ol>
	 * <p>
	 * Subclasses are free to override this default setup of the command line but it will serve 
	 * the purpose.
	 * 
	 * @param cmd
	 *            the command line to add arguments to.
	 */
	protected void setupArguments(Commandline cmd) {
		// determine the formating option
		if (getFormat() != null) {
			cmd.createArgument().setValue(FLAG_FMT);
			cmd.createArgument().setValue(getFormat());
		} else {
			if(getFormatLong())	{
				cmd.createArgument().setValue(FLAG_LONG);
			} else if(formatShort) {
				cmd.createArgument().setValue(FLAG_SHORT);
			}
		}
		
		addExtraArgs(cmd);
		
		//only add object selector if the selector if set.
		if(getObjSelect2() != null)
		{
			StringBuilder objSelector = new StringBuilder();
			if (appendPath && getViewPath() != null && getViewPath().length() > 0) {
				objSelector.append(getViewPath());
	
				if (!(getViewPath().endsWith("/") || getViewPath().endsWith("\\"))) {
					objSelector.append(System.getProperty("file.separator"));
				}
			}
			objSelector.append(getObjSelect2());
			cmd.createArgument().setValue(objSelector.toString());
		}
	}

	/**
	 * Add the extra arguments to the command line.
	 * 
	 * @param cmd the command lot append arguments to.
	 */
	protected void addExtraArgs(Commandline cmd) {
		if(extraArgs != null && extraArgs.length > 0)
		{
			for (int i = 0; i < extraArgs.length; i++)
			{
				cmd.createArgument().setValue(extraArgs[i]);
			}
		}
	}

	/**
	 * Get the format string used by this command.
	 * @return
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * If true the long format is usd to describe output, default is false. This
	 * option is ignored if format is specified.
	 * 
	 * @return true to use long format and false for short
	 */
	public boolean getFormatLong() {
		return formatLong;
	}

	/**
	 * Set the Clearcase format string.
	 * 
	 * @param format
	 *            the format string {@link #format}.
	 */
	public void setFormat(String format) {
		this.format = format;
	}

	/**
	 * If true the long format is usd to describe output, default is false. This
	 * option is ignored if format is specified.
	 * 
	 * @param formatLong
	 *            true to use long format and false for short
	 */
	public void setFormatLong(boolean formatLong) {
		this.formatLong = formatLong;
	}
	
	/**
	 * Specifies the -s format, mutually exclusive to log or format.
	 * 
	 * @param formatShort
	 */
	public void setShort(boolean formatShort)
	{
		this.formatShort = formatShort;
	}
	
	/**
	 * Simple getter.
	 */
	public boolean getShort()
	{
		return this.formatShort;
	}

}

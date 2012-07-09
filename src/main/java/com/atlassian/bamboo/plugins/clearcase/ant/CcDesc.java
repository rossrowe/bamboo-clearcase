package com.atlassian.bamboo.plugins.clearcase.ant;


/**
 * Use to execute the cleartool descibe command on ClearCase object.
 * <p>
 * Parameter descriptions: <table border="1">
 * <tr>
 * <th>Attribute</th>
 * <th>Values</th>
 * <th>Required</th>
 * </tr>
 * <tr>
 * <td>viewpath</td>
 * <td>The location of the view with the object to describe. IF specified then
 * the view path is appended before objectsel </td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>objselect</td>
 * <td>The ClearCase object to describe. If the viewpath is specified it is
 * appended before the this name to the command.</td>
 * <td>Yes</td>
 * </tr>
 * <tr>
 * <td>format</td>
 * <td>The -fmt string to pass to the describe command.</td>
 * <td>No</td>
 * <tr>
 * <tr>
 * <td>formatLong</td>
 * <td>If true set -l ie long foramt. if false then no format is used (ie use command default)</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>output</td>
 * <td>Name of the property to store command output into.</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>failonerr</td>
 * <td>Throw an exception if the command fails. Default is true</td>
 * <td>No</td>
 * <tr> </table>
 */
public class CcDesc extends ClearToolListCommand {

	private static final long serialVersionUID = -4610860605012057417L;

	/**
	 * Default construct setting up the command as 'describe'.
	 */
	public CcDesc()
	{
		super(COMMAND_DESC);
	}

}

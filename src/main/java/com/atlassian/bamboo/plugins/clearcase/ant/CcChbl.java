/**
 * 
 */
package com.atlassian.bamboo.plugins.clearcase.ant;

import org.apache.tools.ant.types.Commandline;

/**
 * Run the cleartool chbl command. Currently only supports setting a baselines
 * promotion level.
 * 
 * <tr>
 * <th>Attribute</th>
 * <th>Values</th>
 * <th>Required</th>
 * </tr>
 * <tr>
 * <td>level</td>
 * <td>The promotion level to set for the baseline. the basleine is speficied by 
 * objselect2</td>
 * <td>Yes (in current implementaiton as this is only thing it supports</td>
 * </tr>
 * <tr>
 * <td>objselect2</td>
 * <td>The ClearCase baseline selector. </td>
 * <td>Yes</td>
 * </tr>
 * <tr> </table>
 */
public class CcChbl extends ClearToolListCommand {

	private static final long serialVersionUID = -8388468726386958817L;

	/**
	 * The promotion level to set on the baseline
	 */
	private String level;
	
	/**
	 * The 'chbl' command
	 */
	public static final String COMMAND_CHBL = "chbl";

	private static final String OPT_LEVEL = "-level";

	/**
	 * 
	 */
	public CcChbl() {
		super(COMMAND_CHBL);
	}

	/**
	 * Setup the chbl arguements, . 
	 * 
	 * @see au.gov.dva.ant.cc.tasks.ClearToolListCommand#setupArguments(org.apache.tools.ant.types.Commandline)
	 */
	@Override
	protected void setupArguments(Commandline cmd) {
		cmd.createArgument().setValue(OPT_LEVEL);
		cmd.createArgument().setValue(level);
		
		cmd.createArgument().setValue(getObjSelect2());
	}

	public void setLevel(String level)
	{
		this.level =level;
	}
	
	public String getLevel()
	{
		return this.level;
	}

}

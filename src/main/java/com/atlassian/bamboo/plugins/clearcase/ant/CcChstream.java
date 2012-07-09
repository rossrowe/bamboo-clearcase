package com.atlassian.bamboo.plugins.clearcase.ant;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;

/**
 * Run the cleartool 'chstream' command to change a stream recommended baseline.
 * It support only chstream options required to recommended a baseline.
 * <p>
 * Parameters:
 * <table border="1pt">
 * <tr>
 * <th>Attribute</th><th>Description</th>
 * <th>Required</th></tr>
 * <tr>
 * <td>objSelect2</td>
 * <td>The stream selector to change the recommended baseline of.</td>
 * <td>Yes</td></tr>
 * <tr>
 * <td>recommendBaselines</td>
 * <td>Comma separated list of one or baselines to recommend for the specified stream.</td>
 * <td>Yes</td></tr>
 * </table>
 */
public class CcChstream extends AbstractCleartoolCmd {

	private static final long serialVersionUID = -3935427081144365414L;
	private static final String FLAG_RECOMMENDED = "-recommended";
	private static final String COMMAND_CHSTREAM = "chstream";
	private String recommendedBaselines;
	
	/**
	 * Default constructor setting command only.
	 */
	public CcChstream() {
		super(COMMAND_CHSTREAM);
	}

	/* (non-Javadoc)
	 * @see com.atlassian.bamboo.plugins.clearcase.ant.AbstractCleartoolCmd#setupArguments(org.apache.tools.ant.types.Commandline)
	 */
	@Override
	protected void setupArguments(Commandline commandLine)
			throws BuildException {
		if(StringUtils.isBlank(getObjSelect2())) {
			throw new BuildException("The stream was not specified by 'objselect2' attribute", getLocation());
		}
		if(StringUtils.isBlank(recommendedBaselines)) {
			throw new BuildException("The 'recommededBaselines' attribute must be specified.");
		}
		
		commandLine.createArgument().setValue(FLAG_RECOMMENDED);
		commandLine.createArgument().setValue(recommendedBaselines);

		commandLine.createArgument().setValue(getObjSelect2());
	}

	/**
	 * @return the recommendBaselines
	 */
	public String getRecommendedBaselines() {
		return recommendedBaselines;
	}

	/**
	 * @param recommendBaselines the recommendBaselines to set
	 */
	public void setRecommendedBaselines(String recommendedBaselines) {
		this.recommendedBaselines = recommendedBaselines;
	}

}

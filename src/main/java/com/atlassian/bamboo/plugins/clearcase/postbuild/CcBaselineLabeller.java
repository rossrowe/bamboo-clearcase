package com.atlassian.bamboo.plugins.clearcase.postbuild;

import java.util.Map;

import com.atlassian.bamboo.build.Buildable;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.jetbrains.annotations.NotNull;

import com.atlassian.bamboo.build.CustomBuildCompleteAction;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.plugins.clearcase.ant.CcChbl;
import com.atlassian.bamboo.plugins.clearcase.ant.CcChstream;
import com.atlassian.bamboo.plugins.clearcase.ant.ClearCaseUtils;
import com.atlassian.bamboo.plugins.clearcase.repository.CcRepository;
import com.atlassian.bamboo.results.BuildResults;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.utils.error.SimpleErrorCollection;
import com.atlassian.bamboo.v2.build.BaseConfigurablePlugin;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;

/**
 * This is a post build action that setting clearCase baseline attributes
 * indicating if baseline build was good or bad. This action only fires if the
 * repository for the build was instance of {@link CcRepository}
 * <p>
 * Current Attributes set on ClearCase baseline are: <table border=1>
 * <tr>
 * <th>Attribute</th>
 * <th>SUCCESS</th>
 * <th>FAILED</th>
 * </tr>
 * <tr>
 * <td>Promotion Level</td>
 * <td>BUILT</td>
 * <td>REJECTED</td>
 * </tr>
 * <tr>
 * <td>buildDate</td>
 * <td colspan=2>The date the baseline was built</td>
 * </tr>
 * <tr>
 * <td>buildNumber</td>
 * <td colspan=2>The build number of the bamboo build that built the baseline</td>
 * </tr>
 * </table>
 * <p>
 * TODO future version may allow the promotion level for build success or
 * failure to be edited. 
 * 
 * TODO allow success to include to exclude test results.
 */
public class CcBaselineLabeller extends BaseConfigurablePlugin implements
		CustomBuildCompleteAction {

	private static final String PREFIX = "custom.cc.baselinelabeller.";

	private static final String FIELD_ENABLED = PREFIX + "enabled";
	private static final String FIELD_RECOMMEND = PREFIX + "recommend";
	private static final String FIELD_PL_SUCCESS = PREFIX + "pl.success";
	private static final String FIELD_PL_FAILED = PREFIX + "pl.fail";
	private static final String PL_FAILED_DEFAULT = "REJECTED";
	private static final String PL_SUCCESS_DEFAULT = "BUILT";

	private Logger log = Logger.getLogger(CcBaselineLabeller.class);

	/**
	 * If true on build success the built baseline becomes the recommended
	 * baseline. *
	 */
	private boolean enabled = false;

	private Project dummy = ClearCaseUtils.getAntProject();


	/**
	 * Get the promotion level configured based on build result, if not
	 * promotion level is found in config then use defaults.
	 * 
	 * @param successfulBuild
	 *            true if the build was successful, false if failed.
	 * @param config
	 *            the configuration map.
	 * @return the promotion level string, should never be null.
	 */
	private String getPromotionLevel(boolean successfulBuild,
			Map<String, String> config) {
		String rval;
		String key;
		if (successfulBuild) {
			rval = PL_SUCCESS_DEFAULT;
			key = FIELD_PL_SUCCESS;
		} else {
			rval = PL_FAILED_DEFAULT;
			key = FIELD_PL_FAILED;
		}
		if (config.containsKey(key)) {
			String value = config.get(key);
			if (StringUtils.isNotBlank(value)) {
				rval = value;
			}
		}
		return rval;
	}

	/**
	 * Check if this labeller was enabled at as part of the project
	 * configuration.
	 * 
	 * @param config
	 *            the custom configuration map object.
	 * @return true if enable, false otherwise.
	 */
	private boolean isEnabled(Map<String, String> config) {
		return config.containsKey(FIELD_ENABLED)
				&& Boolean.parseBoolean(config.get(FIELD_ENABLED));
	}

	/**
	 * Get the ClearCase baseline that was just built.
	 * 
	 * @param build
	 * @return
	 */
	private String getBuiltBaseline(Buildable build) {
        CcRepository repo = (CcRepository) build.getBuildDefinition().getRepository();
        return repo.getLastBuiltBaseline();
	}

	/**
	 * True is repository configured for the build is ClearCase Repository.
	 * 
	 * @param build
	 *            the build to test.
	 * @return true if ClearCase, false if not.
	 */
	private boolean isClearCaseRepository(Buildable build) {
		boolean rval = false;
		if (build != null) {
			rval = build.getBuildDefinition().getRepository() instanceof CcRepository;
		}

		return rval;

	}

	/**
	 * Validate (non-Javadoc)
	 * 
	 */
	public ErrorCollection validate(BuildConfiguration config) {
		ErrorCollection rval = new SimpleErrorCollection();
		log.debug("validate: " + config.asXml());
		enabled = config.getBoolean(FIELD_ENABLED);

		if (enabled) {

		}

		return rval;
	}

    public void run(@NotNull Buildable build, @NotNull BuildResults buildResult) {
		Map<String, String> config = build.getBuildDefinition()
				.getCustomConfiguration();

		if (isClearCaseRepository(build) && isEnabled(config)) {
			String promotion = getPromotionLevel(BuildState.SUCCESS
					.equals(buildResult.getBuildState()), config);
			String baseline = getBuiltBaseline(build);
			log.debug("Build [" + build.getBuildName() + "] buildNumber["
					+ buildResult.getBuildNumber() + "] promotion Level["
					+ promotion + "] baseline [" + baseline + "]");

			CcChbl chbl = new CcChbl();
			chbl.setProject(dummy);
			chbl.setLevel(promotion);
			chbl.setObjSelect2(baseline);
			chbl.execute();

			if (config.containsKey(FIELD_RECOMMEND)
					&& Boolean.parseBoolean(config
							.get(FIELD_RECOMMEND))) {
				log.info("### Recommending baseline " + baseline + " Build ["
						+ build.getBuildName() + "] buildNumber["
						+ buildResult.getBuildNumber() + "]");
				CcRepository repo = (CcRepository) build.getBuildDefinition()
						.getRepository();
				CcChstream chstream = new CcChstream();
				chstream.setProject(dummy);
				chstream.setRecommendedBaselines(baseline);
				chstream.setObjSelect2(repo.getIntStream());
				chstream.execute();
			}
		}
    }
}

package com.atlassian.bamboo.plugins.clearcase.vcsversion;

import com.atlassian.bamboo.build.CustomPreBuildAction;
import com.atlassian.bamboo.plugins.clearcase.repository.CcRepository;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.RepositoryDefinition;
import com.atlassian.bamboo.results.BuildResults;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.atlassian.bamboo.v2.build.task.AbstractBuildTask;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;

/**
 * Stores the Repository version for current build in the
 * {@link BuildResults#getCustomBuildData()} map under {{@link #REVISION_KEY} as
 * a String. The value stored here is retrieved by calling
 * {@link Repository#getCurrentVersion(Build)}.toString().
 * <p/>
 * <strong>Note:</strong> The VcsversionReader pre action supplied by Bamboo was
 * not appropriate as is stores the version key under different string based on
 * the subclass of Repository that is associated with build. So any new
 * Repository types require this class to be modified and any action that depend
 * on it must know the repository type...... Hence this class was created.
 */
public class VersionWriter extends AbstractBuildTask implements
        CustomPreBuildAction {

    public static final String REVISION_KEY = "custom.repository.revision.number";

    /**
     * No validation required when the build is changed.
     */
    public ErrorCollection validate(BuildConfiguration config) {
        return null;
    }

    /**
     * Adds the CurrentVersion to the buildResults customBuildData under the
     * {@link #REVISION_KEY}. The key will noT be added if the
     * {@link Repository#getCurrentVersion(Build)} returns <code>null</code>.
     * <p/>
     * TODO Restore the logic in this class
     *
     * @see com.atlassian.bamboo.build.CustomPreBuildAction#run(com.atlassian.bamboo.build.Build,
     *      com.atlassian.bamboo.results.BuildResults)
     */
    public BuildContext call() throws InterruptedException, Exception {
        CurrentBuildResult buildResult = buildContext.getBuildResult();

        for (RepositoryDefinition repositoryDefinition : buildContext.getRepositoryDefinitions()) {
            Repository repository = repositoryDefinition.getRepository();

            // this is a hack - previously the Repository interface exposed a
            // getCurrentVersion(build) method.
            // since this method has been removed in Bamboo 2.0, we explicitly cast
            // to a CcRepository
            if (repository instanceof CcRepository
                    && ((CcRepository) repository).getClearCaseType().equals(
                    CcRepository.CC_TYPE_UCM)) {
                CcRepository ccRepository = (CcRepository) repository;

                Object latestRevision = ccRepository.getLastBuiltBaseline();

                if (latestRevision != null) {
                    buildResult.getCustomBuildData().put(REVISION_KEY,
                            latestRevision.toString());
                }
            }
        }
        return buildContext;
    }

    public void init(BuildContext buildContext) {
        this.buildContext = buildContext;
    }

}

package com.atlassian.bamboo.plugins.clearcase.repository;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.atlassian.bamboo.build.Build;
import com.atlassian.bamboo.build.DefaultBuild;
import com.atlassian.bamboo.commit.Commit;
import com.atlassian.bamboo.plugins.clearcase.ClearCaseTestCase;
import com.atlassian.bamboo.plugins.clearcase.ant.ClearCaseUtils;
import com.atlassian.bamboo.plugins.clearcase.utils.ValidationException;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;

/**
 * Test the {@link CcRepository} class.
 * Note, many tests in her rely on having ClearCase client installed on your machine and certain 
 * projects existing. To avoid failing when pre Conditions are not meet the test should call
 * {{@link #isClearCaseInstalled()} and stop test if the answer is no eg.
 * <pre><code>
 *   if (!isClearCaseInstalled())
 *   {
 *       return;
 *   }
 *   assert....
 * </pre></code>
 * 
 */
public class CcRepositoryTest extends ClearCaseTestCase {

	private CcRepository repo = new CcRepository();
	private BuildConfiguration config;
	
	/**
	 * @throws java.lang.Exception
	 */
	public void setUp() throws Exception {
		//configure with some good testing values
		repo.setBuildStream("stream:build_ro_test1@\\pacis");
		repo.setViewLocation("d:\\projects\\build_ro_test1");
		repo.setIntStream("stream:test1_Integration@\\pacis");
		config = new BuildConfiguration();
	
}

	/**
	 * @throws java.lang.Exception
	 */
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link au.gov.dva.bamboo.cc.repository.CcRepository#validate(com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration)}.
	 */
	public void testValidate() {
		BuildConfiguration bc = new BuildConfiguration();
		
		// ensure it is not configured with values.
		repo = new CcRepository();
		ErrorCollection errors = repo.validate(bc);
		assertTrue("Mandatory fields",errors.hasAnyErrors());
		String errMsg =  (String) errors.getErrors().get(CcRepository.CC_PROJECT);
		assertNotNull(errMsg);
		
		repo.addDefaultValues(bc);
		errors = repo.validate(bc);
		assertTrue("Auto Create errors",errors.hasAnyErrors());
		errMsg =  (String) errors.getErrors().get(CcRepository.CC_PROJECT);
		assertNotNull("Project error",errMsg);
		
		errMsg = (String)errors.getErrors().get(CcRepository.CC_STORAGE_DIR);
	}
	
	/**
	 * Test creating a new build stream and view.
	 * @throws RepositoryException 
	 * @throws IOException 
	 * @throws ValidationException 
	 */
	public void testAutoCreate() throws RepositoryException, IOException, ValidationException
	{
		// cleanup existing test stream and view.
		removeTestAtrifacts(ClearCaseUtils.getAntProject());
		repo.setAutoCreate(true);
		repo.setBuildStream(getNewTestStream());
		repo.setViewLocation(getNewViewPath());
		repo.setViewStorageDir(getTestVwsDir());
		repo.setIntStream(TEST_INT_STREAM);
		repo.setMainComponent(getTestMainComponent());
		repo.retrieveSourceCode("Test", "Test");
		
		File view = new File(getNewViewPath());
		assertTrue("Build view exists and is a directory"+getViewPath(),view.exists() && view.isDirectory());
		String[] files = view.list();
		assertTrue("At least 2 files within as should have view.dat an at least 1 more.",files.length > 1);
	}
	
	private Build getBuild()
	{
		DefaultBuild rval = new DefaultBuild();
		return rval;
	}
	
	/**
	 * Test behaviour with autocreate settings as true.
	 */
	public void testPrepareConfigObjectAutoCreateTrue()
	{
		// used as part to set the location the view will be created in.
		File workDir = new File(System.getProperty("java.io.tmpdir"));
		repo.setWorkingDir(workDir);
		
		config.setProperty(CcRepository.CC_PROJECT,getTestProject());
		config.setProperty(CcRepository.CC_AUTO_CREATE, "true");
		config.setProperty(CcRepository.CC_BUILD_PREFIX, "build_ro_");
		repo.prepareConfigObject(config);
		if(isClearCaseInstalled())
		{
			// relies on ClearCase test project being configured an know.
			assertEquals("Discovered In stream", TEST_INT_STREAM, config.getString(CcRepository.CC_INT_STREAM));
		}
		
		assertEquals("Build StreamName","stream:build_ro_test1"+PVOB, config.getString(CcRepository.CC_BUILD_STREAM));
		assertEquals("",workDir.toString()+File.separator+"build_ro_test1",config.getString(CcRepository.CC_VIEW_LOCATION));
		
	}

	/**
	 * No setting specified, ie does nothing.
	 */
	public void testPrepareConfigObjectNoSettings()
	{
		repo.prepareConfigObject(config);
		assertEquals("No new keys added as project is mising",config.getKeys().hasNext(), false);
	}

	
	public void testAddChangeSummary() throws RepositoryException	{
		repo.setViewLocation("C:\\dev\\projects\\bamboo\\xml-data\\build-dir\\build_ro2_Rel5_070300");
		String curentBL = "baseline:Rel5_070300_26_10_2007@\\Curam";
		String newBL = "baseline:Rel5_070300_26_10_2007.907@\\Curam";
		
		List<Commit> changeList = new ArrayList<Commit>();
		repo.addChangeSummary(curentBL,newBL,changeList, "Test");
		assertEquals("numbers of changes",2,changeList.size());
	}
	
}

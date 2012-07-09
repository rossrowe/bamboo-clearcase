package com.atlassian.bamboo.plugins.clearcase.ant;

import org.apache.tools.ant.BuildException;

import com.atlassian.bamboo.plugins.clearcase.ClearCaseTestCase;

/**
 * Test the ClearCase Describe Ant task.
 */
public class CCDescTest extends ClearCaseTestCase{

	/**
	 * @throws java.lang.Exception
	 */
	public void setUp() throws Exception {
	}


	/**
	 * @throws java.lang.Exception
	 */
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link au.gov.dva.ant.cc.tasks.CcDesc#execute()}.
	 */
	public void testFoundFile() {
		System.out.println("### Executing method: testFoundFile  ###");
		if (!isClearCaseInstalled()) {
			// stop this test if ClearCase is not installed.
			return;
		}

		CcDesc desc = new CcDesc();
		setProject(desc);
		desc.setViewPath(getViewPath());
		desc.setObjSelect2("acis/test/Module1/build.xml/");
		desc.execute();
		assertTrue("output cotains file name",desc.getCommandOutput().contains("build.xml"));
		
		desc.setFormat("%En");
		desc.execute();
		
		assertTrue("element name only, test -fmt, @@ is name and version seperator",!desc.getCommandOutput().contains("@@"));
	}

	/**
	 * Test build throws an exception when the ClearCase Command fails.
	 */
	public void testBadCommand() {
		System.out.println("### Executing method: testBadCommand ###");
		AbstractCleartoolCmd desc = new CcDesc();
		setProject(desc);
		
		desc.setViewPath("D:/badviewfolder");
		desc.setObjSelect2("Curam/version.txt");
		try {
			desc.execute();
			fail("Build command should fail");
		} catch (BuildException be) {

			// should get here as the command failed.
		}
		
		desc.setViewPath(getViewPath());
		//now turn off the Error on fail and we should have success.
		try {
			desc.setFailOnErr(false);
			desc.execute();
			// all good.
		} catch (BuildException be) {

			be.printStackTrace();
			fail("This error should have been supressed. "+ be.getMessage());
		}
	}

}

package com.atlassian.bamboo.plugins.clearcase.ant;

import java.util.Set;

import org.apache.tools.ant.Project;

import com.atlassian.bamboo.plugins.clearcase.ClearCaseTestCase;

/**
 * Test creating a new build stream, snapshot view and adding appropriate
 * load rules to newly created view.
 * <p>
 * Is really an integration test.
 */
public class CcBuildAtrifactCreatorTest extends ClearCaseTestCase {


	private Project proj = ClearCaseUtils.getAntProject();

	/**
	 * Remove load rules from an exiting view
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		if(getName().equals("testAddLoadRules")) {
			removeTestAtrifacts(proj);
		}
		
		super.setUp();
	}

	public void testAddLoadRules()
	{
		makeStream(proj);
		Set<String> before = ClearCaseUtils.getCurrentLoadRules(getNewViewTag(), proj);
		
		CcUpdateLoadRules uplr = new CcUpdateLoadRules();
		uplr.setProject(ClearCaseUtils.getAntProject());
		uplr.setBaseline("070816_165528_test1_chamms.8268@\\pacis");
		uplr.setViewPath(getNewViewPath());
		uplr.execute();
		Set<String> after = ClearCaseUtils.getCurrentLoadRules(getNewViewTag(), proj);
		assertTrue("Rules added "+before.size()+" "+after.size(),after.size() > before.size());

		// test not adding any new rules.
		before =  after;
		uplr = new CcUpdateLoadRules();
		uplr.setProject(ClearCaseUtils.getAntProject());
		uplr.setBaseline("070816_165528_test1_chamms.8268@\\pacis");
		uplr.setViewPath(getNewViewPath());
		uplr.execute();
		after = ClearCaseUtils.getCurrentLoadRules(getNewViewTag(), proj);
		assertEquals("Rules same",before, after);
		
	}

	private void makeStream(Project proj) {
		CcStreamCreator sc = new CcStreamCreator();
		sc.setProject(proj);
		sc.setVws(getNewViewVwsDir());
		sc.setViewLocation(getNewViewPath());
		sc.setViewTag(getNewViewTag());
		sc.setIntStream(TEST_INT_STREAM);
		sc.setStreamName(getNewTestStream());
		sc.execute();
		
	}

}

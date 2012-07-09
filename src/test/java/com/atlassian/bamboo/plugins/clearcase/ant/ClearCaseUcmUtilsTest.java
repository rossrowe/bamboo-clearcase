package com.atlassian.bamboo.plugins.clearcase.ant;


import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import com.atlassian.bamboo.plugins.clearcase.ClearCaseTestCase;

public class ClearCaseUcmUtilsTest extends ClearCaseTestCase {

	
	public void getStreamBaselines()
	{
		if(!isClearCaseInstalled())	return;
		Project p = ClearCaseUtils.getAntProject();
		List<String> baselines = ClearCaseUtils.getStreamBaselines(TEST_INT_STREAM,NR_COMP, p, "");
		assertTrue("number of baselines", baselines.size() > 4);
	
	}
	
	public void testGetFoundationBaseline()
	{
		if(!isClearCaseInstalled())	return;
		Project p = ClearCaseUtils.getAntProject();
		List<String> baselines = ClearCaseUtils.getFoundationBaselines(TEST_BUILD_STREAM, p, "");
		assertEquals("number of baselines", 1, baselines.size());

		try
		{
			baselines = ClearCaseUtils.getFoundationBaselines("stream:badname"+PVOB, p, "");
			assertEquals("number of baselines", 0, baselines.size());
		} catch (BuildException be)
		{
			assertTrue("Contains not found in exception ["+be.getMessage()+"]",be.getMessage().toLowerCase().indexOf("stream not found") >= 0); 
		}

	}

}

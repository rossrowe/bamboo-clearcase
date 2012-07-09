package com.atlassian.bamboo.plugins.clearcase.ant;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;

import org.apache.tools.ant.BuildException;

import com.atlassian.bamboo.plugins.clearcase.ClearCaseTestCase;

/**
 * Test the CCdiffbl class.
 * 
 */
public class CCDiffblTest extends ClearCaseTestCase {

	/**
	 * Test basic a basic diff
	 */
	public final void testDiffArgs() {
		if (!isClearCaseInstalled()) {
			// stop this test if ClearCase is not installed.
			return;
		}

		CcDiffbl diff = new CcDiffbl();
		setProject(diff);

		diff.setSelector1("baseline:070803_144951_test1_chamms.6779@\\pacis");
		diff.setSelector2("stream:test1_Integration@\\pacis");

		diff.setActivities(true);
		diff.execute();
		assertNotNull(diff.getCommandOutput());
		assertTrue("Not empty output", diff.getCommandOutput().trim()
				.length() > 0);

		diff.setVersions(true);
		diff.setViewPath(getViewPath());
		diff.execute();
		LineNumberReader lr = new LineNumberReader(new StringReader(diff.getCommandOutput()));
		String line;
		try {
			assertNotNull("Activity name line", (line = lr.readLine()));
			assertTrue("Activity found >>", line.startsWith(">>"));
			
			assertNotNull("Followed by version line", (line = lr.readLine()));
			lr.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			fail("Error reading diffbl output");
		}
	}

	/**
	 * Test bad argument configuration, eg version specfied but viewpath not
	 * set.
	 */
	public final void testBadArguments() {
		if (!isClearCaseInstalled()) {
			// stop this test if ClearCase is not installed.
			return;
		}

		CcDiffbl diff = new CcDiffbl();
		setProject(diff);

		diff.setActivities(true);
		diff.setVersions(true);

		try {
			diff.execute();
			fail("No viewpath casues an error");
		} catch (BuildException be) {
			assertTrue("Missing viewPath", be.getMessage().contains(
					"Missing 'ViewPath' attribute"));
		}
	}

}

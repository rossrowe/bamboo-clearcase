package com.atlassian.bamboo.plugins.clearcase;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.clearcase.ClearCase;

import com.atlassian.bamboo.plugins.clearcase.ant.CcRmstream;
import com.atlassian.bamboo.plugins.clearcase.ant.CcRmview;

/**
 * Contains definition of some ClearCase helper methods.
 */
public class ClearCaseTestCase extends TestCase {

	public static final String PVOB = "@\\pacis";
	public static final String TEST_INT_STREAM = "stream:test1_Integration"+PVOB;
	public static final String TEST_BUILD_STREAM = "stream:build_ro_test1"+PVOB;
	public static final String NR_COMP = "test_nr"+PVOB;

	
	/** The values used by different test cases/ **/
	private static final Properties testProps = new Properties();
	
	/**
	 * The file if present that means ClearCase is on this machine. Please set
	 * property "clearcase.file if default of "d:/ccstorage" does not hold.
	 */
	private static final File ccFile = new File(System.getProperty(
			"clearcase.file", "D:/ccstorage"));

	
	/**
	 * Used to prevent test that rely on ClearCase being installed from running
	 * when ClearCase is not found.
	 * 
	 * @return true if ClearCase is installed on machine
	 */
	public boolean isClearCaseInstalled() {
		return ccFile.exists();
	}

	/**
	 * Setup a dummy ANT Project setup with console logger set to debug level.
	 * @param desc
	 */
	public void setProject(ClearCase task) {
		Project p = new Project();
		p.addBuildListener(createLogger());
		task.setProject(p);
	}

	/**
	 * Setup a default logger to write output to stdout an stderr.
	 * 
	 * @return the Ant logger.
	 */
	public BuildLogger createLogger() {
		BuildLogger logger = new DefaultLogger();

		logger.setMessageOutputLevel(Project.MSG_DEBUG);
		logger.setOutputPrintStream(System.out);
		logger.setErrorPrintStream(System.err);
		return logger;
	}

	public static final String PROP_TEST_VIEW = "cleacase.test.view";
	
	/**
	 * Get a physical ClearCase view that can be used for tests. The system property 'cleacase.test.view' can be used to
	 * change the physical value returned by this method.
	 * 
	 * @return the fully qualified path to a view
	 */
	public String getViewPath()
	{
		return System.getProperty(PROP_TEST_VIEW, "D:/projects/chamms_test1");
	}

	public static final String PROP_TEST_NEWVIEW = "cleacase.test.newview";
	/**
	 * Get a physical ClearCase view that will not exit before running tests.
	 * The system property 'cleacase.test.newview' can be used to
	 * change the physical value returned by this method.
	 * 
	 * @return the fully qualified path to a view
	 */
	public String getNewViewPath()
	{
		return System.getProperty(PROP_TEST_NEWVIEW, "D:\\projects\\bamboo\\build_test1");
	}

	public String getNewViewTag()
	{
		return "build_test1";
	}
	
	public String getNewTestStream() {
		return "stream:build_test1"+PVOB;
	}

	/**
	 * The view storage directory
	 * @return
	 */
	public String getTestVwsDir()
	{
		return "\\\\act00301296d\\ccstorage\\views";
	}
	
	/**
	 * Get the directory name to place the view storage in when creating
	 * the 'NewTestView'.
	 * @return the fully qualified view storage dire to pass to mkview command.
	 */
	public String getNewViewVwsDir() {
		return getTestVwsDir()+"\\"+getNewViewTag()+".vws";
	}

	public String getTestMainComponent()
	{
		return "component:test_nr"+PVOB;
	}
	
	/**
	 * Just remove the warning on some runners.
	 */
	public void testDummy()
	{
		
	}
	
	/**
	 * Return the ClearCase project selector for ClearCase project against which
	 * test will be run.
	 * @return
	 */
	public String getTestProject()
	{
		//TODO make this configurable by system properties. 
		return "project:test1"+PVOB;
	}

	/**
	 * Remove the readonly stream or view that might have been created by previous
	 * runs of this test.  Only runs is the {@link #getNewViewPath()} directory
	 * exists.
	 */
	public void removeTestAtrifacts(Project proj) throws IOException {
		if(new File(getNewViewPath()).exists()) {
			CcRmview rmview = new CcRmview();
			rmview.setProject(proj);
			rmview.setView(getNewViewPath());
			rmview.execute();
			
			CcRmstream rmstream = new CcRmstream();
			rmstream.setProject(proj);
			rmstream.setStream(getNewTestStream());
			
		}
	}
	
	/**
	 * Get an expected result or value that is dependent on some external 
	 * ClearCase setup. there is defaults specified specified but he values can
	 * be written to work for give ClearCase environment.
	 * 
	 * See the static Initialiser for how properties are loaded.
	 * @param key the key of the value to load.
	 * @return the value or null if not found.
	 */
	public static String getTestValue(String key) {
		return testProps.getProperty(key);
	}

	/**
	 * Same as {@link #getTestValue(String)} except you can supply a default value
	 * in the case the property is not defined.
	 * 
	 * @param key the key of the value to load.
	 * @param deflt the value if the key is not present.
	 * @return the value or deflt if not found.
	 */
	public static String getTestValue(String key, String deflt)	{
		return testProps.getProperty(key, deflt);
	}
}

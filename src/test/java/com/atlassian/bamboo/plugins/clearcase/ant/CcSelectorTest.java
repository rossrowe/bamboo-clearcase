package com.atlassian.bamboo.plugins.clearcase.ant;

import com.atlassian.bamboo.plugins.clearcase.utils.ValidationException;

import junit.framework.TestCase;


/**
 *  Test the CCProject a parsing and asSelector methods.
 */
public class CcSelectorTest extends TestCase {

	/**
	 * Test method for {@link au.gov.dva.ant.cc.tasks.CcSelector#setAsString(java.lang.String)}.
	 */
	public void testSetSelectorAsString() {
		
		CcSelector p = new CcSelector("project");
		checkSelector("Project Only", p, "Proj1","Proj1", null, "project:Proj1");

		p = new CcSelector(null);
		checkSelector("with selector", p,"project:Proj1", "Proj1", null, "project:Proj1");
		
		p = new CcSelector(null);
		checkSelector("With Pvob", p,"Proj1@\\\\pvob" , "Proj1", "pvob", "Proj1@\\pvob");

		p = new CcSelector("project");
		checkSelector("With Pvob", p,"Proj1@\\\\pvob" , "Proj1", "pvob", "project:Proj1@\\pvob");

		p = new CcSelector(null);
		checkSelector("the works", p,"project:Proj1@\\pvob" , "Proj1", "pvob", "project:Proj1@\\pvob");
		
		try
		{
			p.setAsString(null);
			fail("A null stream selecto shoudl be invalid");
		} catch (ValidationException ve)
		{
			assertTrue("validation msg",ve.getMessage().indexOf(CcSelector.MSG_NULL_SELECTOR)>= 0);
		}
	}
	
	public void checkSelector(String msg, CcSelector project, String testSelector, String pname,String pvob,String asString)
	{
		try
		{
			project.setAsString(testSelector);
		} catch (ValidationException ve)
		{
			fail(msg+": Valdiation Exception testing selectro ["+testSelector+"] " +ve.getMessage());
		}
		assertEquals(msg+": project Name",pname, project.getName());
		assertEquals(msg+": PVob",pvob, project.getVobSelector());
		assertEquals(msg+": asSelector",asString, project.asSelector());
	}

}

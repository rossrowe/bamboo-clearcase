/**
 * 
 */
package com.atlassian.bamboo.plugins.clearcase.ant;

import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.BuildException;

/**
 * @author Ross Rowe
 *
 */
public class CcRebaseCancel extends CcRebase{
	
	private static final long serialVersionUID = 7070793544963493936L;

	protected void setupArguments(Commandline cmd) {
		cmd.createArgument().setValue("-cancel");
	}
    
}

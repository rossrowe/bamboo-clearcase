package com.atlassian.bamboo.plugins.clearcase.ant;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;

/**
 * Use rmstream to remove a stream. Only attribute required is
 * 'stream'.
 * 
 */
public class CcRmstream extends AbstractCleartoolCmd {

	
	private static final long serialVersionUID = -7140035350335429320L;

	public static final String COMMAND_RMSTREAM = "rmstream";
	
	private String stream;
	

	/**
	 * Default constructor setting the command argument
	 */
	public CcRmstream() {
		super(COMMAND_RMSTREAM);
	}

	/* (non-Javadoc)
	 * @see com.atlassian.bamboo.plugins.clearcase.ant.AbstractCleartoolCmd#setupArguments(org.apache.tools.ant.types.Commandline)
	 */
	@Override
	protected void setupArguments(Commandline commandLine)
			throws BuildException {
		if (StringUtils.isBlank(getStream())) {
			throw new BuildException("Missing 'stream' attribute.", getLocation());
		}
		
		commandLine.createArgument().setValue(FLAG_FORCE);
		commandLine.createArgument().setValue(stream);
	}

	/**
	 * @return the stream
	 */
	public String getStream() {
		return stream;
	}

	/**
	 * @param stream the stream to set
	 */
	public void setStream(String stream) {
		this.stream = stream;
	}

}

package com.atlassian.bamboo.plugins.clearcase.ant;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 *  Used to create a build new build stream, by default if the 
 *  stream or view already exist this task has not effect.
 */
public class CcStreamCreator extends Task implements CleartoolCommand {
	
	public static final String NOT_A_STREAM_MSG = "stream not found";
	private String streamName;
	private String viewTag;
	private String viewLocation;
	private String vws;
	private String intStream;
	private boolean readonly = true;
    private boolean dynamicView;
    private String driveLetter;

    public void setCleartoolHome(String cleartoolHome) {
        //dummy implementation, not required
    }

    /**
	 * Coordinate creating new stream and view if they do not already exist.
	 * 
	 * @see org.apache.tools.ant.Task#execute()
	 */
	@Override
	public void execute() throws BuildException {
		checkSettings();
		if(!streamExists())
		{
			createStream();
		}
		
		if(!viewExists())
		{
			createView();
		}
	}


    /**
	 * Create a read only view on the stream.
	 */
	private void createView() {
		CcMkView  mkview = new CcMkView();
		mkview.setProject(getProject());
		mkview.setVws(getVws());
		mkview.setObjSelect2(getViewLocation());
		mkview.setViewTag(getViewTag());
		mkview.setSnapshot(!isDynamicView());
		mkview.setStream(getStreamName());
		mkview.execute();
	}

	/**
	 * Check if view exists but throws BuildExcpetion is ClearCase is in inconsistent state.
	 * ie view tag exists but viewLocation does not, most likely directory was manually deleted not using rmview.
	 * ie view tag does not exist but view location does, need to remove directory and re-run or work out what has happened..
	 * @return trues if view exist an is consistent, false if not exiting
	 */
	private boolean viewExists() {
		boolean rval = false;
		CcLsview lsview =new CcLsview();
		lsview.setProject(getProject());
		lsview.setObjSelect2(getViewTag());
		try {
			lsview.execute();
			rval = true;
		} catch (BuildException be) {
			// assume only errors when not exists.
			getProject().log("Build Excpetion as view should exist" +be.getMessage(), Project.MSG_DEBUG);
		}
		File viewDir = new File(getViewLocation());
		if(rval && !viewDir.exists()) {
			throw new BuildException("Problem as viewtag ("+getViewTag()+") exists but view dir ("+getViewLocation()+") does not, correct situation manually.");
		}

		if(rval == false && viewDir.exists()) {
			throw new BuildException("Problem as viewtag ("+getViewTag()+") does not exists but view dir ("+getViewLocation()+") does, correct situation manually.");
		}

		return rval;
	}

	/**
	 * Create the new stream.
	 */
	private void createStream() {
		CcMkStream mkstream = new CcMkStream();
		mkstream.setProject(getProject());
		mkstream.setObjSelect2(getStreamName());
		mkstream.setReadOnly(readonly);
		mkstream.setParentStream(getIntStream());
		mkstream.execute();
	}

	/**
	 * Use 'lsstream; to check if the stream exists.  The cleartool command
	 * error must include {#link {@link #NOT_A_STREAM_MSG}} to be considered
	 * not an error.
	 * @return true if stream exists.
	 */
	private boolean streamExists() {
		boolean rval = false;
		CcLsstream lsstream = new CcLsstream();
		lsstream.setProject(getProject());
		lsstream.setObjSelect2(getStreamName());
		try {
			lsstream.execute();
			rval = true;
		} catch (BuildException be) {
			if(lsstream.getCommandOutput().toLowerCase().indexOf(NOT_A_STREAM_MSG) == -1)
			{
				throw be;
			}
			// get here the error was stream not exists, so continue.
		}
		return rval;
	}

	/**
	 * Check all required arguments are set.
	 */
	private void checkSettings() {
		//TODO add checks
	}

	/**
	 * @return the intStream
	 */
	public String getIntStream() {
		return intStream;
	}

	/**
	 * @param intStream the intStream to set
	 */
	public void setIntStream(String intStream) {
		this.intStream = intStream;
	}

	/**
	 * Ant attribute style getter.
	 * @return the readonly
	 */
	public boolean getReadonly() {
		return readonly;
	}

	/**
	 * @param readonly the readonly to set
	 */
	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	/**
	 * @return the streamName
	 */
	public String getStreamName() {
		return streamName;
	}

	/**
	 * @param streamName the streamName to set
	 */
	public void setStreamName(String streamName) {
		this.streamName = streamName;
	}

	/**
	 * @return the viewLocation
	 */
	public String getViewLocation() {
		return viewLocation;
	}

	/**
	 * @param viewLocation the viewLocation to set
	 */
	public void setViewLocation(String viewLocation) {
		this.viewLocation = viewLocation;
	}

	/**
	 * @return the viewTag
	 */
	public String getViewTag() {
		return viewTag;
	}

	/**
	 * @param viewTag the viewTag to set
	 */
	public void setViewTag(String viewTag) {
		this.viewTag = viewTag;
	}

	/**
	 * @return the vws
	 */
	public String getVws() {
		return vws;
	}

	/**
	 * @param vws the vws to set
	 */
	public void setVws(String vws) {
		this.vws = vws;
	}


    public void setDynamicView(boolean dynamicView) {
        this.dynamicView = dynamicView;
    }

    public boolean isDynamicView() {
        return dynamicView;
    }

    public String getDriveLetter() {
        return driveLetter;
    }

    public void setDriveLetter(String driveLetter) {
        this.driveLetter = driveLetter;
    }
}

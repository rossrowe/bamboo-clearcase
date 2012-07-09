package com.atlassian.bamboo.plugins.clearcase.ant;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;

/**
 * Run cleartool 'mkview' to create a new view.
 * <p/>
 * Parameters:
 * <table border="1pt">
 * <tr>
 * <th>Attribute</th><th>Description</th>
 * <th>Required</th></tr>
 * <tr>
 * <td>objSelect2</td>
 * <td>A snapshots view path name or dynamic view view storage path</td>
 * <td>Yes</td></tr>
 * <tr>
 * <td>snapshot</td>
 * <td>If true create a snapshot viewParent of the stream to create</td>
 * <td>No, default is true</td></tr>
 * <tr>
 * <td>viewtag</td>
 * <td>View tag to associate with this view</td><td>Yes</td></tr>
 * <tr><td>stream</td>
 * <td>Specifies a UCM stream. The view being created is attached to this stream.</td>
 * <td>No</td></tr>
 * <tr>
 * <td>vws</td>
 * <td>Specifies the location for the snapshot view storage directory. On Windows systems, this must be a UNC name.</td>
 * <td>No, but Yes if snapshot view</td></tr>
 * </table>
 */
public class CcMkView extends AbstractCleartoolCmd {

    private static final long serialVersionUID = 5102417540181976649L;
    public static final String FLAG_STREAM = "-stream";
    public static final String COMMAND_MKVIEW = "mkview";
    public static final String FLAG_VWS = "-vws";
    public static final String FLAG_STGLOC = "-stgloc";
    private static final String FLAG_TAG = "-tag";
    public static final String FLAG_SNAPSHOT = "-snapshot";

    private boolean snapshot = true;
    private String viewTag;
    private String vws;
    private String stream;

    /**
     * Default constructor passing only the mkview command.
     */
    public CcMkView() {
        super(COMMAND_MKVIEW);
    }

    /* (non-Javadoc)
      * @see com.atlassian.bamboo.plugins.clearcase.ant.AbstractCleartoolCmd#setupArguments(org.apache.tools.ant.types.Commandline)
      */
    @Override
    protected void setupArguments(Commandline commandLine)
            throws BuildException {
        if (StringUtils.isBlank(getObjSelect2())) {
            throw new BuildException("The snapshot view path or dynamic view storage dir must be specified by 'objSelect2' attribute", getLocation());
        }

        if (StringUtils.isBlank(getViewTag())) {
            throw new BuildException("'viewtag' attribute is missing.", getLocation());
        }

        if (snapshot) {
            commandLine.createArgument().setValue(FLAG_SNAPSHOT);
        }
        commandLine.createArgument().setValue(FLAG_TAG);
        commandLine.createArgument().setValue(getViewTag());

        if (StringUtils.isNotBlank(getStream())) {
            commandLine.createArgument().setValue(FLAG_STREAM);
            commandLine.createArgument().setValue(stream);
        }

        if (snapshot) {
            if (StringUtils.isNotBlank(getVws())) {
                commandLine.createArgument().setValue(FLAG_VWS);
                commandLine.createArgument().setValue(getVws());
            }
            commandLine.createArgument().setValue(getObjSelect2());
        } else {
            commandLine.createArgument().setValue(getVws());
        }
    }

    /**
     * Ant style attribute setter.
     *
     * @return the snapshot
     */
    public boolean getSnapshot() {
        return snapshot;
    }

    /**
     * @param snapshot the snapshot to set
     */
    public void setSnapshot(boolean snapshot) {
        this.snapshot = snapshot;
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

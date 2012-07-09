package com.atlassian.bamboo.plugins.clearcase.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;
import org.apache.log4j.Logger;

/**
 * Used to rebase a specified stream to specified baseline
 */
public class CcRebase extends ClearToolListCommand {

    private static final long serialVersionUID = 1777682229561192455L;

    private static final Logger log = Logger.getLogger(CcRebase.class);

    /**
     * The 'describe' command
     */
    public static final String COMMAND_REBASE = "rebase";

    private String baseline;
    private String stream;

    private String view;

    public CcRebase() {
        super(COMMAND_REBASE);
    }

    /**
     * @return the baseline
     */
    public String getBaseline() {
        return baseline;
    }

    /**
     * @param baseline the baseline to set
     */
    public void setBaseline(String baseline) {
        this.baseline = baseline;
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

    /**
     * Setup the rebase command line options
     *
     */
    @Override
    protected void setupArguments(Commandline cmd) {
        cmd.createArgument().setValue("-baseline");
        cmd.createArgument().setValue(getBaseline());

        cmd.createArgument().setValue("-stream");
        cmd.createArgument().setValue(getStream());
        cmd.createArgument().setValue("-complete");
        cmd.createArgument().setValue("-force");
        cmd.createArgument().setValue("-view");
        cmd.createArgument().setValue(getView());
    }

    private String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;

    }

}

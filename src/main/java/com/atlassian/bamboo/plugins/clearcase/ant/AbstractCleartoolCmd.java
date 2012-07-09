package com.atlassian.bamboo.plugins.clearcase.ant;

import java.io.File;
import java.io.Serializable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.optional.clearcase.ClearCase;
import org.apache.tools.ant.types.Commandline;

/**
 * Class that defines standard behaviour for executing a ClearCase cleartool
 * command from Ant.
 * <p/>
 * Command behaviour provided is:
 * <ul>
 * <li>Always support failOnError attribute, ie command fails then throw error.</li>
 * <li>support storing command output in to 'output' property and making it
 * Available by {{@link #getCommandOutput()}</li>
 * </ul>
 * <p/>
 * Add the following line to log4j configuration file to turn on debug logging
 * for ant tasks used by these utilities:
 * <p/>
 * <code>log4j.logger.org.apache.tools.ant=DEBUG</code>
 * <p/>
 */
public abstract class AbstractCleartoolCmd extends ClearCase implements Serializable, CleartoolCommand {

    /**
     * The cleartool command being run.
     */
    private String command;

    /**
     * The 'describe' command
     */
    public static final String COMMAND_DESC = "describe";

    /**
     * The 'lsbl' command
     */
    public static final String COMMAND_LSBL = "lsbl";

    /**
     * The 'lsstream' command
     */
    public static final String COMMAND_LSSTREAM = "lsstream";
    /**
     * The 'lshistory' command
     */
    public static final String COMMAND_LSHISTORY = "lshistory";

    public static final String FLAG_FORCE = "-force";

    /**
     * A unique number to append to properties for saving output. *
     */
    private static int pcnt = 0;

    private String exitCodeProp;

    private String output;

    private String outputProp = "";

    private String mobjSelect = null;

    private boolean mFailonerr = true;

    private String cleartoolHome;

    /**
     * Constructor specify the cleartool command to run.
     *
     */
    public AbstractCleartoolCmd() {
        super();
    }

    /**
     * Constructor specify the cleartool command to run.
     *
     * @param command the cleartool command name.
     */
    public AbstractCleartoolCmd(String command) {
        super();
        this.command = command;
    }

    /**
     * Setup the 'cleartool {command}' portion of the command line then
     * call abstract {#link setupCommadnArguments()} for subclasses to specify
     * remaining arguments.
     */
    private Commandline commadLineSetup() {
        Commandline commandLine = new Commandline();
        // build the command line from what we got the format is
        // cleartool command [options...] [objectSelector ...]
        // as specified in the CLEARTOOL.EXE help
        commandLine.createArgument().setValue(command);

        // Check the command line options
        setupArguments(commandLine);
        return commandLine;
    }

    /**
     * Override to setup the additional cleartool command parameters.
     *
     * @param commandLine the command line with the cleartool and command
     *                    added as first 2 arguments.
     * @throws BuildException if the arguments specified are not valid.
     */
    protected abstract void setupArguments(Commandline commandLine) throws BuildException;

    /**
     * Executes the task setup standard output and retuncode properties.
     * <p/>
     * Subclasses should Builds a command line to execute cleartool and then
     * calls Exec's run method to execute the command line.
     *
     * @throws BuildException if the command fails and failonerr is set to true
     */
    public void execute() throws BuildException {
        Commandline commandLine = commadLineSetup();
        commandLine.setExecutable(getClearToolCommand());

        int result = 0;

        // For debugging
        getProject().log(commandLine.toString(), Project.MSG_DEBUG);

        if (!getFailOnErr()) {
            getProject().log(
                    "Ignoring any errors that occur for: "
                            + getViewPathBasename(), Project.MSG_VERBOSE);
        }
        result = runI(commandLine);
        if (Execute.isFailure(result) && getFailOnErr()) {
            String msg = "Failed executing: " + commandLine.toString() + " output[" + getLastOutput() + "]";
            throw new BuildException(msg, getLocation());
        }
    }

    /**
     * Get the output of the last executed command.
     *
     * @return the output as string.
     */
    public String getCommandOutput() {
        return getProject().getProperty(outputProp);
    }

    /**
     * Return the output of the last executed command.
     *
     * @return the last command output.
     */
    public String getLastOutput() {
        return getProject().getProperty(outputProp);
    }

    /**
     * Get the currently set output property, null if not set.
     *
     * @return the output property name.
     */
    public String getOutput() {
        return this.output;
    }

    /**
     * Execute the given command, save output and return success or failure.
     * <p/>
     * The output is save to specified project property or generated on if not
     * specified. The output is also accessible from {@link #getOutput()}.
     *
     * @param cmdline command line to execute
     * @return output of the command line
     */
    protected int runI(Commandline cmdline) {
        // set each time teh command is executed to ensure the p
        setExecuteOutput();

        ExecTask exe = new ExecTask();
        exe.setProject(this.getProject());
        Commandline.Argument arg = exe.createArg();

        if (getViewPath() != null) {
            File viewDir = new File(getViewPath());
            if (viewDir.exists()) {
                exe.setDir(viewDir);
            }
        }
        exe.setExecutable(cmdline.getExecutable());
        exe.setResultProperty(exitCodeProp);
        arg.setLine(Commandline.toString(cmdline.getArguments()));
        exe.setOutputproperty(outputProp);
        exe.setFailonerror(false);
        exe.setFailIfExecutionFails(false);
        exe.execute();

        getProject().log("Command Ouput: " + getCommandOutput(),
                Project.MSG_DEBUG);

        return Integer.parseInt(getProject().getProperty(exitCodeProp));
    }

    /**
     * Called by {@link #runI(Commandline)} to set project property to store
     * output command output in. A new propetry is is generated each time if
     * the {{@link #getOutput()} property is not set to avoid then name being
     * the same between executions.
     */
    protected void setExecuteOutput() {
        outputProp = (getOutput() == null) ? "opts.cc.runI.output" + pcnt
                : getOutput();
        exitCodeProp = "opts.cc.runI.exitcode" + pcnt++;
    }

    /**
     * Store the output prioperty if specified.
     *
     * @param output
     */
    public void setOutput(String output) {
        this.output = output;
    }

    /**
     * Set the object to operate on.
     *
     * @param objSelect object to operate on
     * @since ant 1.6.1 method setObjSelect was added as final to I called this
     *        setObjSelect2 to avboid issues usign older and newer versions of ant.
     */
    public void setObjSelect2(String objSelect) {
        mobjSelect = objSelect;
    }

    /**
     * Get the object to operate on
     *
     * @return mobjSelect
     */
    public String getObjSelect2() {
        return mobjSelect;
    }

    /**
     * If true, command will throw an exception on failure.
     *
     * @param failonerr the status to set the flag to
     * @since ant 1.6.1
     */
    public void setFailOnErr(boolean failonerr) {
        mFailonerr = failonerr;
    }

    /**
     * Get failonerr flag status
     *
     * @return boolean containing status of failonerr flag
     * @since ant 1.6.1
     */
    public boolean getFailOnErr() {
        return mFailonerr;
    }

    /**
     * Get the basename path of the item in a clearcase view
     *
     * @return basename
     */
    public String getViewPathBasename() {
        return (new File(getViewPath())).getName();
    }


    public String getCleartoolHome() {
        return cleartoolHome;
    }

    public void setCleartoolHome(String cleartoolHome) {
        this.cleartoolHome = cleartoolHome;
        setClearToolDir(cleartoolHome);
    }
}
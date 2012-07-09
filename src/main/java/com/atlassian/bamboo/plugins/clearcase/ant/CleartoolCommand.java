package com.atlassian.bamboo.plugins.clearcase.ant;

/**
 * @author Ross Rowe
 */
public interface CleartoolCommand {
    void setCleartoolHome(String cleartoolHome);

    void execute();

    String getTaskName();
}

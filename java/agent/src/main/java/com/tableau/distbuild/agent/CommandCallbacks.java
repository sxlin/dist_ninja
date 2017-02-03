package com.tableau.distbuild.agent;

public interface CommandCallbacks {
    void commandSuccess(Command command);
    void commandFailed(Command command);
    void commandFailed(Command command, String message);
}

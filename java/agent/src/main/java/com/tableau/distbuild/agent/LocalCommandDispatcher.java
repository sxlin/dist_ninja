package com.tableau.distbuild.agent;

public class LocalCommandDispatcher implements CommandDispatcher {
    private Command command;
    private CommandCallbacks callbacks;

    public LocalCommandDispatcher(Command command, CommandCallbacks callbacks) {
        this.command = command;
        this.callbacks = callbacks;
    }

    @Override
    public void run() {
        command.setState(CommandState.RUNNING);
        // XXX - Actually run the command
        command.setState(CommandState.DONE);
        callbacks.commandSuccess(command);
    }
}

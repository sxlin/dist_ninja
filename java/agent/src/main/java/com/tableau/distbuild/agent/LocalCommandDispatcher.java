package com.tableau.distbuild.agent;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;

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
        try {
            System.out.println("Starting local command");
            Process p = runCommand();
            p.waitFor();
            System.out.println("Local command finished");
            command.setState(CommandState.DONE);
            if (p.exitValue() == 0) {
                BufferedReader stdin = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = stdin.readLine()) != null) {
                    System.out.println("Command output: " + line);
                }
                callbacks.commandSuccess(command);
            } else {
                BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                StringBuffer errorMsg = new StringBuffer();

                String line;
                while ((line = stderr.readLine()) != null) {
                    errorMsg.append(line + "\n");
                }
                callbacks.commandFailed(command, errorMsg.toString());
            }
        } catch (IOException e) {
            System.err.println("Failed to launch process: " + e.getMessage());
            command.setState(CommandState.DONE);
            callbacks.commandFailed(command, e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Process interrupted: " + e.getMessage());
            command.setState(CommandState.DONE);
            callbacks.commandFailed(command, e.getMessage());
        }
    }

    private Process runCommand() throws IOException {
        LinkedList<String> pbargs = new LinkedList<>(command.getArgs());
        pbargs.addFirst(command.getCommand());

        ProcessBuilder builder = new ProcessBuilder(pbargs);
        String workingDir = command.getWorkingDirectory();
        if (workingDir != null) {
            builder.directory(new File(workingDir));
        }
        return builder.start();
//        return Runtime.getRuntime().exec(command.getCommand());
    }
}

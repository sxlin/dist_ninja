package com.tableau.distbuild.agent;

import com.tableau.distbuild.protobuf.DistBuildProtos.CommandRequest;
import com.tableau.distbuild.protobuf.DistBuildProtos.CommandResponse;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class NetworkCommand implements Command {
    private Socket clientSocket;
    private CommandRequest command;
    private CommandResponse.Builder responseBuilder;
    private CommandState state;


    public NetworkCommand(Socket clientSocket, CommandRequest command) {
        this.clientSocket = clientSocket;
        this.command = command;
        this.state = CommandState.INIT;
        this.responseBuilder = CommandResponse.newBuilder();
    }

    @Override
    public String getCommand() {
        return command.getCommand();
    }

    @Override
    public List<String> getArgs() {
        return command.getArgsList();
    }

    @Override
    public String getWorkingDirectory() {
        if (command.hasWorkingDir()) {
            return command.getWorkingDir();
        }
        return null;
    }

    @Override
    public CommandResponse.Status getStatus() {
        return responseBuilder.getStatus();
    }

    @Override
    public void setStatus(CommandResponse.Status status) {
        responseBuilder.setStatus(status);
    }

    @Override
    public void setMessage(String message) {
        responseBuilder.setMessage(message);
    }

    @Override
    public CommandState getState() {
        return state;
    }

    @Override
    public void setState(CommandState state) {
        this.state = state;
    }

    @Override
    public void sendResponse() throws IOException {
        if (state != CommandState.DONE) {
            throw new IllegalStateException("Sending response before command is done");
        }
        responseBuilder.setId(command.getId());
        responseBuilder.build().writeDelimitedTo(clientSocket.getOutputStream());
    }
}

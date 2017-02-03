package com.tableau.distbuild.agent;

import com.tableau.distbuild.protobuf.DistBuildProtos;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkServer implements CommandServer, CommandCallbacks {
    static private int DEFAULT_SERVER_PORT = 7821;
    static private int MAX_RUNNING_COMMANDS = 20;

    private ServerCallbacks callbacks;
    private ServerSocket serverSocket = null;
    private int port;
    private ExecutorService commandExecutors;
    private int maxRunning = MAX_RUNNING_COMMANDS;
    private int curRunning = 0;
    private boolean serverStopped = false;

    public NetworkServer(ServerCallbacks callbacks) {
        this(callbacks, DEFAULT_SERVER_PORT);
    }

    public NetworkServer(ServerCallbacks callbacks, int port) {
        this.callbacks = callbacks;
        this.port = port;
        this.commandExecutors = Executors.newCachedThreadPool();
    }

    @Override
    public void startServer() throws IOException {
        System.out.println("Server started");
        serverSocket = new ServerSocket(port);
    }

    @Override
    public void run() {
        try {
            for (; ; ) {
                startCommand(serverSocket.accept());
            }
        } catch (IOException e) {
            System.err.println("Error reading from server socket: " + e.getMessage());
            e.printStackTrace();
        }
        if (serverStopped) {
            callbacks.serverDone(ServerExitState.STOPPED);
        } else {
            callbacks.serverDone(ServerExitState.FAILED);
        }
    }

    @Override
    public void stopServer() {
        System.out.println("Stopping server");
        serverStopped = true;
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public int getMaxRunning() {
        return maxRunning;
    }

    @Override
    public void setMaxRunning(int maxRunning) {
        this.maxRunning = maxRunning;
    }

    @Override
    public int getCurRunning() {
        return curRunning;
    }

    private synchronized void startCommand(Socket clientSocket) throws IOException {
        DistBuildProtos.CommandRequest clientCommand = DistBuildProtos.CommandRequest.parseFrom(clientSocket.getInputStream());
        NetworkCommand command = new NetworkCommand(clientSocket, clientCommand);

        curRunning++;
        if (curRunning > maxRunning) {
            commandDone(command, DistBuildProtos.CommandResponse.Status.BUSY);
            command.sendResponse();
        } else {
            LocalCommandDispatcher dispatcher = new LocalCommandDispatcher(command, this);
            command.setState(CommandState.QUEUED);
            commandExecutors.submit(dispatcher);
        }
    }

    private synchronized void commandDone(Command command, DistBuildProtos.CommandResponse.Status status) {
        commandDone(command, status, null);
    }

    private synchronized void commandDone(Command command, DistBuildProtos.CommandResponse.Status status, String message) {
        curRunning--;
        try {
            command.setStatus(status);
            if (message != null) {
                command.setMessage(message);
            }
            command.sendResponse();
        } catch (IOException e) {
            System.err.println("Failed to send response for command");
            e.printStackTrace();
        }
    }

    @Override
    public void commandSuccess(Command command) {
        commandDone(command, DistBuildProtos.CommandResponse.Status.SUCCESS);
    }

    @Override
    public void commandFailed(Command command) {
        commandDone(command, DistBuildProtos.CommandResponse.Status.FATAL);
    }

    @Override
    public void commandFailed(Command command, String message) {
        commandDone(command, DistBuildProtos.CommandResponse.Status.FATAL, message);
    }
}

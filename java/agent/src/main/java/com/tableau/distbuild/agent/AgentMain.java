package com.tableau.distbuild.agent;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AgentMain implements ServerCallbacks {
    private ExecutorService serverExecutors;
    private CommandServer server;
    private boolean serverRunning = false;

    private AgentMain() {
        serverExecutors = Executors.newSingleThreadExecutor();
        server = new NetworkServer(this);
    }

    private void runServer() throws IOException {
        System.out.println("Agent starting server");
        server.startServer();
        serverExecutors.submit(server);
        serverRunning = true;
    }

    private boolean isServerRunning() {
        return serverRunning;
    }

    @Override
    public void serverDone(ServerExitState exitState) {
        System.out.println("Server stopped running");
        serverRunning = false;
        serverExecutors.shutdownNow();
    }

    public static void main(String[] args) {
        System.out.println("Agent starting");

        try {
            AgentMain agent = new AgentMain();
            agent.runServer();
            while (agent.isServerRunning()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("Interrupted while sleeping");
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to start server");
        }

    }
}

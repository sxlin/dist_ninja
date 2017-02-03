package com.tableau.distbuild.agent;

import java.io.IOException;

public interface CommandServer extends Runnable {
    void startServer() throws IOException;
    void stopServer();

    int getMaxRunning();

    void setMaxRunning(int maxRunning);

    int getCurRunning();
}

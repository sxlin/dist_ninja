package com.tableau.distbuild.agent;

import com.tableau.distbuild.protobuf.DistBuildProtos;

import java.io.IOException;
import java.util.List;

public interface Command {
    String getCommand();

    List<String> getArgs();

    String getWorkingDirectory();

    DistBuildProtos.CommandResponse.Status getStatus();

    void setStatus(DistBuildProtos.CommandResponse.Status status);

    void setMessage(String message);

    CommandState getState();

    void setState(CommandState state);

    void sendResponse() throws IOException;
}

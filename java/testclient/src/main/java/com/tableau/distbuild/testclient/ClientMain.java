package com.tableau.distbuild.testclient;

import com.tableau.distbuild.protobuf.DistBuildProtos;

import java.net.InetAddress;
import java.net.Socket;

public class ClientMain {

    public static void main(String[] args) {
        DistBuildProtos.CommandRequest.Builder reqBuilder = DistBuildProtos.CommandRequest.newBuilder();
        boolean parseWorkingDir = false;

        reqBuilder.setId("TestCommand1");
        for (String arg : args) {
            if (arg.compareTo("--working-dir") == 0) {
                parseWorkingDir = true;
                continue;
            }
            if (parseWorkingDir) {
                reqBuilder.setWorkingDir(arg);
                break;
            } else if (reqBuilder.hasCommand()) {
                reqBuilder.addArgs(arg);
            } else {
                reqBuilder.setCommand(arg);
            }
        }

        System.out.println("Request created");
        try {
            Socket socket = new Socket(InetAddress.getLoopbackAddress(), 7821);
            reqBuilder.build().writeDelimitedTo(socket.getOutputStream());
            socket.getOutputStream().flush();
            System.out.println("Request sent");

            DistBuildProtos.CommandResponse response = DistBuildProtos.CommandResponse.parseDelimitedFrom(socket.getInputStream());
            System.out.println("Response received");
            System.out.println("Response ID: " + response.getId());
            System.out.println("Status code: " + response.getStatus());
            if (response.hasMessage()) {
                System.out.println("Message: " + response.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

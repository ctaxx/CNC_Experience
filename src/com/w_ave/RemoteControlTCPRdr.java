/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.w_ave;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author bykov_s_p
 */
public class RemoteControlTCPRdr implements Runnable {

    private final CNCKernel kernel;
    private Socket socket;

    private InputStreamReader streamReader;
    private BufferedReader reader;

    public RemoteControlTCPRdr(CNCKernel kernel, Socket clientSocket) {
        this.socket = clientSocket;
        this.kernel = kernel;
        try {
            streamReader = new InputStreamReader(clientSocket.getInputStream());
            reader = new BufferedReader(streamReader);

//            String msg = reader.readLine();
//            System.out.println(msg);
        } catch (IOException ex) {
            Logger.getLogger(RemoteControlTCPRdr.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        String msg;
        while (true) {
            try {
                if ((msg = reader.readLine()) != null) {
                    
                    System.out.println(msg);

                    JsonObject msgJson = new JsonParser().parse(msg).getAsJsonObject();
                    String messageContent = msgJson.get("clicked").getAsString();

                    if (messageContent.equals("JOG")) {
                        kernel.setIsJog(true);
                    }

                    if (messageContent.equals("AUTO")) {
                        kernel.setIsJog(false);
                    }

                    if (messageContent.equals("STEP")) {
                        kernel.setIsStepped();
                    }

                    if (messageContent.equals("START")) {
                        kernel.setProgExecuting(true);
                    }

                    if (messageContent.equals("STOP")) {
                        kernel.setProgExecuting(false);
                    }
                    kernel.setHaveToRefreshButtons(true);
                }
            } catch (IOException ex) {
                Logger.getLogger(RemoteControlTCPRdr.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}

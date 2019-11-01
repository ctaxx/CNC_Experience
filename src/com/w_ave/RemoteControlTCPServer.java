/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.w_ave;

import com.google.gson.*;
import java.io.*;
import java.net.*;

/**
 *
 * @author bykov_s_p
 */
public class RemoteControlTCPServer extends Thread{
    ServerSocket srvSocket;
    Socket clientSocket;

    ObjectOutputStream oos;
    OutputStream out;
    InputStream in;
    ObjectInputStream ois;

    CNCKernel kernel;
//    int rsu;

//    boolean flag = true;

    public RemoteControlTCPServer(CNCKernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public void run() {
        while (true) {

            try {
                srvSocket = new ServerSocket(65500);
                System.out.println("run");
                clientSocket = srvSocket.accept();
                System.out.println("accepted");

                InputStreamReader streamReader = new InputStreamReader(clientSocket.getInputStream());
                BufferedReader reader = new BufferedReader(streamReader);
                PrintWriter output = new PrintWriter(clientSocket.getOutputStream());
                String msg = reader.readLine();
                System.out.println(msg);
                
                JsonObject msgJson = new JsonParser().parse(msg).getAsJsonObject();
                String messageContent = msgJson.get("clicked").getAsString();
                
                if (messageContent.equals("JOG")){
                    kernel.setIsJog(true);
                }
                
                if (messageContent.equals("AUTO")){
                    kernel.setIsJog(false);
                } 
                
                if (messageContent.equals("STEP")){
                    kernel.setIsStepped();
                }
                
                if (messageContent.equals("START")){
                    kernel.setProgExecuting(true);
                }
                
                if (messageContent.equals("STOP")){
                    kernel.setProgExecuting(false);
                }
                
                output.print(kernel.getKernelState().toString());
                System.out.println("->send message");
                
                output.close();
                reader.close();
                streamReader.close();
                clientSocket.close();
                srvSocket.close();

            }// try
            catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            } 
        }
    }// run
}

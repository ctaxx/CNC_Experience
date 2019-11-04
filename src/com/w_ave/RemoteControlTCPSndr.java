/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.w_ave;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author bykov_s_p
 */
public class RemoteControlTCPSndr implements Runnable {

    private final CNCKernel kernel;

    private PrintWriter output;
    
    private Socket socket;// tmp

    public RemoteControlTCPSndr(CNCKernel kernel, Socket socket) {
        this.kernel = kernel;
        this.socket = socket;
        try {
            output = new PrintWriter(socket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(RemoteControlTCPSndr.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        System.out.println("sender run");
        while (true) {
            if (kernel.haveToRefreshButtons) {
                kernel.setHaveToRefreshButtons(false);
                output.println(kernel.getKernelState().toString());
                System.out.println("->send message");
                output.flush();
            }
        }
    }

}

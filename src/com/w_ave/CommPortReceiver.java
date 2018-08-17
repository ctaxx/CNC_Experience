/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.w_ave;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author bykov_s_p
 */
public class CommPortReceiver extends Thread {

    InputStream in;
    Protocol protocol = new ProtocolImpl();

    public CommPortReceiver(InputStream in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            int b;
            while (true) {

                // if stream is not bound in.read() method returns -1 
                while ((b = in.read()) != -1) {
                    protocol.onReceive((byte) b);
                }
                protocol.onStreamClosed();

                // wait 10ms when stream is broken and check again  
                sleep(10);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

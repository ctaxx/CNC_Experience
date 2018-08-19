/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.w_ave;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author bykov_s_p
 */
public class ComPortSender {
    static OutputStream out;  
      
    public static void setWriterStream(OutputStream out) {  
        ComPortSender.out = out;  
    }  
      
    public static void send(byte[] bytes) {  
        try {  
            System.out.println("SENDING: " + new String(bytes, 0, bytes.length));  
              
            // sending through serial port is simply writing into OutputStream  
            out.write(bytes);  
            out.flush();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
    }  
}

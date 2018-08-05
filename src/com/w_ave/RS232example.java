/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.w_ave;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

/**
 *
 * @author bykov_s_p
 */
public class RS232example {
    
    public static void main(String [] args)throws Exception{
         // connects to the port which name (e.g. COM1) is in the first argument  
        new RS232example().connect("COM3");  
          
        // send HELO message through serial port using protocol implementation  
        CommPortSender.send(new ProtocolImpl().getMessage("HELO"));  
    }
    
    public void connect(String portName) throws Exception {  
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);  
   
        if (portIdentifier.isCurrentlyOwned()) {  
            System.out.println("Port in use!");  
        } else {  
            // points who owns the port and connection timeout  
            SerialPort serialPort = (SerialPort) portIdentifier.open("RS232example", 2000);  
              
            // setup connection parameters  
            serialPort.setSerialPortParams(  
                9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);  
   
            // setup serial port writer  
            CommPortSender.setWriterStream(serialPort.getOutputStream());  
              
            // setup serial port reader  
//            new CommPortReceiver(serialPort.getInputStream()).start();  
        }  
    } 
}

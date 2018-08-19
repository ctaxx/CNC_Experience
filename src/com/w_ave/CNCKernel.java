/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.w_ave;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author bykov_s_p
 */
public class CNCKernel {

    static CNCFrame cncFrame;

    static int listPointer = 0;
    Character[] symbols = {'N', 'G', 'M', 'X', 'Y', 'Z'};
    static String[] program = {"x-1.e", "x3.e", "x-10.e"};
    static double currentX, prevX, ostX;

    public static void main(String[] args) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(CNCFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CNCFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CNCFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CNCFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                cncFrame = new CNCFrame();
            }
        });

        try {
            connect("COM3");
        } catch (Exception ex) {
            Logger.getLogger(CNCKernel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void connect(String portName) throws Exception {
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
            ComPortSender.setWriterStream(serialPort.getOutputStream());

            // setup serial port reader  
            new ComPortReceiver(serialPort.getInputStream()).start();
        }
    }

    // looking for one axis value from the frame
    private static double parseAxis(char ch, String frame) {
        double axisValue = 0;
        int point = frame.indexOf(ch);
        if (point != -1) {
            for (int i = point + 1; i < frame.length(); i++) {
                if (Character.isLetter(frame.charAt(i))) {
                    axisValue = Double.parseDouble(frame.substring(point + 1, i - 1));
                    break;
                }
            }
        }
        return axisValue;
    }

    // looking for all the delta axis from the frame
    private static double parseFrame(String frame) {
        double deltaX;
        prevX = currentX;
        currentX = parseAxis('x', frame);
        deltaX = currentX - prevX;
        return deltaX;
    }

    private static class ComPortReceiver extends Thread {

        InputStream in;

        StringBuffer stringBuffer = new StringBuffer();

        public ComPortReceiver(InputStream in) {
            this.in = in;
        }

        public void onReceive(byte b) {
            // simple protocol: each message ends with new line  
            if (b == '\n') {
                onMessage();
            } else {
                stringBuffer.append((char) b);
            }
        }

        public void onStreamClosed() {
            onMessage();
        }

        /* 
         * When message is recognized onMessage is invoked  
         */
        private void onMessage() {
            if (stringBuffer.length() != 0) {
                // constructing message  
                System.out.println("RECEIVED MESSAGEs: " + stringBuffer);

                // this logic should be placed in some kind of   
                // message interpreter class not here  
                if (stringBuffer.charAt(0) == '?') {
                    if (listPointer < program.length) {

                        String s = "x";
                        double d = parseFrame(program[listPointer]);
                        if (d >= 0) {
                            s += "<";
                        } else {
                            s += ">";
                        }
                        
                        d = Math.abs(d);
                        int val = (int)d * 96;
//                        ostX = d - val;
                        s += val;

                        s += "@";
                        System.out.println("listPointer is " + listPointer);
                        ComPortSender.send(getMessage(s));
                        listPointer++;
                    }
//                    ComPortSender.send(getMessage("x<100@"));
                }
                stringBuffer = new StringBuffer();
            }
        }

        // helper methods   
        public byte[] getMessage(String message) {
//        return (message+"\n").getBytes();
            return (message).getBytes();
        }

        @Override
        public void run() {
            try {
                int b;
                while (true) {

                    // if stream is not bound in.read() method returns -1 
                    while ((b = in.read()) != -1) {
                        onReceive((byte) b);
                    }
                    onStreamClosed();

                    // wait 10ms when stream is broken and check again  
                    sleep(10);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}

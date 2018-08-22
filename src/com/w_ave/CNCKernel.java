/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.w_ave;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author bykov_s_p
 */
public class CNCKernel {

    private static String INPUT_PATH = "d:/CNC_root/Programs/prog.mpf";

    CNCFrame cncFrame;
    
    ArrayList programsList;

    int listPointer = 0;
    Character[] symbols = {'N', 'G', 'M', 'X', 'Y', 'Z'};
//    String[] program = {"x-1.", "x3.", "x-10."};
    ArrayList<String> prog = new ArrayList();
    double currentX, prevX, ostX;

    public CNCKernel() {
        programsList = filing(new File("d:/CNC_root/Programs"));
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
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
                cncFrame = new CNCFrame();
                cncFrame.setProgramToList(prog);
                cncFrame.setProgramsToList(programsList);
            }
        });
        
        this.prog = prepareProgram();
    }

    public static void main(String[] args) {

        CNCKernel kernel = new CNCKernel();
        
        try {
            kernel.connect("COM3");
        } catch (Exception ex) {
            Logger.getLogger(CNCKernel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void connect(String portName) throws Exception {
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

    private ArrayList<String> prepareProgram() {
        File inputFile = new File(INPUT_PATH);
        ArrayList<String> array = new ArrayList<String>();
        StringBuilder stringBuilder = new StringBuilder();

        try {
            Reader rin = new FileReader(inputFile.getAbsoluteFile());
            int c;
            while ((c = rin.read()) != -1) {
                if (c == '\n') {
                    array.add(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                }else{
                    stringBuilder.append((char) c);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String s : prog) {
            System.out.println(s);
        }
        return array;
    }

    // looking for one axis value from the frame
    private double parseAxis(char ch, String frame) {
        double axisValue = 0;
        int point = frame.indexOf(ch);
        if (point != -1) {
            for (int i = point + 1; i < frame.length(); i++) {
                if (Character.isLetter(frame.charAt(i))) {
                    axisValue = Double.parseDouble(frame.substring(point + 1, i - 1));
                    return axisValue;
                }
            }
            axisValue = Double.parseDouble(frame.substring(point + 1, frame.length() - 1));
        }
        return axisValue;
    }

    // looking for all the delta axis from the frame
    private String parseFrame(String frame) {
        double deltaX;
        prevX = currentX;
        currentX = parseAxis('x', frame);
        deltaX = currentX - prevX;
        return prepareAxisTask('x', deltaX);
    }

    private String prepareAxisTask(char ch, double value) {
        StringBuilder s = new StringBuilder();
        s.append(ch);

        if (value >= 0) {
            s.append("<");
        } else {
            s.append(">");
        }

        value = Math.abs(value);
        int val = (int) value * 96;
//        ostX = d - val;
        s.append(val);
        s.append("@");

        return s.toString();
    }

    private String prepareAuxiliaryFuncTask(char ch, int value) {
        return null;
    }

    private class ComPortReceiver extends Thread {

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
                    if (listPointer < prog.size()) {
                        ComPortSender.send(getMessage(parseFrame(prog.get(listPointer))));
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
    
    public ArrayList<String> filing(File f){
        ArrayList<String> filesList = new ArrayList();
        File [] files = f.listFiles();
        for(File file: files){
            filesList.add(file.getName());
        }        
        return filesList;
       

    }

}

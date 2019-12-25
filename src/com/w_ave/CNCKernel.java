/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.w_ave;

import com.google.gson.*;
import com.w_ave.utils.Utils;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author bykov_s_p
 */
public class CNCKernel {
// deprecated

    private static String INPUT_FILE = "prog.mpf";
    private static String PROGRAM_ROOT = "d:/CNC_root/Programs/";
    private static String SETTINGS_ROOT = "d:/CNC_root/";

    private static final String M_ADDRESS = "M";
    private static final String F_ADDRESS = "F";
    private static final double TOLERANCE = 100.0;

    CNCFrame cncFrame;

    ArrayList programsList;

    int listPointer = 0;
//    Character[] symbols = {'N', 'G', 'M', 'X', 'Y', 'Z'};
    ArrayList<String> prog;
    double currentX, ostX;
    double currentY, ostY;

    boolean isJog = true;       // jog = true,  auto = false;

    public void setIsJog(boolean isJog) {
        this.isJog = isJog;
    }
 
    boolean requiredNextFrame = false;

    public synchronized void setRequiredNextFrame(boolean requiredNextFrame) {
        this.requiredNextFrame = requiredNextFrame;
    }
    boolean progExecuting = false;

    public void setProgExecuting(boolean progExecuting) {
        if (!isJog) {
            this.progExecuting = progExecuting;
        }
    }
        
    public boolean haveToStop = false;
   
    short step = 0;             // 10, 100, 1000
    boolean isStepped = false;

    public void setIsStepped() {
        this.isStepped = !this.isStepped;
    }
    char axis = '0';

    boolean haveToRefreshButtons = false;

    public synchronized void setHaveToRefreshButtons(boolean haveToRefreshButtons) {
        this.haveToRefreshButtons = haveToRefreshButtons;
        System.out.println("haveTo have been set to " + haveToRefreshButtons);
    }

    public CNCKernel() {
        programsList = fillListOfFiles(new File(PROGRAM_ROOT));
        this.prog = Utils.prepareProgram(PROGRAM_ROOT + getSelectedFileFromSettings());
        CNCKernel kernel = this;
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
                cncFrame.addObserverForPrograms(kernel);
            }
        });
    }

    public static void main(String[] args) {

        CNCKernel kernel = new CNCKernel();
        try {
            kernel.connect("COM3");
            
        } catch (Exception ex) {
            Logger.getLogger(CNCKernel.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        kernel.RemoteControlTCPServerConnect();
    }

    public JsonObject getKernelState() {
        JsonObject json = new JsonObject();
        json.addProperty("START", Boolean.toString(progExecuting));
        json.addProperty("STOP", Boolean.toString(!progExecuting));
        json.addProperty("RESET", "false");
        json.addProperty("JOG", Boolean.toString(isJog));
        json.addProperty("AUTO", Boolean.toString(!isJog));
        json.addProperty("10", "false");
        json.addProperty("100", "false");
        json.addProperty("1000", "false");
        json.addProperty("STEP", Boolean.toString(isStepped));
        json.addProperty("X", "false");
        json.addProperty("Y", "false");
        json.addProperty("+", "false");
        json.addProperty("-", "false");
        return json;
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
            // sepup serial port writer
            new Thread(new Runnable() {

                // helper methods   
                public byte[] getMessage(String message) {
                    return (message).getBytes();
                }

                @Override
                public void run() {
                    while (true) {
                        if (haveToStop){
                            ComPortSender.send("#".getBytes());
                            haveToStop = false;
                        }
                        if (requiredNextFrame) {
                            if (isJog) {
                            } else {
                                if (listPointer < prog.size() & progExecuting) {
                                    setRequiredNextFrame(false);
                                    setProgExecuting(!isStepped);
                                    ComPortSender.send(getMessage(parseFrame(prog.get(listPointer))));
                                    listPointer++;
                                }
                            }
                        }
                    }
                }
            }).start();
        }
    }

    public void RemoteControlTCPServerConnect() {
        ServerSocket srvSocket;
        Socket clientSocket;

        try {
            srvSocket = new ServerSocket(65500);
            System.out.println("server running");

            while (true) {
                clientSocket = srvSocket.accept();
                System.out.println("accepted");
                
                setHaveToRefreshButtons(true);

                new Thread(new RemoteControlTCPRdr(this, clientSocket)).start();

                new Thread(new RemoteControlTCPSndr(this, clientSocket)).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    // looking for all the delta axis from the frame
    private String parseFrame(String frame) {
        StringBuilder result = new StringBuilder();

        if (frame.contains(M_ADDRESS)) {
            result.append("m");
            int mIndex = frame.indexOf(M_ADDRESS);
            result.append(frame.substring(mIndex + 1, mIndex + 3));
        } else {
            double deltaX;
            double prevX;
            result.append('x');
            if (frame.indexOf('X') != -1) {
                prevX = currentX;
                currentX = Utils.parseAddressValue('X', frame);
                deltaX = currentX - prevX;
                deltaX = Math.round(deltaX * TOLERANCE) / TOLERANCE;
                result.append(deltaX);
            } else {
                result.append("0.0");
            }

            double deltaY;
            double prevY;
            result.append(" y");
            if (frame.indexOf('Y') != -1) {
                prevY = currentY;
                currentY = Utils.parseAddressValue('Y', frame);
                deltaY = currentY - prevY;
                deltaY = Math.round(deltaY * TOLERANCE) / TOLERANCE;
//            result.append(prepareAxisTask('z', deltaZ, 100*5/3));
                result.append(deltaY);
            } else {
                result.append("0.0");
            }
            result.append(" f");
            result.append(frame.substring(frame.indexOf(F_ADDRESS) + 1, frame.length() - 1));
        }
        result.append("@");
        return result.toString();
    }

    public void onProgramChoosed(String programName) {
        System.out.println("program was changed");
        System.out.println(programName);
        this.prog = Utils.prepareProgram(PROGRAM_ROOT + programName);
        cncFrame.setProgramToList(prog);
        setSelectedFileToSettings(programName);
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
                    setRequiredNextFrame(true);
//                    if (listPointer < prog.size()) {
//                        ComPortSender.send(getMessage(parseFrame(prog.get(listPointer))));
//                        listPointer++;
//                    }
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

    public ArrayList<String> fillListOfFiles(File f) {
        ArrayList<String> filesList = new ArrayList();
        File[] files = f.listFiles();
        for (File file : files) {
            filesList.add(file.getName());
        }
        return filesList;
    }

    private void setSelectedFileToSettings(String fileName) {
        JSONObject resultJSONObject = new JSONObject();
        resultJSONObject.put("selectedProgram", fileName);

        JSONStreamAware jSONStreamAware = resultJSONObject;

        try {
            Writer sout = new FileWriter(SETTINGS_ROOT + "selectedProgram.txt");

            jSONStreamAware.writeJSONString(sout);
            sout.flush();
            sout.close();

        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private String getSelectedFileFromSettings() {
        String fileName = new String();
        try {
            JSONParser parser = new JSONParser();
            JSONObject setting = (JSONObject) parser.parse(new FileReader(SETTINGS_ROOT + "selectedProgram.txt"));
            fileName = (String) setting.get("selectedProgram");
        } catch (IOException ex) {
            Logger.getLogger(CNCKernel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(CNCKernel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return fileName;
    }
}

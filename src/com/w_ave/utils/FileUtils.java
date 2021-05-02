/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.w_ave.utils;

import com.w_ave.CNCKernel;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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
public class FileUtils {

    private static final String SETTINGS_ROOT = "d:/CNC_root/";

    public static String getSelectedFileNameFromSettings() {
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

    public static void setSelectedFileNameToSettings(String fileName) {
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
    
    public static ArrayList<String> fillListOfFiles(File f) {
        ArrayList<String> filesList = new ArrayList();
        File[] files = f.listFiles();
        for (File file : files) {
            filesList.add(file.getName());
        }
        return filesList;
    }
}

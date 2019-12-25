/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.w_ave.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

/**
 *
 * @author bykov_s_p
 */
public class Utils {
    
     public static ArrayList<String> prepareProgram(String nameOfSelectedFile) {
        File inputFile = new File(nameOfSelectedFile);
        ArrayList<String> array = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();

        try {
            Reader rin = new FileReader(inputFile.getAbsoluteFile());
            int c;
            while ((c = rin.read()) != -1) {
                if (c == '\n') {
                    array.add(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                } else {
                    stringBuilder.append((char) c);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return array;
    }
     
         // looking for one axis value from the frame
    public static double parseAddressValue(char ch, String frame) {
        double axisValue;

        int point = frame.indexOf(ch);
        for (int i = point + 1; i < frame.length(); i++) {
            if (Character.isLetter(frame.charAt(i))) {
                axisValue = Double.parseDouble(frame.substring(point + 1, i - 1));
                return axisValue;
            }
        }
        axisValue = Double.parseDouble(frame.substring(point + 1, frame.length() - 1));
        return axisValue;
    }
}

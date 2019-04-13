/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mvjf.OpenMturk;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JTextField;

/**
 *
 * @author matt
 */
public class Util {
    public static double calculatePrice(double reward, int assignments, boolean masters, boolean hyperbatch) {
        double fee = 1.2;
        if (assignments > 9 && !hyperbatch) {
            fee += 0.2;
        }
        if (masters) {
            fee += 0.05;
        }
        if (reward == 0) {
            reward = 1;
        }
        double price = fee * reward * assignments;
        double roundedPrice = Math.round(price * 100.0)/100.0;
        return roundedPrice;
    }
    
    public static String[] getListData(JList list) {
        String[] jlistArray = new String[list.getModel().getSize()];
        for (int i = 0; i < list.getModel().getSize(); i++) {
            jlistArray[i] = String.valueOf(list.getModel().getElementAt(i));
        }
        return jlistArray;
    }
    
    public static void generateCSV(File csv, HashMap<String,String[]> data) throws IOException {
        FileWriter fw = new FileWriter(csv);
        BufferedWriter bw = new BufferedWriter(fw);
        String heading = arrayToCSString((String[])data.keySet().toArray(new String[data.size()]));
        bw.write(heading);
        bw.newLine();
        String[][] valueArrays = (String[][])data.values().toArray(new String[data.size()][]);
        for (int i = 0; i < valueArrays[0].length; i++) {
            String line = "";
            for (int j = 0; j < valueArrays.length; j++) {
                line += valueArrays[j][i] + ",";
            }
            bw.write(line.substring(0, line.length() - 1));
            bw.newLine();
        }
        bw.close();
        fw.close();
    }
    
    public static List<String[]> loadCSV(String csvPath) throws IOException {
        Reader reader = Files.newBufferedReader(Paths.get(csvPath));
        CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build();
        List<String[]> data = csvReader.readAll();
        return data;
    }
    
    public static void clearJlist(JList list) {
        list.setModel(new DefaultListModel());
        DefaultListModel lm = (DefaultListModel)list.getModel();
        lm.removeAllElements();
    }
    
    public static String arrayToCSString(String[] array) {
        StringBuilder builder = new StringBuilder();
        for (String string : array) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(string);
        }
        String string = builder.toString();
        return string;
    }
    
    public static String[] getLocales() {
        String[] locales = java.util.Locale.getISOCountries();
        List<String> countries = new ArrayList<>();
        for (int i = 0; i < locales.length; i++) {
            java.util.Locale locale = new java.util.Locale("", locales[i]);
            String name = locale.getDisplayCountry();
            countries.add(name + " - " + locales[i]);
        }
        Collections.sort(countries);
        String[] countryArray = new String[countries.size()];
        countryArray = countries.toArray(countryArray);
        return countryArray;
    }
    
    public static int validatePositiveIntegerWithComparator(JTextField field) {
        int value = Integer.parseInt(field.getText().trim());
        if (value < 0 || value > 100) {
            throw new NumberFormatException();
        }
        return value;
    }
}

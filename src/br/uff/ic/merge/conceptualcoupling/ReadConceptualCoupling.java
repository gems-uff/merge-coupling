/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.uff.ic.merge.conceptualcoupling;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author Cristiane
 */
public class ReadConceptualCoupling {

public static void readFiles(String filePath) throws IOException{
        
        String SHAMerge;

        FileFilter filter = new FileFilter() {
            public boolean accept(File file) {
                return file.getName().endsWith(".txt");
            }
        }; 
        filePath = filePath + "\\Output\\";
        String filePathName = filePath + "MergeConceptualCoupling" + ".txt";
        FileWriter arquivo = null;
        arquivo = new FileWriter(new File(filePathName));

        File directory = new File(filePath);
        File files[] = directory.listFiles(filter);
        for (File file : files) {
            FileInputStream stream = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(stream);
            BufferedReader br = new BufferedReader(reader);
            Double threshold;
            Double intensity, sum = 0.0;
            int count = 0;
            String line = br.readLine();
            while (line != null) {
                String classA = line.substring(0, line.indexOf(','));
                String classB = line.substring(line.indexOf(',') + 1, line.lastIndexOf(','));
                String similarity = line.substring(line.lastIndexOf(',') + 1, line.length());
                line = br.readLine();

                SHAMerge = file.getName().substring(file.getName().lastIndexOf(' ') + 1, file.getName().length()-4);
                
                threshold = Double.parseDouble(similarity);

                if ((classA.startsWith("Left") && classB.startsWith("Right")))  { //&& threshold >= 0.2)
                    arquivo.write(SHAMerge + "," + classA + "," + classB + "," + threshold + "\n");
                    sum = sum + threshold; 
                    count++;
                    System.out.println(classA + " and " + classB + " threshold " + threshold + " have conceptual coupling.");
                }
            }
            intensity = sum / count;
            arquivo.write("Intensity " + intensity + "\n");
        }
        arquivo.close();
    }
}
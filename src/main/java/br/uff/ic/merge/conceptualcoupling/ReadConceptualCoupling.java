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

    public static void readFiles(String filePath) throws IOException {

        String SHAMerge;

        FileFilter filter = new FileFilter() {
            public boolean accept(File file) {
                return file.getName().endsWith(".txt");
            }
        };
        filePath = filePath + File.separator + "Output" + File.separator;
        String filePathName = filePath + "MergeConceptualCoupling";
        FileWriter arquivo = null;

        File directory = new File(filePath);
        File files[] = directory.listFiles(filter);
        for (File file : files) {
            SHAMerge = file.getName().substring(file.getName().lastIndexOf("Coupling") + 8, file.getName().length() - 4);
            arquivo = new FileWriter(new File(filePathName + SHAMerge + ".txt"));

            FileInputStream stream = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(stream);
            BufferedReader br = new BufferedReader(reader);
            Double threshold;
            String line = br.readLine();
            while (line != null) {
                String methodA = line.substring(0, line.indexOf(','));
                String methodB = line.substring(line.indexOf(',') + 1, line.lastIndexOf(','));
                String similarity = line.substring(line.lastIndexOf(',') + 1, line.length());
                line = br.readLine();

                threshold = Double.parseDouble(similarity);

                if ((methodA.startsWith("Left") && methodB.startsWith("Right"))) {
                    arquivo.write(SHAMerge + "," + methodA + "," + methodB + "," + threshold + "\n");
                }
            }
            arquivo.close();
        }
    }

    public static void createFinalResult(String filePath) throws IOException {

        FileFilter filter = new FileFilter() {
            public boolean accept(File file) {
                return file.getName().startsWith("Merge");
            }
        };

        filePath = filePath + File.separator + "Output" + File.separator;
        String filePathName = filePath + "FinalResultMergeConceptualCoupling.cvs";
        try (FileWriter arquivo = new FileWriter(new File(filePathName))) {
            File directory = new File(filePath);
            File files[] = directory.listFiles(filter);
            for (File file : files) {
                FileInputStream stream = new FileInputStream(file);
                InputStreamReader reader = new InputStreamReader(stream);
                BufferedReader br = new BufferedReader(reader);
                Double threshold;
                Double sum = 0.0;
                String SHAMerge = "";
                String similarity;
                int count = 0;

                String line = br.readLine();
                while (line != null) {
                    SHAMerge = line.substring(0, line.indexOf(','));
                    similarity = line.substring(line.lastIndexOf(',') + 1, line.length());
                    line = br.readLine();
                    threshold = Double.parseDouble(similarity);

                    if (threshold >= 0.0) {
                        count++;
                        sum = sum + threshold;
                    }
                }
                if (!(count == 0)) {
                    arquivo.write(SHAMerge + "," + count + "," + sum/count + "\n");
                }
            }
        }

    }
}

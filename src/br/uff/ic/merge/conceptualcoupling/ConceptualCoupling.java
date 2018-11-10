/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.uff.ic.merge.conceptualcoupling;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cristiane
 */
public class ConceptualCoupling {

    public static void main(String[] args) throws IOException, InterruptedException {
        List<String> projectsPath = new ArrayList<>();

        File directory = new File(System.getProperty("user.home") + File.separator + "gitProjects" + File.separator);
        File files[] = directory.listFiles();

        for (File projectDir : files) //list the projects
        {
            if (projectDir.isDirectory()) {
                String projectDirectory = projectDir.toString();
                projectsPath.add(projectDirectory);
            }
        }

        for (String projectPath : projectsPath) {
            String projectName = projectPath.substring(projectPath.lastIndexOf(File.separator) + 1, projectPath.length());

            String path = System.getProperty("user.home") + File.separator + "projects" + File.separator + projectName;
            new File(path).mkdir();
            new File(path + File.separator + "Input").mkdir();

            // create the Output, ClassLevelGranularity and CorpusPreProcessed folders to be used for Semantic Similarity Java
            new File(path + File.separator + "Output").mkdir();
            new File(path + File.separator + "Output" + File.separator + "ClassLevelGranularity").mkdir();
            new File(path + File.separator + "Output" + File.separator + "ClassLevelGranularity" + File.separator + "CorpusPreProcessed").mkdir();
            
            //System.out.println("Extracting Diff ...  " + projectPath);
            //generateFilesDiff(projectPath, projectName);

            try {
                System.out.println("Calculating Similarity ... " + projectPath);
                Process process = Runtime.getRuntime().exec("java -jar SemanticSimilarityJava.jar -p " + projectName);

                //Check if the process is finished
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while ((reader.readLine()) != null) {
                }
                process.waitFor();

            } catch (IOException ex) {
                Logger.getLogger(Git.class.getName()).log(Level.SEVERE, null, ex);
            }
           
            System.out.println("Processing files ...  " + projectPath);
            ReadConceptualCoupling.readFiles(path);
            ReadConceptualCoupling.createFinalResult(path);
        }
    }

    public static void generateFilesDiff(String projectPath, String projectName) throws IOException {
        List<String> mergeRevisions = Git.getMergeRevisions(projectPath);

        String project, SHAMerge, SHALeft, SHARight, SHAmergeBase, path;
        SHALeft = "";
        SHARight = "";
        SHAmergeBase = "";

        project = projectPath;

        for (String mergeRevision : mergeRevisions) {

            SHAMerge = mergeRevision;

            List<String> parents = Git.getParents(projectPath, mergeRevision);

            if (parents.size() == 2) {
                SHALeft = parents.get(0);
                SHARight = parents.get(1);
                SHAmergeBase = Git.getMergeBase(projectPath, SHALeft, SHARight);
            }
            //Check if is a fast-forward merge
            if ((!(SHAmergeBase.equals(SHALeft))) && (!(SHAmergeBase.equals(SHARight)))) {
                path = extractDiff(projectPath, SHAmergeBase, SHALeft, SHAMerge, "Left", projectName);
                path = extractDiff(projectPath, SHAmergeBase, SHARight, SHAMerge, "Right", projectName);
                removeFiles(path);
            }

        }
    }

    public static String extractDiff(String projectPath, String SHAmergeBase, String SHALeft, String mergeName, String branchName, String projectName) throws IOException {

        List<String> diff = Git.diff(projectPath, SHAmergeBase, SHALeft);

        FileWriter arquivo = null;

        String path = "";

        //remove the first list line
        if (!(diff.size() == 0)) {
            String firstLine = diff.get(0);
            diff.remove(diff.get(0));

            path = System.getProperty("user.home") + File.separator + "projects" + File.separator + projectName + File.separator + "Input";

            //create the first case merge file
            if (firstLine.startsWith("diff")) {
                path = path + File.separator + mergeName;
                new File(path).mkdir();

                arquivo = createFile(firstLine, projectName, path, branchName);
            }

            for (String lineDiff : diff) {

                if (((lineDiff.startsWith("+ ")) || (lineDiff.startsWith("- ")))) {
                    arquivo.write(lineDiff + "\n");
                } else if (lineDiff.startsWith("diff")) {
                    arquivo.close();

                    arquivo = createFile(lineDiff, projectName, path, branchName);
                }

            }
            arquivo.close();
        }
        return path;
    }

    public static void removeFiles(String path) {
        //remove the file that is not a javafile
        String name;
        File directory = new File(path);
        File files[] = directory.listFiles();
        for (File file : files) {
            name = file.getName();
            if (name.contains("REMOVE")) {
                file.delete();
            }
        }
        //Check if the directory is empty or if it has just one file and delete
        String nameAux;
        File filesAux[] = directory.listFiles();
        int directorySize = filesAux.length;
        if ((directorySize == 0) || (directorySize == 1)) {
            if (directorySize == 1) {
                for (File fileAux : filesAux) {
                    nameAux = fileAux.getName();
                    fileAux.delete();
                }
                directory.delete();
            } else {
                directory.delete();

            }

        }

    }

    public static FileWriter createFile(String lineDiff, String projectName, String path, String branchName) throws IOException {

        String fileName, filePath, packageName, className;
        fileName = branchName + lineDiff.substring(lineDiff.lastIndexOf("/") + 1, lineDiff.length());

        if (!fileName.endsWith("java")) {
            fileName = "REMOVE";
        }
        filePath = path + File.separator + fileName;
        packageName = "package " + projectName + "\n";
        className = "class " + fileName + "\n";
        FileWriter arquivo = new FileWriter(new File(filePath));
        arquivo.write(packageName);
        arquivo.write(className);

        return arquivo;
    }
}

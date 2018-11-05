/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.uff.ic.merge.conceptualcoupling;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author Cristiane
 */
public class ConceptualCoupling {

    public static void main(String[] args) throws IOException {
        List<String> projectsPath = new ArrayList<>();

        File directory = new File(System.getProperty("user.home") + "\\gitProjects\\");
        File files[] = directory.listFiles();

        for (File projectDir : files) //list the projects
        {
            if (projectDir.isDirectory()) {
                String projectDirectory = projectDir.toString();
                projectsPath.add(projectDirectory);
            }
        }

        for (String projectPath : projectsPath) {
            String projectName = projectPath.substring(projectPath.lastIndexOf("\\") + 1, projectPath.length());

            String path = System.getProperty("user.home") + "\\projects\\" + projectName;
            new File(path).mkdir();
            new File(path + "\\Input").mkdir();

            // create the Output, ClassLevelGranularity and CorpusPreProcessed folders to be used for Semantic Similarity Java
            new File(path + "\\Output").mkdir();
            new File(path + "\\Output\\ClassLevelGranularity").mkdir();
            new File(path + "\\Output\\ClassLevelGranularity\\CorpusPreProcessed").mkdir();

            System.out.println("Extracting Diff ...  " + projectPath);
            generateFilesDiff(projectPath, projectName);
            
            System.out.println("Calculating Similarity ... " + projectPath);
            Runtime.getRuntime().exec("java -jar SemanticSimilarityJava.jar -p " + projectName);
            
            System.out.println("Processing files ...  " + projectPath);
            ReadConceptualCoupling.readFiles(path);
        }

    }

    public static void generateFilesDiff(String projectPath, String projectName) throws IOException {
        List<String> mergeRevisions = Git.getMergeRevisions(projectPath);

        String project, SHAMerge, SHALeft, SHARight, SHAmergeBase;
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

            String mergeName = "Merge " + SHAMerge;
            extractDiff(projectPath, SHAmergeBase, SHALeft, mergeName, "Left", projectName);
            extractDiff(projectPath, SHAmergeBase, SHARight, mergeName, "Right", projectName);

        }
    }

    public static void extractDiff(String projectPath, String SHAmergeBase, String SHALeft, String mergeName, String branchName, String projectName) throws IOException {

        List<String> diff = Git.diff(projectPath, SHAmergeBase, SHALeft);

        FileWriter arquivo = null;

        //remove the first list line
        if (!(diff.size() == 0)) {
            String firstLine = diff.get(0);
            diff.remove(diff.get(0));

            String path = System.getProperty("user.home") + "\\projects\\" + projectName + "\\Input";

            //create the first case merge file
            if (firstLine.startsWith("diff")) {
                path = path + "\\" + mergeName;
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

        }
    }

    public static FileWriter createFile(String lineDiff, String projectName, String path, String branchName) throws IOException {

        String fileName, filePath, packageName, className;
        fileName = branchName + lineDiff.substring(lineDiff.lastIndexOf("/") + 1, lineDiff.length());

        if (!fileName.endsWith("java")) {
            fileName = "REMOVE";
        }
        filePath = path + "\\" + fileName;
        packageName = "package " + projectName + "\n";
        className = "class " + fileName + "\n";
        FileWriter arquivo = new FileWriter(new File(filePath));
        arquivo.write(packageName);
        arquivo.write(className);

        return arquivo;

    }
}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.uff.ic.merge.conceptualcoupling;

import br.uff.ic.coupling.ChunkInformation;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import br.uff.ic.mergeguider.MergeGuider;
import static br.uff.ic.coupling.CouplingChunks.extractAST;
import static br.uff.ic.coupling.CouplingChunks.generateASTFiles;
import static br.uff.ic.coupling.CouplingChunks.leftBaseCCMethodDeclarations;
import static br.uff.ic.coupling.CouplingChunks.leftCCMethodDeclarations;
import br.uff.ic.mergeguider.javaparser.ClassLanguageContructs;
import br.uff.ic.mergeguider.languageConstructs.MyMethodDeclaration;
import br.uff.ic.coupling.Git;

/**
 *
 * @author Cristiane
 */
public class ConceptualCoupling {

    public static void main(String[] args) throws IOException, InterruptedException {
        List<String> projectsPath = new ArrayList<>();

        File directory = new File(System.getProperty("user.home") + File.separator + "gitProjects" + File.separator);
        File files[] = directory.listFiles();

        String sandbox = "C:\\Cristiane\\mestrado\\sandbox";

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
            new File(path + File.separator + "Output" + File.separator + "MethodLevelGranularity").mkdir();
            new File(path + File.separator + "Output" + File.separator + "MethodLevelGranularity" + File.separator + "CorpusPreProcessed").mkdir();

            System.out.println("Extracting Diff ...  " + projectPath);
            generateFilesDiff(projectPath, projectName, sandbox);

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

    public static void generateFilesDiff(String projectPath, String projectName, String sandbox) throws IOException {
        List<String> mergeRevisions = Git.getMergeRevisions(projectPath);

        String project, SHAMerge, SHALeft, SHARight, SHAmergeBase, path;

        SHAMerge = null;
        SHALeft = null;
        SHARight = null;
        SHAmergeBase = null;

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
                boolean result = extractDiff(projectPath, SHAmergeBase, SHALeft, SHARight, SHAMerge, "Left", projectName, sandbox);
                if (!result) {
                    System.out.println(SHAMerge + "does not has java files in both branchs");
                }

            }

        }
    }

    public static boolean extractDiff(String projectPath, String SHAmergeBase, String SHALeft, String SHARight, String mergeName, String branchName, String projectName, String sandbox) throws IOException {
        //Getting modified files 
        List<String> changedFilesLeft = new ArrayList<String>();
        List<String> changedFilesRight = new ArrayList<String>();
        
        List<String> changedFilesLeftAux = Git.getChangedFiles(projectPath, SHALeft, SHAmergeBase);
        List<String> changedFilesRightAux = Git.getChangedFiles(projectPath, SHARight, SHAmergeBase);

        //to remove files that have extension other than java
        for (int i = 0; i < changedFilesLeftAux.size(); i++) {
            if (changedFilesLeftAux.get(i).endsWith("java")) {
                changedFilesLeft.add(changedFilesLeftAux.get(i));
            }
        }
        for (int i = 0; i < changedFilesRightAux.size(); i++) {
            if (changedFilesRightAux.get(i).endsWith("java")) {
                changedFilesRight.add(changedFilesRightAux.get(i));
            }
        }
        //If not exist java files, the variable changedFiles can be empty and we can't identify dependencies
        if ((changedFilesLeft.isEmpty()) || (changedFilesRight.isEmpty())) {
            return false;
        }

        //Extracting Left AST
        System.out.println("Cloning left repository...");
        String repositoryLeft = sandbox + File.separator + "left";

        MergeGuider.clone(projectPath, repositoryLeft);
        Git.reset(repositoryLeft);
        Git.clean(repositoryLeft);
        Git.checkout(repositoryLeft, SHALeft);

        System.out.println("Extracting left repository AST...");

        List<ClassLanguageContructs> ASTLeft = extractAST(repositoryLeft);

        //Extracting Right AST
        System.out.println("Cloning right repository...");

        String repositoryRight = sandbox + File.separator + "right";

        MergeGuider.clone(projectPath, repositoryRight);
        Git.reset(repositoryRight);
        Git.clean(repositoryRight);
        Git.checkout(repositoryRight, SHARight);

        System.out.println("Extracting right repository AST...");

        List<ClassLanguageContructs> ASTRight = extractAST(repositoryRight);
        
        //Getting modified files AST
        List<ClassLanguageContructs> ASTchangedFilesLeft = generateASTFiles(ASTLeft, changedFilesLeft);
        List<ClassLanguageContructs> ASTchangedFilesRight = generateASTFiles(ASTRight, changedFilesRight);
        
        //Getting chunks tem que armazenar as linhas add e removidas para cada arquivo
        List<ChunkInformation> cisL = ChunkInformation.extractChunksInformation(repositoryLeft, changedFilesLeft, SHAmergeBase, SHALeft, "Left");
        List<ChunkInformation> cisR = ChunkInformation.extractChunksInformation(repositoryRight, changedFilesRight, SHAmergeBase, SHARight, "Right");

        generateMethodFiles(cisL, repositoryLeft, ASTchangedFilesLeft, SHALeft, SHAmergeBase,
                sandbox, projectName, mergeName, "Left");
        generateMethodFiles(cisR, repositoryRight, ASTchangedFilesRight, SHARight, SHAmergeBase,
                sandbox, projectName, mergeName, "Right");

        return true;
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

    public static void generateMethodFiles(List<ChunkInformation> cis, String projectPath, List<ClassLanguageContructs> AST,
            String SHAParent, String SHAmergeBase, String sandboxAux, String projectName, String mergeName, 
            String branchName) throws IOException {

        FileWriter arquivo = null;
        String path = "";

        for (ChunkInformation ci : cis) {

            List<String> fileDiff = Git.fileDiff(projectPath, ci.getFilePath(), SHAmergeBase, SHAParent);

            //Find method declaration that has some intersection with a method declaration
            List<MyMethodDeclaration> MethodDeclarations = leftCCMethodDeclarations(projectPath, ci, AST);

            //del equals method   
            for (int i = MethodDeclarations.size() - 1; i > 0; i--) {
                if (MethodDeclarations.get(i).equals(MethodDeclarations.get(i - 1))) {
                    MethodDeclarations.remove(MethodDeclarations.get(i));
                }
            }

            for (MyMethodDeclaration leftMethodDeclaration : MethodDeclarations) {
                path = System.getProperty("user.home") + File.separator + "projects" + File.separator + projectName + File.separator + "Input";
                path = path + File.separator + mergeName;
                int begin = leftMethodDeclaration.getLocation().getElementLineBegin();
                int end = leftMethodDeclaration.getLocation().getElementLineEnd();
                String methodName = leftMethodDeclaration.getMethodDeclaration().getName().toString();
                List<String> result = new ArrayList<>();

                int initialline, line = 0;

                for (String lineDiff : fileDiff) {

                    if (lineDiff.startsWith("@@")) {
                        //read the interval line
                        initialline = finalLine(lineDiff); //initialLine
                        if (initialline == 0) {//it is a added file
                            initialline = 1;
                        }
                        line = initialline;

                    } else if (lineDiff.startsWith("+ ")) {
                        line++;

                        if ((line >= begin) && (line <= end)) {
                            result.add(lineDiff);
                        }

                    } else if (lineDiff.startsWith("- ")) {
                        if ((line >= begin) && (line <= end)) {
                            result.add(lineDiff);
                        }
                    } else {

                        line++;

                    }
                }
                arquivo = createFile(result, projectName, path, branchName, methodName);
            }

        }
    }

    public static FileWriter createFile(List<String> lines, String projectName, String path, String branchName, String methodName) throws IOException {

        String fileName, filePath, packageName, className;

        new File(path).mkdir();
        filePath = path + File.separator + branchName + methodName + ".java";

        packageName = "package " + projectName + "\n";
        className = "class " + branchName + methodName + "\n";
        FileWriter arquivo = new FileWriter(new File(filePath));
        arquivo.write(packageName);
        arquivo.write(className);

        for (String line : lines) {
            arquivo.write(line + "\n");
        }

        arquivo.close();

        return arquivo;
    }

    public static int finalLine(String line) {

        String[] intervals = line.split("\\+");

        String[] limits = intervals[1].split(",");

        return Integer.parseInt(limits[0].replace(" ", ""));
    }

}

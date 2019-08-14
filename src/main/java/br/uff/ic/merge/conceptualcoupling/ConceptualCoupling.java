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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jdt.core.dom.IMethodBinding;

/**
 *
 * @author Cristiane
 */
public class ConceptualCoupling {

    public static void main(String[] args) throws IOException, InterruptedException, ParseException {

        /*String input = "C:\\Users\\Carlos\\gitProjects";
        String output = "C:\\Users\\Carlos\\projects";*/
        List<String> projectsPath = new ArrayList<>();

        final Options options = new Options();

        String input = "";
        String output = "";
        //Double threshold = 0.0;

        try {
            options.addOption("i", true, "input directory");
            options.addOption("o", true, "output directory");
            options.addOption("t", true, "threshold from 0.0 to 1.0");

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("conceptual-coupling", options, true);

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("i")) {
                input = cmd.getOptionValue("i");
            }
            if (cmd.hasOption("o")) {
                output = cmd.getOptionValue("o");
            }
            /*if (cmd.hasOption("t")) {
                threshold = Double.parseDouble(cmd.getOptionValue("t"));
            }*/
        } catch (ParseException ex) {
            Logger.getLogger(ConceptualCoupling.class.getName()).log(Level.SEVERE, null, ex);

        }
        File directory = new File(input + File.separator);
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

            String path = output + File.separator + projectName;
            new File(path).mkdir();
            new File(path + File.separator + "Input").mkdir();

            // create the Output, ClassLevelGranularity and CorpusPreProcessed folders to be used for Semantic Similarity Java
            new File(path + File.separator + "Output").mkdir();
            new File(path + File.separator + "Output" + File.separator + "MethodLevelGranularity").mkdir();
            new File(path + File.separator + "Output" + File.separator + "MethodLevelGranularity" + File.separator + "CorpusPreProcessed").mkdir();

            String sandbox = path + File.separator + "Output" + File.separator + "sandbox";
            new File(sandbox);

            System.out.println("Extracting Diff ...  " + projectPath);
            generateFilesDiff(projectPath, projectName, output, sandbox);

            try {
                System.out.println("Calculating Similarity ... " + projectPath);
                Process process = Runtime.getRuntime().exec("java -jar " + File.separator + System.getProperty("user.home") + File.separator + "SemanticSimilarityJava.jar -p " + projectName + " -i " + input + " -o " + output);

                //Process process = Runtime.getRuntime().exec("java -jar " + System.getProperty("user.home") + File.separator + "SemanticSimilarityJava.jar -p " + projectName + " -i " + input + " -o " + output);
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

    public static void generateFilesDiff(String projectPath, String projectName, String output, String sandbox) throws IOException {
        List<String> mergeRevisions = Git.getMergeRevisions(projectPath);

        String project, SHAMerge, SHALeft, SHARight, SHAmergeBase, path;

        SHAMerge = null;
        SHALeft = null;
        SHARight = null;
        SHAmergeBase = null;

        project = projectPath;

        String pathFile = output + File.separator + projectName;

        String finalFilePath = pathFile + File.separator + "Output" + File.separator;
        String filePathName = finalFilePath + "EspecialMerge.txt";
        FileWriter arquivo = new FileWriter(new File(filePathName));

        for (String mergeRevision : mergeRevisions) {

            SHAMerge = mergeRevision;

            //SHAMerge = "1f67ba86730d086921cddd38dcc10616920fd7b6";
            List<String> parents = Git.getParents(projectPath, mergeRevision);
            //List<String> parents = Git.getParents(projectPath, SHAMerge);

            if (parents.size() == 2) {

                SHALeft = parents.get(0);
                SHARight = parents.get(1);
                SHAmergeBase = Git.getMergeBase(projectPath, SHALeft, SHARight);
            }
            //Check if is a fast-forward merge
            if (!(SHAmergeBase == null)) {
                if ((!(SHAmergeBase.equals(SHALeft))) && (!(SHAmergeBase.equals(SHARight)))) {
                    boolean result = extractDiff(projectPath, output, SHAmergeBase, SHALeft, SHARight, SHAMerge, "Left", projectName, sandbox);
                    if (!result) {
                        System.out.println(SHAMerge + "does not has java files in both branches");
                        arquivo.write(SHAMerge + "," + "0.0" + "\n");
                    }
                }
            }

        }
        arquivo.close();
    }

    public static boolean extractDiff(String projectPath, String output, String SHAmergeBase, String SHALeft, String SHARight, String mergeName, String branchName, String projectName, String sandbox) throws IOException {
        //Getting modified files 
        List<String> changedFilesLeft = new ArrayList<String>();
        List<String> changedFilesRight = new ArrayList<String>();

        List<String> changedFilesLeftAux = Git.getChangedFiles(projectPath, SHALeft, SHAmergeBase);
        List<String> changedFilesRightAux = Git.getChangedFiles(projectPath, SHARight, SHAmergeBase);

        if (!(changedFilesLeftAux == null)) {
            //to remove files that have extension other than java
            for (int i = 0; i < changedFilesLeftAux.size(); i++) {
                if (changedFilesLeftAux.get(i).endsWith("java")) {
                    changedFilesLeft.add(changedFilesLeftAux.get(i));
                }
            }
        }
        if (!(changedFilesRightAux == null)) {
            for (int i = 0; i < changedFilesRightAux.size(); i++) {
                if (changedFilesRightAux.get(i).endsWith("java")) {
                    changedFilesRight.add(changedFilesRightAux.get(i));
                }
            }
        }
        //If not exist java files, the variable changedFiles can be empty and we can't identify dependencies
        if ((changedFilesLeft.isEmpty()) && (changedFilesRight.isEmpty())) {
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

        //Extracting merge-base AST
        System.out.println("Cloning merge-base repository...");
        String repositoryBase = sandbox + File.separator + "base";

        MergeGuider.clone(projectPath, repositoryBase);
        Git.reset(repositoryBase);
        Git.clean(repositoryBase);
        Git.checkout(repositoryBase, SHAmergeBase);

        System.out.println("Extracting merge-base repository AST...");

        List<ClassLanguageContructs> ASTmergeBase = extractAST(repositoryBase);

        //Getting modified files AST
        List<ClassLanguageContructs> ASTchangedFilesLeft = generateASTFiles(ASTLeft, changedFilesLeft);
        List<ClassLanguageContructs> ASTchangedFilesRight = generateASTFiles(ASTRight, changedFilesRight);

        //Getting chunks tem que armazenar as linhas add e removidas para cada arquivo
        List<ChunkInformation> cisL = ChunkInformation.extractChunksInformation(repositoryLeft, changedFilesLeft, SHAmergeBase, SHALeft, "Left");
        List<ChunkInformation> cisR = ChunkInformation.extractChunksInformation(repositoryRight, changedFilesRight, SHAmergeBase, SHARight, "Right");

        generateMethodFiles(cisL, repositoryLeft, output, ASTchangedFilesLeft, ASTmergeBase, SHALeft, SHAmergeBase,
                sandbox, projectName, mergeName, "Left");
        generateMethodFiles(cisR, repositoryRight, output, ASTchangedFilesRight, ASTmergeBase, SHARight, SHAmergeBase,
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

    public static void generateMethodFiles(List<ChunkInformation> cis, String projectPath, String output, List<ClassLanguageContructs> AST,
            List<ClassLanguageContructs> ASTmergeBase, String SHAParent, String SHAmergeBase, String sandboxAux, String projectName, String mergeName,
            String branchName) throws IOException {

        FileWriter arquivo = null;
        String path = "";

        for (ChunkInformation ci : cis) {

            List<String> fileDiff = Git.fileDiff(projectPath, ci.getFilePath(), SHAmergeBase, SHAParent);

            //Find method declaration that has some intersection with a method declaration
            List<MyMethodDeclaration> MethodDeclarations = leftCCMethodDeclarations(projectPath, ci, AST);

            List<MyMethodDeclaration> MethodDeclarationsBase = leftBaseCCMethodDeclarations(projectPath, ci, ASTmergeBase);

            List<MyMethodDeclaration> MethodDeclarationsBaseAux = leftBaseCCMethodDeclarations(projectPath, ci, ASTmergeBase);

            //exclui os metodos inseridos repetidos
            if (MethodDeclarationsBaseAux.size() > 1) {
                //del equals method   
                for (int i = MethodDeclarationsBaseAux.size() - 1; i > 0; i--) {
                    IMethodBinding MethodDeclarationsBaseAux1 = MethodDeclarationsBaseAux.get(i).getMethodDeclaration().resolveBinding();
                    IMethodBinding MethodDeclarationsBaseAux2 = MethodDeclarationsBaseAux.get(i - 1).getMethodDeclaration().resolveBinding();

                    if (MethodDeclarationsBaseAux1 != null && MethodDeclarationsBaseAux2 != null && MethodDeclarationsBaseAux1.isEqualTo(MethodDeclarationsBaseAux2)) {
                        MethodDeclarationsBaseAux.remove(MethodDeclarationsBaseAux.get(i));
                    }
                }
            }

            //exclui os metodos modificados repetidos
            if (MethodDeclarations.size() > 1) {
                //del equals method   
                for (int i = MethodDeclarations.size() - 1; i > 0; i--) {
                    IMethodBinding methodDeclaration1 = MethodDeclarations.get(i).getMethodDeclaration().resolveBinding();
                    IMethodBinding methodDeclaration2 = MethodDeclarations.get(i - 1).getMethodDeclaration().resolveBinding();

                    if (methodDeclaration1 != null && methodDeclaration2 != null && methodDeclaration1.isEqualTo(methodDeclaration2)) {
                        MethodDeclarations.remove(MethodDeclarations.get(i));
                    }
                }
            }
            //identifica as linhas dos metodos que foram modificados e inseridos.
            for (MyMethodDeclaration leftMethodDeclaration : MethodDeclarations) {
                path = output + File.separator + projectName + File.separator + "Input";
                path = path + File.separator + mergeName;
                int begin = leftMethodDeclaration.getLocation().getElementLineBegin();
                int end = leftMethodDeclaration.getLocation().getElementLineEnd();
                String methodName = leftMethodDeclaration.getMethodDeclaration().getName().toString();
                /*String mName = leftMethodDeclaration.getMethodDeclaration().resolveBinding().toString();
                String methodName = mName.substring(0,mName.lastIndexOf("("));
                methodName = methodName.replaceAll("[^a-zZ-Z1-9 ]", "");
                methodName = methodName.replace(" ", "");*/

                String classFilePath = ci.getFilePath();
                String className = classFilePath.substring(classFilePath.lastIndexOf("/") + 1, classFilePath.length() - 5);
                List<String> result = new ArrayList<>();

                int initialline, line = 0;
                int count = 0;

                for (String lineDiff : fileDiff) {

                    if (lineDiff.startsWith("@@")) {
                        //read the interval line
                        initialline = finalLine(lineDiff); //initialLine
                        if (initialline == 0) {//it is a added file
                            initialline = 1;
                        }
                        line = initialline;

                    } else if (lineDiff.startsWith("+") && (!(lineDiff.startsWith("+++")))) {
                        line++;
                        if ((line >= begin) && (line <= end)) {
                            result.add(lineDiff);
                        }

                    } else if (lineDiff.startsWith("-") && (!(lineDiff.startsWith("---")))) {
                        count++;
                        /*if ((line >= begin) && (line <= end)) {
                            result.add(lineDiff);
                        }*/
                    } else {

                        line++;

                    }
                }
                if (!(result.isEmpty())) {
                    arquivo = createFile(result, projectName, path, branchName, methodName, className);
                }
            }

            //identifica as linhas  dos metodos que foram totalmente excluidos, base.
            for (MyMethodDeclaration leftMethodDeclarationBase : MethodDeclarationsBaseAux) {

                path = output + File.separator + projectName + File.separator + "Input";
                path = path + File.separator + mergeName;
                int begin = leftMethodDeclarationBase.getLocation().getElementLineBegin();
                int end = leftMethodDeclarationBase.getLocation().getElementLineEnd();
                String methodName = leftMethodDeclarationBase.getMethodDeclaration().getName().toString();
                /*String mName = leftMethodDeclarationBase.getMethodDeclaration().resolveBinding().toString();
                String methodName = mName.substring(0,mName.lastIndexOf("("));
                methodName = methodName.replaceAll("[^a-zZ-Z1-9 ]", "");
                methodName = methodName.replace(" ", "");*/

                String classFilePath = ci.getFilePath();
                String className = classFilePath.substring(classFilePath.lastIndexOf("/") + 1, classFilePath.length() - 5);
                List<String> result = new ArrayList<>();

                int initialline, line = 0;
                int count = 0;

                for (String lineDiff : fileDiff) {

                    if (lineDiff.startsWith("@@")) {
                        //read the interval line
                        initialline = initialLine(lineDiff); //initialLine
                        if (initialline == 0) {//it is a added file
                            initialline = 1;
                        }
                        line = initialline;

                    } else if (lineDiff.startsWith("+") && (!(lineDiff.startsWith("+++")))) {
                        count++;
                        /*if ((line >= begin) && (line <= end)) {
                            result.add(lineDiff);
                        }*/

                    } else if (lineDiff.startsWith("-") && (!(lineDiff.startsWith("---")))) {
                        line++;
                        if ((line >= begin) && (line <= end)) {
                            result.add(lineDiff);
                        }
                    } else {

                        line++;

                    }
                }
                if (!(result.isEmpty())) {
                    arquivo = createFileBase(result, projectName, path, branchName, methodName, className);
                }
            }

        }
    }

    public static FileWriter createFile(List<String> lines, String projectName, String path, String branchName, String methodName, String classNamePath) throws IOException {

        String fileName, filePath, packageName, className;

        new File(path).mkdir();
        filePath = path + File.separator + branchName + classNamePath + "$" + methodName + ".java";

        packageName = "package " + projectName + "\n";
        className = "class " + branchName + classNamePath + "$" + methodName + "\n";
        FileWriter arquivo = new FileWriter(new File(filePath));
        arquivo.write(packageName);
        arquivo.write(className);

        if (!(lines.isEmpty())) {
            for (String line : lines) {
                arquivo.write(line + "\n");
            }
        }
        arquivo.close();

        return arquivo;
    }

    public static FileWriter createFileBase(List<String> lines, String projectName, String path, String branchName, String methodName, String classNamePath) throws IOException {

        FileWriter arquivo = null;
        String fileName, filePath, packageName, className;

        filePath = path + File.separator + branchName + classNamePath + "$" + methodName + ".java";
        File file = new File(filePath); //abre o arquivo

        if (file.exists()) {
            //BufferedReader arquivo = new BufferedReader(new FileReader(nome));
            arquivo = new FileWriter(filePath, true); //new File
            if (!(lines.isEmpty())) {
                for (String line : lines) {
                    arquivo.append(line + "\n");
                }
            }
            arquivo.close();
        } else if (!(file.exists())) {
            arquivo = createFile(lines, projectName, path, branchName, methodName, classNamePath);

        }
        return arquivo;
    }

    public static int finalLine(String line) {

        String[] intervals = line.split("\\+");

        String[] limits = intervals[1].split(",");

        return Integer.parseInt(limits[0].replace(" ", ""));
    }

    private static int initialLine(String line) {

        line = line.replaceFirst("@@ -", "");
        String[] intervals = line.split("\\+");
        String[] limits = intervals[0].split(",");

        return Integer.parseInt(limits[0].replace(" ", ""));
    }

}

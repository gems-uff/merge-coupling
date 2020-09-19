/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.uff.ic.merge.conceptualcoupling;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gleiph
 */
public class Git {

    private String repository;

    public Git(String repository) {
        this.repository = repository;
    }

    /**
     *
     * @param repository
     * @param mergebaseSHA
     * @param commitSHA
     * @return
     */

   
    public static List<String> getMergeRevisions(String repositoryPath) {
        String command = "git log --all --merges --pretty=%H";
//        System.out.println(command);
        List<String> output = new ArrayList<String>();

        try {
            Process exec = Runtime.getRuntime().exec(command, null, new File(repositoryPath));
//            exec.waitFor();

            String s;

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(exec.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(exec.getErrorStream()));

            // read the output from the command
            while ((s = stdInput.readLine()) != null) {
                output.add(s);

            }

            // read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }

        } catch (IOException ex) {
            Logger.getLogger(Git.class.getName()).log(Level.SEVERE, null, ex);
        }

        return output;
    }

    public static List<String> getMergeRevisions(String repositoryPath, boolean  reverse) {
        String command = "git log --all --merges --pretty=%H";
        
        if(reverse)
            command = "git log --all --merges --reverse --pretty=%H";
//        System.out.println(command);
        List<String> output = new ArrayList<String>();

        try {
            Process exec = Runtime.getRuntime().exec(command, null, new File(repositoryPath));
//            exec.waitFor();

            String s;

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(exec.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(exec.getErrorStream()));

            // read the output from the command
            while ((s = stdInput.readLine()) != null) {
                output.add(s);

            }

            // read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }

        } catch (IOException ex) {
            Logger.getLogger(Git.class.getName()).log(Level.SEVERE, null, ex);
        }

        return output;
    }
    
   
    public static List<String> getParents(String repositoryPath, String revision) {
//        String command = "git rev-list --parents -n 1 " + revision;
        String command = "git log --pretty=%P -n 1 " + revision;

        List<String> output = new ArrayList<String>();

        try {
            Process exec = Runtime.getRuntime().exec(command, null, new File(repositoryPath));

            String s;

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(exec.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(exec.getErrorStream()));

            // read the output from the command
            while ((s = stdInput.readLine()) != null) {
                String[] split = s.split(" ");
                for (String rev : split) {
                    output.add(rev);
                }

            }

            // read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }

        } catch (IOException ex) {
            Logger.getLogger(Git.class.getName()).log(Level.SEVERE, null, ex);
        }

        return output;
    }

    public static String getMergeBase(String repositoryPath, String commit1, String commit2) {
//        String command = "git rev-list --parents -n 1 " + revision;
        String command = "git merge-base " + commit1 + " " + commit2;

        String output = null;

        try {
            Process exec = Runtime.getRuntime().exec(command, null, new File(repositoryPath));

            String s;

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(exec.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(exec.getErrorStream()));

            // read the output from the command
            while ((s = stdInput.readLine()) != null) {
                output = s;
            }

            // read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }

        } catch (IOException ex) {
            Logger.getLogger(Git.class.getName()).log(Level.SEVERE, null, ex);
        }

        return output;
    }
    
     public static List<String> diff(String repository, String sourceFile, String targetFile) {
        List<String> result = new ArrayList<String>();
        String command = "git diff " + sourceFile + " " + targetFile;

        CMDOutput cmdOutput = CMD.cmd(repository, command);
        if (cmdOutput.getErrors().isEmpty()) {
            return cmdOutput.getOutput();
        } else {
            return null;
        }
    }
     
     public static List<String> getChangedFiles(String repository, String SHAParent, String SHAMergeBase) {

        List<String> result = new ArrayList<>();
        //"diff-tree"
        String[] command = {"git", "diff", "--no-commit-id", "--name-only", "-r", SHAMergeBase, SHAParent};

        CMDOutput cmdOutput = CMD.cmdArray(repository, command);
        if (cmdOutput.getErrors().isEmpty()) {

            for (String output : cmdOutput.getOutput()) {
                result.add(output);
            }
            return result;
        } else {
            return null;
        }

    }
     
     public static List<String> fileDiff(String repository, String file, String sourceSHA, String targetSHA) {
        List<String> result = new ArrayList<String>();
        String command = "git diff " + sourceSHA + " " + targetSHA + " " + file;

        CMDOutput cmdOutput = CMD.cmd(repository, command);
        if (cmdOutput.getErrors().isEmpty()) {
            return cmdOutput.getOutput();
        } else {
            return null;
        }
    }

     
     
    /**
     * @return the repository
     */
    public String getRepository() {
        return repository;
    }

    /**
     * @param repository the repository to set
     */
    public void setRepository(String repository) {
        this.repository = repository;
    }

}
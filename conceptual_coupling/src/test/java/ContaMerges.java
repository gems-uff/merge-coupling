
import br.uff.ic.coupling.Git;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Carlos
 */
public class ContaMerges {

    public static void main(String args[]) throws FileNotFoundException, IOException {

        String SHALeft = "", SHARight = "", SHAmergeBase = "", SHAMerge = "";

        //String SHALeft, SHARight, SHAmergeBase, SHAMerge;
        FileInputStream stream = new FileInputStream("C:\\Cristiane\\mestrado\\Experimentos\\Merges_Projetos\\merges_che.txt");
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader br = new BufferedReader(reader);

        SHAMerge = br.readLine();

        while (SHAMerge != null) {

            List<String> parents = Git.getParents("C:\\Cristiane\\mestrado\\Experimentos\\repos\\che", SHAMerge);

            if (parents.size() == 2) {
                SHALeft = parents.get(0);
                SHARight = parents.get(1);
                SHAmergeBase = Git.getMergeBase("C:\\Cristiane\\mestrado\\Experimentos\\repos\\che", SHALeft, SHARight);
            }
            //Check if is a fast-forward merge
            System.out.println(SHALeft + ", " + SHARight + ", " + SHAmergeBase + ", " + SHAMerge + "\n");
            if (!(SHAmergeBase == null)) {
                if ((!(SHAmergeBase.equals(SHALeft))) && (!(SHAmergeBase.equals(SHARight)))) {
                   // System.out.println(SHAMerge);
                }
                
            }
            SHAMerge = br.readLine();
        }

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
            Logger.getLogger(br.uff.ic.merge.conceptualcoupling.Git.class.getName()).log(Level.SEVERE, null, ex);
        }

        return output;
    }

}

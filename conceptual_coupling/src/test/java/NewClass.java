

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Carlos
 */
public class NewClass {

    public static void main(String args[]) {
            
            String SHAMerge = null;
             String SHAMergeAnt = "12345678910111213141516171819202121";
        
         if (SHAMerge.equals(SHAMergeAnt)) {
             
             System.out.println("oi");
}}

        /*int a = 0;
        String line = "6c7e6abde2200d68c614ed94a15e07cddea919b1,LeftClass$listStudents,RightClass$Class,0.0";
        String SHAMerge = line.substring(0, 40);
        String leftMethod = line.substring(41, line.indexOf(",R"));
        String rightMethod = line.substring(line.indexOf(",R") + 1, line.lastIndexOf(','));
        String similarity = line.substring(line.lastIndexOf(',') + 1, line.length());
                    
        System.out.println(SHAMerge + ", " + leftMethod + ", " + rightMethod + "," + similarity);*/

        //String line = "@@ -10,8 15,13 @@ package metodo_atributo";

        /*line = line.replaceFirst("@@ -", "");
        String[] intervals = line.split("\\+");
        String[] limits = intervals[0].split(",");*/
        //@@ -10,8 10,13 @@ package metodo_atributo;
       /* String[] intervals = line.split("\\+");
        String[] limits;

        //intervals[1] = intervals[1].replaceFirst("+", "");
        if (intervals.length > 1) {
            limits = intervals[1].split(",");
        } else {
            line = line.replaceFirst("@@ -", "");
            intervals = line.split("\\+");
            limits = intervals[0].split(",");
        }

            a = Integer.parseInt(limits[0].replace(" ", ""));
            
            System.out.println(a);*/
        }

    //}



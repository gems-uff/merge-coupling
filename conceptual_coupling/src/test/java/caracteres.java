/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Carlos
 */
public class caracteres {

    public static void main(String[] args) {
        /* String text = "ola como você vai? [] , . ?<>"; 
      text = text.replaceAll("[^a-zZ-Z1-9 ]", ""); 
      //text = text.repce(" ", "");
      System.out.println(text); } */
        String SHALeft = "", SHARight = "", SHAmergeBase = "", SHAMerge = "";

        SHAMerge = "111";
        SHALeft = "345";
        SHARight = "222";
        SHAmergeBase = null;

        if (!(SHAmergeBase == null)) {
            if ((!(SHAmergeBase.equals(SHALeft))) && (!(SHAmergeBase.equals(SHARight)))) {
                System.out.println(SHAMerge);
            }
        }
    }
}

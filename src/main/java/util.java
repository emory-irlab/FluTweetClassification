import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;

/**
 * Created by Alec Wolyniec on 4/20/16.
 */
public class util {

    public static void printStringFeaturesIntValuesFromHashtable(Hashtable<String, Integer> table) {
        System.out.println("FEATURES FOR ONE ITEM:");
        Enumeration<String> features = table.keys();
        while (features.hasMoreElements()) {
            String feature = features.nextElement();
            int value = table.get(feature);
            System.out.println(feature+": "+value);
        }
    }

    /*
        Return the lemma of the inputted token, in all lower case if it is not a proper noun, with
        only the first letter capitalized if it is a proper noun
     */
    public static String lowerCaseLemmaUnlessProperNoun(CoreLabel token) {
        String lem = token.get(LemmaAnnotation.class);
        String tag = token.tag();
        if (tag.substring(0, Math.min(3, tag.length())).equals("NNP")) {
            StringBuilder output = new StringBuilder();
            output.append(Character.toUpperCase(lem.charAt(0)));
            output.append(lem.toLowerCase().substring(1));
            return output.toString();
        }
        else {
            return lem.toLowerCase();
        }
    }

    /*
    Return the lemma of the inputted token, in all lower case if it is not a proper noun, with
    only the first letter capitalized if it is a proper noun
    */
    public static String lowerCaseLemmaUnlessProperNoun(IndexedWord token) {
        String lem = token.get(LemmaAnnotation.class);
        String tag = token.tag();
        if (tag.substring(0, Math.min(3, tag.length())).equals("NNP")) {
            StringBuilder output = new StringBuilder();
            output.append(Character.toUpperCase(lem.charAt(0)));
            output.append(lem.toLowerCase().substring(1));
            return output.toString();
        }
        else {
            return lem.toLowerCase();
        }
    }

    public static int containsAlphabeticCharacters(String text) {
        for (int i = 0; i < text.length(); i++)
            if (Character.isAlphabetic(text.charAt(i))) return 1;
        return 0;
    }

    public static boolean isAllNumeric(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static void printAllFieldsOneLinePerEntry(ArrayList<String[]> input, String pathToOutputFile) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(pathToOutputFile)));
        for (String[] entry: input) {
            for (String en : entry) {
                if (en.contains(",")) {
                    bufferedWriter.write("\\\"");
                }
                bufferedWriter.write(en);
                if (en.contains(",")) {
                    bufferedWriter.write("\\\"");
                }
                bufferedWriter.write(",");
            }
            bufferedWriter.newLine();
        }

        bufferedWriter.close();
    }
}

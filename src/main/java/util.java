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

    public static int containsAlphabeticCharacters(String text) {
        for (int i = 0; i < text.length(); i++)
            if (Character.isAlphabetic(text.charAt(i))) return 1;
        return 0;
    }

    public static void emptyDirectory(File directory) {
        for (File fileEntry : directory.listFiles()) {
            if (fileEntry.isDirectory()) {
                emptyDirectory(fileEntry);
            } else {
                fileEntry.delete();
            }
        }
    }

    /*
        Takes all the non-directory files that are direct children of the current file, and
        combine all their text together

        Only combines files with the same, specified extension
     */
    public static void consolidateDirectory(String pathToDirectory, String fileExtension) throws IOException {
        File directory = new File(pathToDirectory);

        //set up a writer to write to the file
        File outputFile = new File(pathToDirectory.replace("\\/[^\\/]+$", "\\/")+"consolidated"+fileExtension);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile));

        for (File fileEntry: directory.listFiles()) {
            //make sure the file isn't a directory and it has the correct extension
            String path = fileEntry.getAbsolutePath();
            if (!fileEntry.isDirectory() && path.substring(path.length() - fileExtension.length()).equals(fileExtension)) {
                //open up the file and read each line to the output file
                BufferedReader bufferedReader = new BufferedReader(new FileReader(fileEntry));
                String currentLine;
                while ((currentLine = bufferedReader.readLine()) != null) {
                    bufferedWriter.write(currentLine+"\n");
                }

                bufferedReader.close();
            }
            bufferedWriter.flush();

        }
        bufferedWriter.close();
    }

    public static boolean isAllNumeric(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /*
        Return the lemma of the inputted token, in all lower case if it is not a proper noun, with
        only the first letter capitalized if it is a proper noun
    */
    public static String lowerCaseTextUnlessProperNoun(IndexedWord token) {
        String text = token.originalText();
        String tag = token.tag();
        if (tag.substring(0, Math.min(3, tag.length())).equals("NNP")) {
            StringBuilder output = new StringBuilder();
            output.append(Character.toUpperCase(text.charAt(0)));
            output.append(text.toLowerCase().substring(1));
            return output.toString();
        }
        else {
            return text.toLowerCase();
        }
    }

    public static void printAllFieldsOneLinePerEntry(ArrayList<String[]> input, String pathToOutputFile) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(pathToOutputFile)));
        for (String[] entry: input) {
            for (String en : entry) {
                bufferedWriter.write(en);
                bufferedWriter.write("\t");
            }
            bufferedWriter.newLine();
        }

        bufferedWriter.close();
    }

    public static void printStringFeaturesIntValuesFromHashtable(Hashtable<String, Integer> table) {
        System.out.println("FEATURES FOR ONE ITEM:");
        Enumeration<String> features = table.keys();
        while (features.hasMoreElements()) {
            String feature = features.nextElement();
            int value = table.get(feature);
            System.out.println(feature+": "+value);
        }
    }
}

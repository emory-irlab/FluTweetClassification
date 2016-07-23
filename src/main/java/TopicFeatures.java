import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.SystemUtils;

import java.io.*;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alec Wolyniec on 7/21/16.
 */
public class TopicFeatures {

    private static StanfordCoreNLP pipeline;
    private static long totalNumWords = 0;
    private static long numTopics = 0;
    private static File countFile;
    private static File compositionFile;
    private static File keyFile;

    /*
        Gets the probability of a given topic occurring in the text
     */
    private static double getProbabilityOfTopic(int topic, List<CoreLabel> textTokens) throws IOException {
        double probability = 0.0;

        //for each token, get the probability of the topic given it. Add it to the total probability of the topic
        for (CoreLabel token: textTokens) {
            //get the probability of the topic given the word
            String word = token.originalText().toLowerCase();

            //the probability of the word given the topic

            //the probability of the word
            double wordProb = getProbabilityOfWord(word);

            //the probability of the topic

            //topic given word
        }

        return probability;
    }

    /*
        Gets the total count of the word over the total number of words
     */
    public static double getProbabilityOfWord(String word) throws IOException {
        double prob = 0.0;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(countFile));
        String currentLine;

        while ((currentLine = bufferedReader.readLine()) != null) {
            String[] split = currentLine.split(" ");
            if (split.length < 1) {
                continue;
            }

            //check to see if this line contains the count for the provided word
            String wordInLine = split[1];
            if (wordInLine.equals(word)) {
                //start after the word itself
                for (int i = 2; i < split.length; i++) {
                    int relevantNumber = Integer.parseInt(split[i].substring(split[i].indexOf(":") + 1));
                    prob += relevantNumber;
                }
                break;
            }
        }
        prob /= totalNumWords;
        return prob;
    }

    private static void initializeNumTopics() throws FileNotFoundException, IOException {
        long total = 0;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(countFile));
        String currentLine;

        while ((currentLine = bufferedReader.readLine()) != null) {
            String[] split = currentLine.split(" ");
            if (split.length >= 1) {
                total++;
            }
        }
        numTopics = total;
    }

    /*
        Gets the total number of words
     */
    private static void initializeTotalWords() throws FileNotFoundException, IOException {
        long total = 0;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(countFile));
        String currentLine;

        while ((currentLine = bufferedReader.readLine()) != null) {
            String[] split = currentLine.split(" ");
            if (split.length < 1) {
                continue;
            }

            //start after the word itself
            for (int i = 2; i < split.length; i++) {
                int relevantNumber = Integer.parseInt(split[i].substring(split[i].indexOf(":") + 1));
                total += relevantNumber;
            }
        }

        totalNumWords = total;
    }

    /*
        For a given text, gets the probability of each topic occurring
     */
    public static int[] getNMostLikelyTopics(int N, String text, String pathToCompositionFile, String pathToCountFile, String pathToKeyFile) throws IOException {
        //initialize if things haven't been initialized yet
        countFile = new File(pathToCountFile);
        compositionFile = new File(pathToCompositionFile);
        keyFile = new File(pathToKeyFile);
        if (pipeline == null) {
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        }
        if (totalNumWords == 0) {
            initializeTotalWords();
        }
        //get the topic number
        if (numTopics == 0) {
            initializeNumTopics();
        }

        //annotate the text

        /*
        for (int i = 0; i < numTopics; i++) {
            getProbabilityOfTopic(i, )
        }
        */

        return new int[0];
    }

}

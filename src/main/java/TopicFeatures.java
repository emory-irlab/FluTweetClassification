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

    /*
        Gets the probability of a given topic occurring in the text
     */
    private static double getProbabilityOfTopic(int topic, List<CoreLabel> textTokens) {
        double probability = 0.0;
        for (CoreLabel token: textTokens) {
            //get the probability of the topic given the word
            String word = token.originalText().toLowerCase();

            //the probability of the word given the topic

            //the probability of the word

            //the probability of the topic
        }

        return probability;
    }

    /*
        Gets the total number of words
     */
    //MAKE PRIVATE
    public static void initializeTotalWords(String pathToCountFile) throws FileNotFoundException, IOException {
        long total = 0;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(pathToCountFile)));
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
        if (pipeline == null) {
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        }
        if (totalNumWords == 0) {
            initializeTotalWords(pathToCountFile);
        }

        return new int[0];
    }

}

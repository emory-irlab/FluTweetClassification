import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.SystemUtils;

import java.io.*;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Hashtable;
import java.util.Comparator;
import edu.stanford.nlp.util.Pair;
import java.util.PriorityQueue;

/**
 * Created by Alec Wolyniec on 7/21/16.
 */
public class TopicFeatures {

    private static StanfordCoreNLP pipeline;
    private static long totalNumWords = 0;
    private static File countFile;
    private static File compositionFile;
    private static File keyFile;
    //private static ArrayList<String[]> topics;
    private static Hashtable<Integer, Double> topicProbs = new Hashtable<Integer, Double>();

    /*
        Gets the probability of a given topic occurring in the text
     */
    private static double getProbabilityOfTopicGivenText(int topic, List<CoreLabel> textTokens) throws IOException {
        double probability = 0.0;

        //for each token, get the probability of the topic given it. Add it to the total probability of the topic
        for (CoreLabel token: textTokens) {
            //get the probability of the topic given the word
            String word = token.originalText().toLowerCase();

            //the probability of the word given the topic (not sure if gettable)

            //the probability of the word
            double wordProb = getProbabilityOfWord(word);

            //the probability of the topic
            double topicProb = topicProbs.get(topic);

            //topic given word
        }

        return probability;
    }

    /*
        Gets the total count of the word over the total number of words
     */
    private static double getProbabilityOfWord(String word) throws IOException {
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

    private static void initializeTopicProbs() throws FileNotFoundException, IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(compositionFile));
        String currentLine;

        //for each topic, average the probabilities of it occurring in all documents
        int documentCounter = 0;
        while ((currentLine = bufferedReader.readLine()) != null) {
            documentCounter++;
            String[] split = currentLine.split("\\t");
            for (int i = 2; i < split.length; i += 2) {
                int topicNum = Integer.parseInt(split[i]);
                double topicProb = Double.parseDouble(split[i+1]);
                if (topicProbs.get(topicNum) == null) {
                    topicProbs.put(topicNum, topicProb);
                }
                else {
                    topicProbs.put(topicNum, topicProbs.get(topicNum) + topicProb);
                }
            }
        }

        //divide by the number of documents to get the average
        Enumeration<Integer> topics = topicProbs.keys();
        while (topics.hasMoreElements()) {
            int key = topics.nextElement();
            topicProbs.put(key, topicProbs.get(key)/documentCounter);
        }
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
            pipeline = new StanfordCoreNLP(props);
        }
        if (totalNumWords == 0) {
            initializeTotalWords();
        }
        //get topic probabilities
        if (topicProbs.size() == 0) {
            initializeTopicProbs();
        }

        //annotate the text
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);
        List<CoreLabel> tokens = annotation.get(TokensAnnotation.class);

        //get the top N topic probabilities
        PriorityQueue<Pair<Integer, Double>> pq = new PriorityQueue<Pair<Integer, Double>>(N + 1, new intDoubleComparator());
        //get the probability of each individual topic and add it to the pq. If it's not in the top N, remove the lowest-probability item on the pq
        for (int i = 0; i < topicProbs.size(); i++) {
            pq.add(new Pair<Integer, Double>(i, getProbabilityOfTopicGivenText(i, tokens)));

            if (pq.size() > N) {
                pq.poll();
            }
        }

        return new int[0];
    }

}

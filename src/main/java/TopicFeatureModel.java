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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Alec Wolyniec on 7/21/16.
 */
public class TopicFeatureModel {

    private StanfordCoreNLP pipeline;
    private long totalNumWords = 0;
    private String countFilePath;
    private ArrayList<String> countFileSplitPaths = new ArrayList<String>();
    private File compositionFile;
    private Hashtable<Integer, Double> topicProbs = new Hashtable<Integer, Double>();
    private double basicTopicProb;
    private Hashtable<String, Integer> stopWordList = new Hashtable<String, Integer>();
    private String stopWordFilePath;
    private int nCores;
    //each key is a word, and each value is an array of pairs of <topic, number of times the word appears in the topic>
    private Hashtable<String, ArrayList<Pair<Integer, Integer>>> wordTopicCounts;

    public TopicFeatureModel(String pathToCountFile, String pathToCompositionFile, String sWordFilePath, int n) throws IOException {
        countFilePath = pathToCountFile;
        compositionFile = new File(pathToCompositionFile);
        nCores = n;
        if (pipeline == null) {
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize");
            pipeline = new StanfordCoreNLP(props);
        }
        //get the total number of words, as well as the x most common words
        initializeWordTopicCounts();

        //get topic probabilities
        initializeTopicProbs();

        //initialize stopwords
        stopWordFilePath = sWordFilePath;
        if (stopWordFilePath.length() > 0) {
            getStopWordList(sWordFilePath);
        }

    }

    /*
        Gets the probability of a given topic occurring in the text
     */
    public double getProbabilityOfTopicGivenText(int topic, List<CoreLabel> textTokens) throws IOException, InterruptedException {
        double probability = 0.0;

        //for each token, get the probability of the topic given it. Add it to the total probability of the topic
        for (CoreLabel token: textTokens) {
            //System.out.print(token.originalText()+ " ");
            //remove all apostrophes, since no tokens in the count file contain non-alphabetic characters
            String word = token.originalText().toLowerCase().replace("\'", "");

            //don't scan a token that contains non-alphabetic characters or is a stop word
            if (TextFeatures.containsNonAlphabeticCharacters(word) == 1 || isStopWord(word)) {
                continue;
            }

            //the probability of the word given the topic (likely off by a linear factor)
            double wordGivenTopicProb = getProbabilityOfWordGivenTopic(word, topic);

            //the probability of the word
            double wordProb = getProbabilityOfWord(word);

            //the probability of the topic
            double topicProb = topicProbs.get(topic);

            //use the above fields to assemble P(topic | word)
            if (wordProb > 0.0) {
                probability += (wordGivenTopicProb * topicProb) / wordProb;
            }
        }
        return probability;
    }

    public double getProbabilityOfWordGivenTopic(String word, int topic) throws FileNotFoundException, IOException, InterruptedException {
        assert (topic >= 0);
        assert (topic < topicProbs.size());

        //get the number of instances of the word in the topic
        int instancesOfWordInTopic = 0;
        ArrayList<Pair<Integer, Integer>> topicCountsForWord = wordTopicCounts.get(word);

        //return a probability of 0 if this word is not known
        if (topicCountsForWord == null) {
            return 0.0;
        }

        //otherwise, check through all the topics and counts
        for (Pair<Integer, Integer> topicAndCount: topicCountsForWord) {
            int topicThisCount = topicAndCount.first();
            int countThisCount = topicAndCount.second();

            if (topicThisCount == topic) {
                instancesOfWordInTopic = countThisCount;
                break;
            }
        }

        //get the number of instances of the topic
        double topicInstances = topicProbs.get(topic)/basicTopicProb;

        return instancesOfWordInTopic/topicInstances;
    }

    /*
        Gets the total count of the word over the total number of words
     */
    public double getProbabilityOfWord(String word) throws IOException, InterruptedException {
        double prob = 0.0;

        ArrayList<Pair<Integer, Integer>> topicCountsForWord = wordTopicCounts.get(word);
        //return a probability of 0 if the word is not known
        if (topicCountsForWord == null) {
            return 0.0;
        }

        //otherwise, get its probability
        for (Pair<Integer, Integer> topicAndCount: topicCountsForWord) {
            prob += topicAndCount.second(); //topicAndCount.second() is the count
        }

        prob /= totalNumWords;
        return prob;
    }

    /*
        Initialize the hashtable containing the probability of each topic
     */
    public void initializeTopicProbs() throws FileNotFoundException, IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(compositionFile));
        String currentLine;

        //for each topic, average the probabilities of it occurring in all documents
        int documentCounter = 0;
        currentLine = bufferedReader.readLine(); //skip the first line
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

        //Get the probability representing 1 instance (100x the smallest probability in topicProbs)
        double smallest = 1.0;
        topics = topicProbs.keys();
        while (topics.hasMoreElements()) {
            double poss = topicProbs.get(topics.nextElement());
            if (poss < smallest) {
                smallest = poss;
            }
        }
        basicTopicProb = smallest;

        bufferedReader.close();
    }

    /*
        Gets a hashtable of all the words and their topic counts. Also collects the total number of instances of words
     */
    public void initializeWordTopicCounts() throws FileNotFoundException, IOException {
        wordTopicCounts = new Hashtable<String, ArrayList<Pair<Integer, Integer>>>();

        //counts the total number of word instances
        long instancesOfAllWords = 0;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(countFilePath)));
        String currentLine;

        currentLine = bufferedReader.readLine(); //skip the first line; it merely contains headers

        //look through each word
        while ((currentLine = bufferedReader.readLine()) != null) {
            String[] split = currentLine.split(" ");
            if (split.length < 3) {
                continue;
            }

            String word = split[1];
            ArrayList<Pair<Integer, Integer>> topicCountsThisWord = new ArrayList<Pair<Integer, Integer>>();

            //check all topics the word appears in, collect all topic-count pairs
            for (int i = 2; i < split.length; i++) {
                String[] topicAndCount = split[i].split(":");
                int topic = Integer.parseInt(topicAndCount[0]);
                int count = Integer.parseInt(topicAndCount[1]);
                instancesOfAllWords += count;

                //save the pair
                topicCountsThisWord.add(new Pair<Integer, Integer>(topic, count));
            }

            //add the data on this word to the hashtable
            wordTopicCounts.put(word, topicCountsThisWord);
        }

        totalNumWords = instancesOfAllWords;
        bufferedReader.close();
    }

    /*
        For a given text, gets the probability of each topic occurring
     */
    public int[] getNMostLikelyTopics(int N, String text) throws IOException, InterruptedException {
        //annotate the text
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);
        List<CoreLabel> tokens = annotation.get(TokensAnnotation.class);

        //get a pq of the top N topic probabilities
        PriorityQueue<Pair<Integer, Double>> pq = new PriorityQueue<Pair<Integer, Double>>(N + 1, new intDoubleComparator());
        //get the probability of each individual topic and add it to the pq. If it's not in the top N, remove the lowest-probability item on the pq
        assert(topicProbs.size() >= N);
        for (int i = 0; i < topicProbs.size(); i++) {
            pq.add(new Pair<Integer, Double>(i, getProbabilityOfTopicGivenText(i, tokens)));

            if (pq.size() > N) {
                pq.poll();
            }
        }

        //get an array of the top topics
        int[] top = new int[N];
        int topCounter = 0;
        while (pq.size() > 0) {
            top[topCounter] = pq.poll().first();
            topCounter++;
        }

        return top;
    }

    //section dealing with stopwords
    public boolean isStopWord(String input) {
        /*
        for (String stopWord: stopWordList) {
            if (input.equals(stopWord)) return true;
        }
        */
        if (stopWordList.get(input) != null) {
            return true;
        }
        return false;
    }

    /*
        generates a list of stop words from a file
    */
    public void getStopWordList(String stopWordFilePath) throws FileNotFoundException, IOException {
        //ArrayList<String> stopWords = new ArrayList<String>(0);
        Hashtable<String, Integer> stopWords = new Hashtable<String, Integer>();
        BufferedReader reader = new BufferedReader(new FileReader(new File(stopWordFilePath)));
        //read
        String currentLine;
        //Pattern pattern = Pattern.compile("(\\S+)\\s");
        Pattern pattern = Pattern.compile("(\\S+)");
        Matcher matcher;
        while ((currentLine = reader.readLine()) != null) {
            matcher = pattern.matcher(currentLine);
            while (matcher.find()) {
                //stopWords.add(matcher.group(1));
                stopWords.put(matcher.group(1), 1);
            }
            //stopWords.add(currentLine);
        }

        stopWordList = stopWords;
        reader.close();
    }

}

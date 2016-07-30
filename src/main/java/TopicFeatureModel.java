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

    private class RecentWordsContainer {
        private Hashtable<String, ArrayList<String>> recentWords = new Hashtable<String, ArrayList<String>>();
        private int sizeLimit = 100; //customized for the size of a tweet

        public ArrayList<String> get(String key) {
            return recentWords.get(key);
        }

        public void flush() {
            recentWords = new Hashtable<String, ArrayList<String>>();
        }

        public void put(String key, ArrayList<String> object) {
            if (recentWords.size() >= sizeLimit) {
                flush();
            }
            recentWords.put(key, object);
        }
    }

    private StanfordCoreNLP pipeline;
    private long totalNumWords = 0;
    private File countFile;
    private File compositionFile;
    private Hashtable<Integer, Double> topicProbs = new Hashtable<Integer, Double>();
    private double basicTopicProb;
    private Hashtable<String, ArrayList<String>> mostCommonWords;
    private int mostCommonWordsSize = 20000;
    private RecentWordsContainer recentWords;
    private Hashtable<String, Integer> stopWordList = new Hashtable<String, Integer>();
    private String stopWordFilePath;

    //size-limited container of recent words and their entries

    public TopicFeatureModel(String pathToCountFile, String pathToCompositionFile, String sWordFilePath) throws IOException {
        mostCommonWords = new Hashtable<String, ArrayList<String>>();
        recentWords = new RecentWordsContainer();
        countFile = new File(pathToCountFile);
        compositionFile = new File(pathToCompositionFile);
        if (pipeline == null) {
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize");
            pipeline = new StanfordCoreNLP(props);
        }
        //get the total number of words, as well as the x most common words
        initializeTotalWords();

        //get topic probabilities
        initializeTopicProbs();

        //initialize stopwords
        stopWordFilePath = sWordFilePath;
        getStopWordList(sWordFilePath);
    }

    /*
        Gets an ArrayList of the counts of each word across all topics it's in
     */
    public ArrayList<String> getTopicCountsForWord(String word) throws IOException {
        ArrayList<String> result;
        //first check to see if it's in the recent words list
        result = recentWords.get(word);
        if (result != null) {
            return result;
        }
        //if it's not in the recent words list, check the most common words list. Return the result and place into
        //the recent words list if it can be found

        result = mostCommonWords.get(word);
        if (result != null) {
            recentWords.put(word, result);
            return result;
        }


        //if it still hasn't been found, attempt to extract counts from the countFile. Return the result (whether
        //counts have been found or not), and place it into the recent words list
        result = new ArrayList<String>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(countFile));
        String currentLine;

        while ((currentLine = bufferedReader.readLine()) != null) {
            String[] split = currentLine.split(" ");
            if (split.length < 3) {
                continue;
            }

            //check to see if this line contains the count for the provided word
            String wordInLine = split[1];
            if (wordInLine.equals(word)) {
                //if it does, append all of the counts to the ArrayList
                for (int i = 2; i < split.length; i++) {
                    result.add(split[i]);
                }
                break;
            }
        }

        //return the result from the countFile. If the word has not been found, an empty ArrayList will be returned
        //representing 0 instances across all topics
        recentWords.put(word, result);
        return result;
    }

    /*
        Gets the probability of a given topic occurring in the text
     */
    public double getProbabilityOfTopicGivenText(int topic, List<CoreLabel> textTokens) throws IOException {
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
            probability += (wordGivenTopicProb * topicProb) / wordProb;
        }
        return probability;
    }

    public double getProbabilityOfWordGivenTopic(String word, int topic) throws FileNotFoundException, IOException {
        assert (topic >= 0);
        assert (topic < topicProbs.size());

        //get the number of instances of the word in the topic
        int instancesInTopic = 0;
        ArrayList<String> topicCountsForWord = getTopicCountsForWord(word);
        //check through all the topics and counts
        for (String topicAndCount: topicCountsForWord) {
            String[] splitTAndC = topicAndCount.split(":");
            //if the word has a count for the given topic, use that as the count
            if (Integer.parseInt(splitTAndC[0]) == topic) {
                instancesInTopic = Integer.parseInt(splitTAndC[1]);
            }
        }

        //get the number of instances of the topic
        double topicInstances = topicProbs.get(topic)/basicTopicProb;

        return instancesInTopic/topicInstances;
    }

    /*
        Gets the total count of the word over the total number of words
     */
    public double getProbabilityOfWord(String word) throws IOException {
        double prob = 0.0;

        ArrayList<String> topicCountsForWord = getTopicCountsForWord(word);
        for (String topicAndCount: topicCountsForWord) {
            prob += Integer.parseInt(topicAndCount.split(":")[1]); //split[1] is the count
        }

        prob /= totalNumWords;
        return prob;
    }

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
    }

    /*
        Gets the total number of words. Also collects all data on the N most common words
     */
    public void initializeTotalWords() throws FileNotFoundException, IOException {
        long instancesOfAllWords = 0;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(countFile));
        String currentLine;
        currentLine = bufferedReader.readLine(); //skip the first line

        //to contain the N most frequently occurring words
        PriorityQueue<Pair<Pair<String, Integer>, ArrayList<String>>> pq = new PriorityQueue<Pair<Pair<String, Integer>, ArrayList<String>>>(mostCommonWordsSize + 1, new MostCommonWordsComparator());

        while ((currentLine = bufferedReader.readLine()) != null) {
            String[] split = currentLine.split(" ");
            if (split.length < 1) {
                continue;
            }

            //start after the word itself
            //keep a record of the word
            //also get the total number of instances of the word

            Pair<Pair<String, Integer>, ArrayList<String>> wordRecord;
            String word = split[1];
            int instancesOfWord = 0;
            ArrayList<String> instancesOfWordByTopic = new ArrayList<String>();


            //check all topics the word appears in
            for (int i = 2; i < split.length; i++) {
                int relevantNumber = Integer.parseInt(split[i].substring(split[i].indexOf(":") + 1));
                instancesOfAllWords += relevantNumber;

                //save the instances of the word in the topic, as well as the count

                instancesOfWordByTopic.add(split[i]);
                instancesOfWord += relevantNumber;

            }

            //complete the word's record and add it to the priority queue. If it is not one of the top N, remove it

            wordRecord = new Pair<Pair<String, Integer>, ArrayList<String>>(new Pair<String, Integer>(word, instancesOfWord), instancesOfWordByTopic);
            pq.add(wordRecord);
            if (pq.size() > mostCommonWordsSize) {
                pq.poll();
            }

        }

        //convert the pq of the top N words into the mostCommonWords hashtable for easier access

        while (pq.size() > 0) {
            //get the entry
            Pair<Pair<String, Integer>, ArrayList<String>> entry = pq.poll();
            //put the word and its counts across topics into the hashtable
            mostCommonWords.put(entry.first().first(), entry.second());
        }


        totalNumWords = instancesOfAllWords;
    }

    /*
        For a given text, gets the probability of each topic occurring
     */
    public int[] getNMostLikelyTopics(int N, String text) throws IOException {
        //annotate the text
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);
        List<CoreLabel> tokens = annotation.get(TokensAnnotation.class);

        //get a pq of the top N topic probabilities
        PriorityQueue<Pair<Integer, Double>> pq = new PriorityQueue<Pair<Integer, Double>>(N + 1, new intDoubleComparator());
        //get the probability of each individual topic and add it to the pq. If it's not in the top N, remove the lowest-probability item on the pq
        assert(topicProbs.size() >= N);
        recentWords.flush(); //ensure that the current recentWords entry only contains the current text's words
        for (int i = 0; i < topicProbs.size(); i++) {
            pq.add(new Pair<Integer, Double>(i, getProbabilityOfTopicGivenText(i, tokens)));

            if (pq.size() > N) {
                pq.poll();
            }
        }
        recentWords.flush(); //ensure that the current recentWords entry only contains the current text's words

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
    }

}

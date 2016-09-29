import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.CoreMap;

import java.util.ArrayList;
import java.io.*;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Various test cases for the features used by this project's classifiers
 *
 *
 * Created by Alec Wolyniec on 6/9/16.
 */
public class featureTestCases {

    //topic features
    public static void testGetProbabilityOfWord() throws IOException {
        System.out.println();
        System.out.println("Testing topic features' get probability of word...");
        String pathToCountFile = "data/topics/countFile.txt";
        String pathToCompositionFile = "data/topics/tweet_composition.txt";
        try {
            TopicFeatureModel model = new TopicFeatureModel(pathToCountFile, pathToCompositionFile, "data/stopwords.txt", 1);

            if (model.getProbabilityOfWord("canadian") != ((double)5246)/103277747 ) {
                System.out.println("FAILED due to improper values");
            }
            else if (model.getProbabilityOfWord("dillpickles") != 0.0) {
                System.out.println("FAILED due to improper values");
            }
            else if (model.getProbabilityOfWord("fredconcha") != ((double)3)/103277747) {
                System.out.println("FAILED due to improper values");
            }
            else if (model.getProbabilityOfWord("consigo") != ((double)403)/103277747) {
                System.out.println("FAILED due to improper values");
            }
            else if (model.getProbabilityOfWord("consiga") != ((double)37)/103277747) {
                System.out.println("FAILED due to improper values");
            }
            else if (model.getProbabilityOfWord("video") != ((double)239395)/103277747) {
                System.out.println("FAILED due to improper values");
            }
            else if (model.getProbabilityOfWord("flag") != ((double)20078)/103277747 ) {
                System.out.println("FAILED due to improper values");
            }
            else if (model.getProbabilityOfWord("vegan") != ((double)4168)/103277747 ) {
                System.out.println("FAILED due to improper values");
            }
            else if (model.getProbabilityOfWord("nigerians") != ((double)1466)/103277747 ) {
                System.out.println("FAILED due to improper values");
            }
            else {
                System.out.println("PASSED.");
            }
        }
        catch (Exception e) {
            System.out.println("FAILED due to exception");
            e.printStackTrace();
        }
    }

    public static void testGetProbabilityOfTopic() {

    }

    public static void testGetProbabilityOfWordGivenTopic() throws IOException {
        System.out.println();
        System.out.println("Testing topic features get probability of word given topic method...");
        try {
            //set up the topic model with the given files
            String pathToCountFile = "data/topics/countFile.txt";
            String pathToCompositionFile = "data/topics/tweet_composition.txt";
            TopicFeatureModel model = new TopicFeatureModel(pathToCountFile, pathToCompositionFile, "data/stopwords.txt", 1);

            if (Math.abs(model.getProbabilityOfWordGivenTopic("monday", 38) - 334.347136) > 0.0001) {
                System.out.println("FAILED. Incorrect value for word \"monday\" given topic 38");
            }
            else if (Math.abs(model.getProbabilityOfWordGivenTopic("monday", 42) - 302.14865) > 0.0001) {
                System.out.println("FAILED. Incorrect value for word \"monday\" given topic 42");
            }
            else if (Math.abs(model.getProbabilityOfWordGivenTopic("weekend", 39) - 236.868367) > 0.0001) {
                System.out.println("FAILED. Incorrect value for word \"weekend\" given topic 39");
            }
            else if (model.getProbabilityOfWordGivenTopic("burrito", 19) != 0.0) {
                System.out.println("FAILED. Incorrect value for word \"burrito\" given topic 19");
            }
            else if (Math.abs(model.getProbabilityOfWordGivenTopic("flag", 38) - 247.360892) > 0.0001) {
                System.out.println("FAILED. Incorrect value for word \"flag\" given topic 38");
            }
            else if (Math.abs(model.getProbabilityOfWordGivenTopic("chicago", 59) - 134.075720) > 0.0001) {
                System.out.println("FAILED. Incorrect value for word \"chicago\" given topic 59");
            }
            else if (Math.abs(model.getProbabilityOfWordGivenTopic("donut", 39) - 221.1675998) > 0.0001) {
                System.out.println("FAILED. Incorrect value for word \"donut\" given topic 39");
            }
            else {
                System.out.println("PASSED.");
            }
        }
        catch (Exception e) {
            System.out.println("FAILED due to exception.");
            e.printStackTrace();
        }

    }

    public static void testGetProbabilityOfWordGivenTopicMultithreaded() {
        System.out.println();
        System.out.println("Testing get probability of word given topic method, using 4-core multithreading...");
        //set up the topic model with the given files
        String pathToCountFile = "data/topics/countFile.txt";
        String pathToCompositionFile = "data/topics/tweet_composition.txt";
        try {
            TopicFeatureModel model = new TopicFeatureModel(pathToCountFile, pathToCompositionFile, "data/stopwords.txt", 4);

            if (Math.abs(model.getProbabilityOfWordGivenTopic("flag", 38) - 247.360892) > 0.0001) {
                System.out.println("FAILED. Incorrect value for word \"flag\" given topic 38");
            }
            else if (Math.abs(model.getProbabilityOfWordGivenTopic("chicago", 59) - 134.075720) > 0.0001) {
                System.out.println("FAILED. Incorrect value for word \"chicago\" given topic 59");
            }
            else if (Math.abs(model.getProbabilityOfWordGivenTopic("donut", 39) - 221.1675998) > 0.0001) {
                System.out.println("FAILED. Incorrect value for word \"donut\" given topic 39");
            }
            else {
                System.out.println("PASSED.");
            }
        }
        catch (Exception e) {
            System.out.println("FAILED due to exception.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        //tests for the event classifier

        //tests for the N-gram model (all words measure by it are assumed to be lowercase unless they are proper nouns,
        //in which case the first character (and only the first character) is uppercase
         //test the unigram idf method
        //testUnigramFeaturesIDFThresholdOf1WithStopWords();

         //test the combination of the unigram tf and idf methods
        //testUnigramFeaturesTFIDFThresholdOf1WithStopWords();

         //test a tf-idf unigram model with a higher frequency threshold than 1
        //testUnigramFeaturesTFIDFThresholdOf2WithStopWords();

         //test a tf-idf unigram model without stopwords
        //testUnigramFeaturesTFIDFThresholdOf1NoStopWords();

         //test a tf-idf bigram model without stopwords
        //testBigramFeaturesTFIDFThresholdOf1NoStopWords();

         //test 6-grams
        //test6gramFeaturesTFIDFThresholdOf1NoStopWords();

         //test phrase template model

         //test topic modeling
        testGetProbabilityOfWord();
        //testGetProbabilityOfTopic();
        testGetProbabilityOfWordGivenTopic();
        testGetProbabilityOfWordGivenTopicMultithreaded();
        //testGetNMostLikelyTopics();

         //test other features

    }
}

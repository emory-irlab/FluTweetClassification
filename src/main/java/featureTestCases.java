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
 * Created by Alec Wolyniec on 6/9/16.
 */
public class featureTestCases {

    /*
        From 5 training tweets, collect each word's idf (number of documents / word's number of occurrences within the
        corpus) and check if the idf values are correct
            Expected idf values:
            -this - 5
            -be - 5/3
            -a - 5/3
            -test - 2.5
            -for - 5
            -Speakonia - 5
            -Mr. - 5
            -Bingo - 5
            -you - 2.5
            -will - 2.5
            -fail - 5/3
            -the - 5
            -i - 5
            -can - 5/3
            -Disneyland - 5
            -steal - 5
            -that - 5
            -my - 5
            -dear - 5
            -not - 2.5
            -ideal - 5
            -or - 5
            -it - 2.5
            - -lrb- - 5
            - -rrb- - 5
            -Can - 5
    */
    public static void testUnigramFeaturesIDFThresholdOf1WithStopWords() throws IOException {
        //---------------
        //TRAINING
        //---------------
        //set up vars
        ArrayList<String[]> trainingTweets = TweetParser.getTweets("data/testData/5ExampleTweetTexts.txt");
        ArrayList<String> trainingLabelSet = new ArrayList<String>();
        TweetVector[] trainingTweetVectors = new TweetVector[trainingTweets.size()];

        //train the model on the training data
        for (int i = 0; i < trainingTweets.size(); i++) {
            String[] tweet = trainingTweets.get(i);
            trainingTweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5], trainingLabelSet);
        }
        NGramModel unigramModel = new NGramModel(1, trainingTweetVectors, NGramModel.textName, "", 1);


        //--------------
        //TESTING
        //--------------

        //annotator
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //get the test data into phrase form so tf features can be extracted
        ArrayList<String[]> testingTweets = TweetParser.getTweets("data/testData/5MoreExampleTweetTexts.txt");

        System.out.println("Testing IDF values with a frequency threshold of 1 and stop words...");
        try {
            Hashtable<String, Double> idfs = unigramModel.getTweetIDFs();

            //check the idfs
            if (idfs.get("this") != 5.0 ||
                    idfs.get("be") != ((double) 5) / 3 ||
                    idfs.get("a") != ((double) 5) / 3 ||
                    idfs.get("test") != 2.5 ||
                    idfs.get("for") != 5.0 ||
                    idfs.get("Speakonia") != 5.0 ||
                    idfs.get("Mr.") != 5.0 ||
                    idfs.get("Bingo") != 5.0 ||
                    idfs.get("you") != 2.5 ||
                    idfs.get("will") != 2.5 ||
                    idfs.get("fail") != ((double) 5) / 3 ||
                    idfs.get("the") != 5.0 ||
                    idfs.get("i") != 5.0 ||
                    idfs.get("can") != ((double) 5) / 3 ||
                    idfs.get("Disneyland") != 5.0 ||
                    idfs.get("steal") != 5.0 ||
                    idfs.get("that") != 5.0 ||
                    idfs.get("my") != 5.0 ||
                    idfs.get("dear") != 5.0 ||
                    idfs.get("not") != 2.5 ||
                    idfs.get("ideal") != 5.0 ||
                    idfs.get("or") != 5.0 ||
                    idfs.get("it") != 2.5 ||
                    idfs.get("-lrb-") != 5 ||
                    idfs.get("-rrb-") != 5 ||
                    idfs.get("Can") != 5) {
                System.out.println("FAILED due to improper idf value(s) for word(s)");
            } else if (unigramModel.getNumIDFs() != 26) {
                System.out.println("FAILED due to superfluous idfs");
            }
            else {
                System.out.println("PASSED.");
            }
        }
        catch (Exception e) {
            System.out.println("FAILED due to exception");
            System.out.println(e.toString());
        }
    }

    /*
    Assuming a working TweetVector model and a working Stanford CoreNLP model, tests to see if the unigram model
    present here will yield the correct tf-idf values for words in 10 example tweets (5 training and 5 testing),
    including stop words. The unigram model must also represent all words in lowercase unless they are proper
    nouns, in which case the first letter (and only the first letter) is capitalized.

    Expected tf-idf values:
    First tweet:
        -test - 2.5
        -be - 5/3

    Second tweet:
        -a - 10/3
        -test - 5
        -be - 5/3

    Third:
        -Can - 5
        -you - 5
        -can - 5/3
        -i - 5

    Fourth:
        -be - 5/3
        -you - 2.5
        -Disneyland - 5

    Fifth:

 */
    public static void testUnigramFeaturesTFIDFThresholdOf1WithStopWords() throws IOException {
        //---------------
        //TRAINING
        //---------------
        //set up vars
        ArrayList<String[]> trainingTweets = TweetParser.getTweets("data/testData/5ExampleTweetTexts.txt");
        ArrayList<String> trainingLabelSet = new ArrayList<String>();
        TweetVector[] trainingTweetVectors = new TweetVector[trainingTweets.size()];

        //train the model on the training data
        for (int i = 0; i < trainingTweets.size(); i++) {
            String[] tweet = trainingTweets.get(i);
            trainingTweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5], trainingLabelSet);
        }
        NGramModel unigramModel = new NGramModel(1, trainingTweetVectors, NGramModel.textName, "", 1);


        //--------------
        //TESTING
        //--------------

        //feature var
        ArrayList<Hashtable<String, Double>> tfIdfFeatures = new ArrayList<Hashtable<String, Double>>();
        //annotator
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //get the test data into phrase form so tf features can be extracted
        ArrayList<String[]> testingTweets = TweetParser.getTweets("data/testData/5MoreExampleTweetTexts.txt");

        //get the tfs
        for (String[] tweet: testingTweets) {
            Annotation tweetDocument = new Annotation(readTweetsGetFeatures.process(tweet[4]));
            pipeline.annotate(tweetDocument);
            CoreLabel[][] tweetPhrases = readTweetsGetFeatures.getPhrases(tweetDocument);

            Hashtable<String, Double> tfIdfForTweet = unigramModel.getFeaturesForTweetTFIDF(tweetPhrases);
            tfIdfFeatures.add(tfIdfForTweet);
        }

        System.out.println("Testing TF-IDF unigram features with a document frequency threshold of 1 and stop words...");
        try {
            //generate vars
            Hashtable<String, Double> firstTweet = tfIdfFeatures.get(0);
            Hashtable<String, Double> secondTweet = tfIdfFeatures.get(1);
            Hashtable<String, Double> thirdTweet = tfIdfFeatures.get(2);
            Hashtable<String, Double> fourthTweet = tfIdfFeatures.get(3);
            Hashtable<String, Double> fifthTweet = tfIdfFeatures.get(4);

            //check the tf-idf values
            //tweet 1
            if (firstTweet.get("test") != 2.5 ||
                    firstTweet.get("be") != ((double)5)/3) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.size() != 2) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 2
            else if (secondTweet.get("a") != ((double)10)/3 ||
                    secondTweet.get("test") != 5.0 ||
                    secondTweet.get("be") != ((double)5)/3) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.size() != 3) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 3
            else if (thirdTweet.get("can") != ((double)5)/3 ||
                    thirdTweet.get("you") != 5.0 ||
                    thirdTweet.get("Can") != 5.0 ||
                    thirdTweet.get("i") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (thirdTweet.size() != 4) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 4
            else if (fourthTweet.get("be") != ((double)5)/3 ||
                    fourthTweet.get("you") != 2.5 ||
                    fourthTweet.get("Disneyland") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (fourthTweet.size() != 3) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 5
            else if (fifthTweet.size() != 0) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            else {
                System.out.println("PASSED.");
            }
        }
        catch (Exception e) {
            System.out.println("FAILED due to exception.");
            System.out.println(e.getMessage());
        }
    }

    /*
        From 5 training tweets, test to see if tf-idf values are correct when only words that appear at least 2 times
        in the training tweets are considered.

        Expected tf-idf values:
        First tweet:
            -test - 2.5
            -be - 5/3

        Second tweet:
            -a - 10/3
            -test - 5
            -be - 5/3

        Third:
            -you - 5
            -can - 5/3

        Fourth:
            -be - 5/3
            -you - 2.5

        Fifth:

     */
    public static void testUnigramFeaturesTFIDFThresholdOf2WithStopWords() throws IOException {
        //---------------
        //TRAINING
        //---------------
        //set up vars
        ArrayList<String[]> trainingTweets = TweetParser.getTweets("data/testData/5ExampleTweetTexts.txt");
        ArrayList<String> trainingLabelSet = new ArrayList<String>();
        TweetVector[] trainingTweetVectors = new TweetVector[trainingTweets.size()];

        //train the model on the training data
        for (int i = 0; i < trainingTweets.size(); i++) {
            String[] tweet = trainingTweets.get(i);
            trainingTweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5], trainingLabelSet);
        }
        NGramModel unigramModel = new NGramModel(1, trainingTweetVectors, NGramModel.textName, "", 2);


        //--------------
        //TESTING
        //--------------

        //feature var
        ArrayList<Hashtable<String, Double>> tfIdfFeatures = new ArrayList<Hashtable<String, Double>>();
        //annotator
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //get the test data into phrase form so tf features can be extracted
        ArrayList<String[]> testingTweets = TweetParser.getTweets("data/testData/5MoreExampleTweetTexts.txt");

        //get the tfs
        for (String[] tweet: testingTweets) {
            Annotation tweetDocument = new Annotation(readTweetsGetFeatures.process(tweet[4]));
            pipeline.annotate(tweetDocument);
            CoreLabel[][] tweetPhrases = readTweetsGetFeatures.getPhrases(tweetDocument);

            Hashtable<String, Double> tfIdfForTweet = unigramModel.getFeaturesForTweetTFIDF(tweetPhrases);
            tfIdfFeatures.add(tfIdfForTweet);
        }

        System.out.println("Testing TF-IDF unigram features with a document frequency threshold of 2 and stop words...");
        try {
            //generate vars
            Hashtable<String, Double> firstTweet = tfIdfFeatures.get(0);
            Hashtable<String, Double> secondTweet = tfIdfFeatures.get(1);
            Hashtable<String, Double> thirdTweet = tfIdfFeatures.get(2);
            Hashtable<String, Double> fourthTweet = tfIdfFeatures.get(3);
            Hashtable<String, Double> fifthTweet = tfIdfFeatures.get(4);

            //check the tf-idf values
            //tweet 1
            if (firstTweet.get("test") != 2.5 ||
                    firstTweet.get("be") != ((double)5)/3) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.size() != 2) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 2
            else if (secondTweet.get("a") != ((double)10)/3 ||
                    secondTweet.get("test") != 5.0 ||
                    secondTweet.get("be") != ((double)5)/3) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.size() != 3) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 3
            else if (thirdTweet.get("can") != ((double)5)/3 ||
                    thirdTweet.get("you") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (thirdTweet.size() != 2) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 4
            else if (fourthTweet.get("be") != ((double)5)/3 ||
                    fourthTweet.get("you") != 2.5) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (fourthTweet.size() != 2) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 5
            else if (fifthTweet.size() != 0) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            else {
                System.out.println("PASSED.");
            }
        }
        catch (Exception e) {
            System.out.println("FAILED due to exception.");
            System.out.println(e.getMessage());
        }
    }

    /*
        Test to see if tf-idf values remain correct even if stopwords are removed

        Expected tf-idf values:
        First tweet:
            -test - 2.5

        Second tweet:
            -test - 5

        Third:
            -Can - 5.0

        Fourth:
            -Disneyland - 5.0

        Fifth:

     */
    public static void testUnigramFeaturesTFIDFThresholdOf1NoStopWords() throws IOException {
        //---------------
        //TRAINING
        //---------------
        //set up vars
        ArrayList<String[]> trainingTweets = TweetParser.getTweets("data/testData/5ExampleTweetTexts.txt");
        ArrayList<String> trainingLabelSet = new ArrayList<String>();
        TweetVector[] trainingTweetVectors = new TweetVector[trainingTweets.size()];

        //train the model on the training data
        for (int i = 0; i < trainingTweets.size(); i++) {
            String[] tweet = trainingTweets.get(i);
            trainingTweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5], trainingLabelSet);
        }
        NGramModel unigramModel = new NGramModel(1, trainingTweetVectors, NGramModel.textName, "data/stopwords.txt", 1);


        //--------------
        //TESTING
        //--------------
        //feature var
        ArrayList<Hashtable<String, Double>> tfIdfFeatures = new ArrayList<Hashtable<String, Double>>();
        //annotator
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //get the test data into phrase form so tf features can be extracted
        ArrayList<String[]> testingTweets = TweetParser.getTweets("data/testData/5MoreExampleTweetTexts.txt");

        //get the tfs
        for (String[] tweet: testingTweets) {
            Annotation tweetDocument = new Annotation(readTweetsGetFeatures.process(tweet[4]));
            pipeline.annotate(tweetDocument);
            CoreLabel[][] tweetPhrases = readTweetsGetFeatures.getPhrases(tweetDocument);

            Hashtable<String, Double> tfIdfForTweet = unigramModel.getFeaturesForTweetTFIDF(tweetPhrases);
            tfIdfFeatures.add(tfIdfForTweet);
        }

        System.out.println("Testing TF-IDF unigram features with a document frequency threshold of 1 and no stop words...");
        try {
            //generate vars
            Hashtable<String, Double> firstTweet = tfIdfFeatures.get(0);
            Hashtable<String, Double> secondTweet = tfIdfFeatures.get(1);
            Hashtable<String, Double> thirdTweet = tfIdfFeatures.get(2);
            Hashtable<String, Double> fourthTweet = tfIdfFeatures.get(3);
            Hashtable<String, Double> fifthTweet = tfIdfFeatures.get(4);

            //check the tf-idf values
            //tweet 1
            if (firstTweet.get("test") != 2.5) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.size() != 1) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 2
            else if (secondTweet.get("test") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.size() != 1) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 3
            else if (thirdTweet.get("Can") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (thirdTweet.size() != 1) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 4
            else if (fourthTweet.get("Disneyland") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (fourthTweet.size() != 1) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 5
            else if (fifthTweet.size() != 0) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            else {
                System.out.println("PASSED.");
            }
        }
        catch (Exception e) {
            System.out.println("FAILED due to exception.");
            System.out.println(e.getMessage());
        }
    }

    /*
        From 5 training and 5 test tweets, extract tf-idf features on bigrams and check to make sure they're correct.

        Expected values:
        First tweet:
        -observe chicken - 2.5
        -table -post- - 5.0
        -chicken soup - 5.0
        -soup table - 5.0
        - -pre- observe - 5.0

        Second tweet:
        -Mr. Bingley - 5.0

        Third tweet:

        Fourth tweet:
        -table -post- - 5.0

        Fifth tweet:
        - -pre- marshmallow - 5.0

     */
    public static void testBigramFeaturesTFIDFThresholdOf1NoStopWords() throws IOException {
        //---------------
        //TRAINING
        //---------------
        //set up vars
        ArrayList<String[]> trainingTweets = TweetParser.getTweets("data/testData/5BigramExampleTweets.txt");
        ArrayList<String> trainingLabelSet = new ArrayList<String>();
        TweetVector[] trainingTweetVectors = new TweetVector[trainingTweets.size()];

        //train the model on the training data
        for (int i = 0; i < trainingTweets.size(); i++) {
            String[] tweet = trainingTweets.get(i);
            trainingTweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5], trainingLabelSet);
        }
        NGramModel bigramModel = new NGramModel(2, trainingTweetVectors, NGramModel.textName, "data/stopwords.txt", 1);


        //--------------
        //TESTING
        //--------------

        //feature var
        ArrayList<Hashtable<String, Double>> tfIdfFeatures = new ArrayList<Hashtable<String, Double>>();
        //annotator
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //get the test data into phrase form so tf features can be extracted
        ArrayList<String[]> testingTweets = TweetParser.getTweets("data/testData/5MoreBigramExampleTweets.txt");

        //get the tfs
        for (String[] tweet: testingTweets) {
            Annotation tweetDocument = new Annotation(readTweetsGetFeatures.process(tweet[4]));
            pipeline.annotate(tweetDocument);
            CoreLabel[][] tweetPhrases = readTweetsGetFeatures.getPhrases(tweetDocument);

            Hashtable<String, Double> tfIdfForTweet = bigramModel.getFeaturesForTweetTFIDF(tweetPhrases);
            tfIdfFeatures.add(tfIdfForTweet);
        }

        System.out.println("Testing TF-IDF bigram features with a document frequency threshold of 1 and no stop words...");
        try {
            //generate vars
            Hashtable<String, Double> firstTweet = tfIdfFeatures.get(0);
            Hashtable<String, Double> secondTweet = tfIdfFeatures.get(1);
            Hashtable<String, Double> thirdTweet = tfIdfFeatures.get(2);
            Hashtable<String, Double> fourthTweet = tfIdfFeatures.get(3);
            Hashtable<String, Double> fifthTweet = tfIdfFeatures.get(4);

            //check the tf-idf values
            //tweet 1
            if (firstTweet.get("observe chicken") != 2.5) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("table -post-") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("chicken soup") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("soup table") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("-pre- observe") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.size() != 5) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 2
            else if (secondTweet.get("Mr. Bingley") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.size() != 1) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 3
            else if (thirdTweet.size() != 0) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 4
            else if (fourthTweet.get("table -post-") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (fourthTweet.size() != 1) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 5
            else if (fifthTweet.get("-pre- marshmallow") != 5.0) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }
            else if (fifthTweet.size() != 1) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            else {
                System.out.println("PASSED.");
            }
        }
        catch (Exception e) {
            System.out.println("FAILED due to exception.");
            System.out.println(e.getMessage());
        }
    }

    public static void test6gramFeaturesTFIDFThresholdOf1NoStopWords() throws IOException {
        //---------------
        //TRAINING
        //---------------
        //set up vars
        ArrayList<String[]> trainingTweets = TweetParser.getTweets("data/testData/5_6gramExampleTweets.txt");
        ArrayList<String> trainingLabelSet = new ArrayList<String>();
        TweetVector[] trainingTweetVectors = new TweetVector[trainingTweets.size()];

        //train the model on the training data
        for (int i = 0; i < trainingTweets.size(); i++) {
            String[] tweet = trainingTweets.get(i);
            trainingTweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5], trainingLabelSet);
        }
        NGramModel bigramModel = new NGramModel(6, trainingTweetVectors, NGramModel.textName, "data/stopwords.txt", 1);


        //--------------
        //TESTING
        //--------------

        //feature var
        ArrayList<Hashtable<String, Double>> tfIdfFeatures = new ArrayList<Hashtable<String, Double>>();
        //annotator
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //get the test data into phrase form so tf features can be extracted
        ArrayList<String[]> testingTweets = TweetParser.getTweets("data/testData/5More6gramExampleTweets.txt");

        //get the tfs
        for (String[] tweet: testingTweets) {
            Annotation tweetDocument = new Annotation(readTweetsGetFeatures.process(tweet[4]));
            pipeline.annotate(tweetDocument);
            CoreLabel[][] tweetPhrases = readTweetsGetFeatures.getPhrases(tweetDocument);

            Hashtable<String, Double> tfIdfForTweet = bigramModel.getFeaturesForTweetTFIDF(tweetPhrases);
            tfIdfFeatures.add(tfIdfForTweet);
        }

        System.out.println("Testing TF-IDF 6-gram features with a document frequency threshold of 1 and no stop words...");
        try {
            //generate vars
            Hashtable<String, Double> firstTweet = tfIdfFeatures.get(0);
            Hashtable<String, Double> secondTweet = tfIdfFeatures.get(1);
            Hashtable<String, Double> thirdTweet = tfIdfFeatures.get(2);
            Hashtable<String, Double> fourthTweet = tfIdfFeatures.get(3);
            Hashtable<String, Double> fifthTweet = tfIdfFeatures.get(4);

            //check the tf-idf values
            //tweet 1
            if (firstTweet.get("-pre- -pre- -pre- -pre- -pre- potato") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("-pre- -pre- -pre- -pre- potato -post-") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("-pre- -pre- -pre- potato -post- -post-") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("-pre- -pre- potato -post- -post- -post-") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("-pre- potato -post- -post- -post- -post-") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("potato -post- -post- -post- -post- -post-") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.size() != 6) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 2
            else if (secondTweet.get("-pre- -pre- -pre- -pre- -pre- moose") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("-pre- -pre- -pre- -pre- moose sheep") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("-pre- -pre- -pre- moose sheep cat") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("-pre- -pre- moose sheep cat dog") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("-pre- moose sheep cat dog goat") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("moose sheep cat dog goat pig") != 2.5) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("sheep cat dog goat pig -post-") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("cat dog goat pig -post- -post-") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("dog goat pig -post- -post- -post-") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("goat pig -post- -post- -post- -post-") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("pig -post- -post- -post- -post- -post-") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.size() != 11) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 3
            else if (thirdTweet.get("-pre- -pre- -pre- -pre- -pre- dance") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (thirdTweet.size() != 1) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 4
            else if (fourthTweet.size() != 0) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 5
            else if (fifthTweet.size() != 0) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            else {
                System.out.println("PASSED.");
            }
        }
        catch (Exception e) {
            System.out.println("FAILED due to exception.");
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        //tests for the event classifier

        //tests for the N-gram model (all words measure by it are assumed to be lowercase unless they are proper nouns,
        //in which case the first character (and only the first character) is uppercase
         //test the unigram idf method
        testUnigramFeaturesIDFThresholdOf1WithStopWords();

         //test the combination of the unigram tf and idf methods
        testUnigramFeaturesTFIDFThresholdOf1WithStopWords();

        //test a tf-idf unigram model with a higher frequency threshold than 1
        testUnigramFeaturesTFIDFThresholdOf2WithStopWords();

         //test a tf-idf unigram model without stopwords
        testUnigramFeaturesTFIDFThresholdOf1NoStopWords();

         //test a tf-idf bigram model without stopwords
        testBigramFeaturesTFIDFThresholdOf1NoStopWords();

         //test 6-grams
        test6gramFeaturesTFIDFThresholdOf1NoStopWords();


         //test phrase template model

    }
}

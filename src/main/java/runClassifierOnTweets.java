import java.io.*;
import java.util.*;
import cc.mallet.types.InstanceList;

public class runClassifierOnTweets {
	
	/*TODO:
	 *     Check documentation for AUC metric
	 *     CSV out the labelins and plot in spreadsheet
	 * */

    public static long startRunTime;
    public static long endRunTime;

    /*
        Trains a classifier of the specified type on the given training data, saves it
     */
    public static void trainAndSaveClassifier (ArrayList<String[]> trainingTweets, String pathToClassifier, String classifierType) throws IOException, ClassNotFoundException {
        TweetVector[] tweetVectors = readTweetsGetFeatures.getVectorModelsFromTweets(trainingTweets, classifierType);
        MaxEntClassification classifier = new MaxEntClassification(pathToClassifier);

        //construct the instance list
        for (int i = 0; i < tweetVectors.length; i++) {
            //add the current tweet
            TweetVector currentTweet = tweetVectors[i];
            classifier.addToInstanceList(currentTweet.getFeatures(), currentTweet.getName(), currentTweet.getLabel());
        }
        //train
        classifier.trainClassifier(classifier.instances);
        classifier.saveClassifier(new File(pathToClassifier));
    }

    /*
        Trains a classifier of the specified type on the given training data, then tests on the given training data.
        Saves the classifier to the file at the given path
    */
    public static void trainAndTestClassifier(ArrayList<String[]> trainingAndTestTweets, String pathToClassifier, String classifierType, String pathToResultsFile) throws IOException, ClassNotFoundException {
        TweetVector[] tweetVectors = readTweetsGetFeatures.getVectorModelsFromTweets(trainingAndTestTweets, classifierType);

        //make the classifier
        MaxEntClassification classifier = new MaxEntClassification(pathToClassifier);
        //get instances for the classifier
        for (int i = 0; i < tweetVectors.length; i++) {
            //add the current tweet
            TweetVector currentTweet = tweetVectors[i];
            classifier.addToInstanceList(currentTweet.getFeatures(), currentTweet.getName(), currentTweet.getLabel());
        }
       //train and test
        /*
         * Cross validation portion.
         * */
        //classifier.crossValidate(5, pathToResultsFile);

        //non-cross-validation test
        //classifier.runNTrials(5, pathToResultsFile);

        //non-cross-validation test for "person" class of HvN with varying confidence intervals
        classifier.runNTrials(5, pathToResultsFile, "person", 0.8, "organization");
    }

    /*
        Given a set of test tweets and a path to a pre-made classifier, test the accuracy, precision, recall, and
        F1 of the classifier for the test tweets. Classify a specific desired class with a given confidence threshold.
        Print out the results.
     */
    public static void testClassifier(String classifierType, ArrayList<String[]> testTweets, String pathToClassifier, String pathToResultsFile, String desiredClass, double confThreshold, String altClass)
    throws IOException, ClassNotFoundException {
        //get features for the test tweets, then save said tweets as test instances of a dummy classifier
        TweetVector[] tweetVectors = readTweetsGetFeatures.getVectorModelsFromTweets(testTweets, classifierType);
        MaxEntClassification dummy = new MaxEntClassification("");
        //get instances for the classifier
        for (int i = 0; i < tweetVectors.length; i++) {
            //add the current tweet
            TweetVector currentTweet = tweetVectors[i];
            dummy.addToInstanceList(currentTweet.getFeatures(), currentTweet.getName(), currentTweet.getLabel());
        }

        //load the classifier
        MaxEntClassification classifier = new MaxEntClassification(pathToClassifier);

        //evaluate the instances and print
        classifier.writeEvaluatedToFile(classifier.evaluateWithConfidenceLevel(dummy.instances, desiredClass, confThreshold, altClass), 1, pathToResultsFile, false);
    }

    /*
        Loads a classifier from a file and runs it on a set of tweets. Returns the ones that are labeled with the desired
        class, according to the specified confidence threshold
     */
    public static ArrayList<String[]> runClassifierAndGetTweetsByLabel(ArrayList<String[]> testTweets, String pathToClassifier, String classifierType, String desiredClass, double confidenceThreshold, String altClass)
    throws IOException, ClassNotFoundException {
        //classifier and output variables
        MaxEntClassification classifier = new MaxEntClassification(pathToClassifier);
        ArrayList<String[]> outputTweets = new ArrayList<String[]>();

        //get the tweets to be labeled, generate features for them, and store them as Instances in a dummy classifier
        TweetVector[] tweetVectors = readTweetsGetFeatures.getVectorModelsFromTweets(testTweets, classifierType);
        MaxEntClassification dummy = new MaxEntClassification("");
        for (TweetVector tweetVector: tweetVectors) {
            dummy.addToInstanceList(tweetVector.getFeatures(), tweetVector.getName(), tweetVector.getLabel());
        }

        //run the instances through the classifier by confidence level, add the ones labeled as the desired class
        //to the output
        InstanceList instances = dummy.instances;
        for (int i = 0; i < instances.size(); i++) {
            if (classifier.getLabelConfThresholdForDesiredClass(instances.get(i), desiredClass, confidenceThreshold, altClass).equals(desiredClass)) {
                outputTweets.add(testTweets.get(i));
            }
        }

        return outputTweets;
    }



    /*
        From a path to a file containing tweet ids, tweet labels, and tweet texts separated by null characters,
        construct and train a classifier, then use it to classify any given tweet data
        Args:
        0 - path to a file containing HvN tweets, one in each line (with its id, label, and text separated by double spaces)
        1 - path to a file where the human vs. non-human classifier will be stored
        2 - path to a file containing EvNE tweets
        3 - path to a file where the <relevant life event> vs. <non-relevant life event> classifier will be stored
        4 - path to a file containing SvO tweets
        5 - path to a file where the self vs. other classifier will be stored
        6 - path to a file where the results will be stored
    */
    public static void main (String[] args) throws IOException, ClassNotFoundException {

        //String dataPathHvN = "C:\\Users\\AshMo\\Documents\\IR Lab-Classification\\tweet_person_vs_organization.csv";
        //String classifierPathHvN = "C:\\Users\\AshMo\\Documents\\IR Lab-Classification\\HvNClassifierFile.txt";
        String dataPathEvN = "C:\\Users\\AshMo\\Documents\\IR Lab-Classification\\twitterLifeEventExtracted1000.csv";
        String classifierPathEvN = "C:\\Users\\AshMo\\Documents\\IR Lab-Classification\\EvNClassifierFile.txt";
        //String[] labels = {"Human", "Not Human"};
        startRunTime = System.currentTimeMillis();

        //ArrayList<String[]> randomTweets = TweetParser.getUnlabeledTweetEntitiesAndLabel("data/tweets/5kRandomTweets.txt", 2, true, "none_of_the_above");

        //Initialize hash sets once at run time
        TextFeatures.initializeHashSets();

        //random tweet texts for EvN dataset
//        ArrayList<String[]> randomTweets = TweetParser.getTweets("data/tweets/random_tweets_100k.csv");
//        TweetParser.writeTweetEntitiesToFile(randomTweets, "data/tweets/tweet_event_plus_100k_random.csv", true);

        //get the training tweets
        //ArrayList<String[]> HvNTweets = TweetParser.getTweets(args[0]);
        //trainAndTestClassifier(HvNTweets, args[1], "HumanVsNonHuman", args[6]);
        //ArrayList<String[]> EvNETweets = TweetParser.getTweets(args[2]);
        //trainAndTestClassifier(EvNETweets, args[3], "EventVsNonEvent", args[6]);
        //ArrayList<String[]> SvOTweets = TweetParser.getTweets(args[4]);
        //trainAndTestClassifier(trainingTweets, args[5], "SelfVsOther", args[6]);

        //train and run the classifier, get the tweets, write them to a file
        //trainAndSaveClassifier(HvNTweets, args[1], "HumanVsNonHuman");
        ArrayList<String[]> testTweets = TweetParser.getTweets(args[7]);
        testClassifier("HumanVsNonHuman", testTweets, "HumanVsNonHumanClassifier.txt", "data/testResults.txt", "person", 0.8, "organization");
        ArrayList<String[]> outputtedTweets =
                runClassifierAndGetTweetsByLabel(testTweets, args[1], "HumanVsNonHuman", "person", 0.8, "organization");
        //print out the tweets
        util.printAllFieldsOneLinePerEntry(outputtedTweets, args[8]);
    }
}
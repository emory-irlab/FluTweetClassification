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
        Trains a classifier of the specified type on the given training data
     */
    public static void trainClassifier (ArrayList<String[]> trainingTweets, String pathToClassifier, String classifierType) throws IOException, ClassNotFoundException {
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
    }

    /*
        Trains a classifier of the specified type on the given training data, then tests on the given training data.
        Saves the classifier to the file at the given path
    */
    public static void testClassifier (ArrayList<String[]> trainingTweets, String pathToClassifier, String classifierType, String pathToResultsFile) throws IOException, ClassNotFoundException {

        TweetVector[] tweetVectors = readTweetsGetFeatures.getVectorModelsFromTweets(trainingTweets, classifierType);

        //make the classifier
        MaxEntClassification classifier = new MaxEntClassification(pathToClassifier);
        //train the classifier
        for (int i = 0; i < tweetVectors.length; i++) {
            //add the current tweet
            TweetVector currentTweet = tweetVectors[i];
            classifier.addToInstanceList(currentTweet.getFeatures(), currentTweet.getName(), currentTweet.getLabel());
        }
        /*
         * Cross validation portion.
         * */
        //classifier.crossValidate(5, pathToResultsFile);

        //non-cross-validation test
        //classifier.runNTrials(5, pathToResultsFile);

        //non-cross-validation test for "person" class of HvN with varying confidence intervals
        classifier.runNTrials(5, pathToResultsFile, 0.8);
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
        ArrayList<String[]> HvNTweets = TweetParser.getTweets(args[0]);
        //testClassifier(HvNTweets, args[1], "HumanVsNonHuman", args[6]);
        //ArrayList<String[]> EvNETweets = TweetParser.getTweets(args[2]);
        //testClassifier(EvNETweets, args[3], "EventVsNonEvent", args[6]);
        //ArrayList<String[]> SvOTweets = TweetParser.getTweets(args[4]);
        //testClassifier(trainingTweets, args[5], "SelfVsOther", args[6]);

        //train and run the classifier, get the tweets, write them to a file
        trainClassifier(HvNTweets, args[1], "HumanVsNonHuman");
        ArrayList<String[]> testTweets = TweetParser.getTweets(args[7]);
        ArrayList<String[]> outputtedTweets =
                runClassifierAndGetTweetsByLabel(testTweets, args[1], "HumanVsNonHuman", "person", 0.8, "organization");
    }
}
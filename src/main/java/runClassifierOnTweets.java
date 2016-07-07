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
        Trains a classifier of the specified type on the given training data, then tests on the given training data.
        Saves the classifier to the file at the given path
    */
    public static void runClassifier (ArrayList<String[]> trainingTweets, String path, String classifierType) throws IOException, ClassNotFoundException {

        TweetVector[] tweetVectors = readTweetsGetFeatures.getVectorModelsFromTweets(trainingTweets, classifierType);

        //make the classifier
        MaxEntClassification classifier = new MaxEntClassification(path);
        //train the classifier
        for (int i = 0; i < tweetVectors.length; i++) {
            //add the current tweet
            TweetVector currentTweet = tweetVectors[i];
            classifier.addToInstanceList(currentTweet.getFeatures(), currentTweet.getName(), currentTweet.getLabel());
        }
        /*
         * Cross validation portion.
         * */
        classifier.crossValidate(5);

        //regular run
        /*
        InstanceList testInstances = classifier.split(classifier.instances);
        classifier.trainClassifier(classifier.instances);
        classifier.saveClassifier(classifier.classifierFile);

        classifier.clearInstances();
        Hashtable<String, Hashtable<String, Double>> results = classifier.evaluate(testInstances);
        classifier.printEvaluated(results, 1);
        */
        //classifier.evaluateWithConfidenceThreshold(testInstances, .9);
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
        /*
        ArrayList<String[]> randomTweets = TweetParser.getTweets("data/tweets/random_tweets_20k.csv");
        TweetParser.writeTweetEntitiesToFile(randomTweets, "data/tweets/tweet_event_plus_20k_random.csv", true);
    	*/

        //get the training tweets
        //ArrayList<String[]> HvNTweets = TweetParser.getTweets(args[0]);
        //runClassifier(HvNTweets, args[1], "HumanVsNonHuman");
        ArrayList<String[]> EvNETweets = TweetParser.getTweets(args[2]);
        runClassifier(EvNETweets, args[3], "EventVsNonEvent");
        //ArrayList<String[]> SvOTweets = TweetParser.getTweets(args[4]);
        //runClassifier(trainingTweets, args[5], "SelfVsOther");

    }
}
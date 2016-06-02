import java.io.*;
import java.util.*;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

/**
 * Created by Alec Wolyniec on 4/26/16.
 */
public class runClassifierOnTweets {

    /*
        Trains a classifier of the specified type on the given training data, then tests on the given training data.
        Saves the classifier to the file at the given path
    */
    public static void runClassifier (ArrayList<String[]> trainingTweets, ArrayList<String[]> testTweets, String path, String classifierType) throws IOException, ClassNotFoundException {
        TweetVector[] tweetVectors = readTweetsGetFeatures.getVectorModelsFromTweets(trainingTweets, classifierType);

        //make the classifier
        SelfVsOtherClassifier classifier = new SelfVsOtherClassifier(path);
        //train the classifier
        for (int i = 0; i < tweetVectors.length; i++) {
            //add the current tweet
            TweetVector currentTweet = tweetVectors[i];
            classifier.addToInstanceList(currentTweet.getFeatures(), currentTweet.getName(), currentTweet.getLabel());
        }
        classifier.trainClassifier(classifier.instances);
        classifier.saveClassifier(classifier.classifierFile);

        //for testing purposes
        /*
         //observe all data in the InstanceList
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("BABABABABA");
        for (Instance in: classifier.instances) {
            Object data = in.getData();
            //if (data instanceof Hashtable) {
                //util.printStringFeaturesIntValuesFromHashtable((Hashtable<String, Integer>)data);
            //}
            Object label = in.getSource();
            System.out.println("Tweet label: "+label); //listed as null here
            Object name = in.getName();
            System.out.println("Tweet name: "+name);
            System.out.println();
        }
        */

        classifier.clearInstances();

        //get the test tweets
        TweetVector[] testingInstances = readTweetsGetFeatures.getVectorModelsFromTweets(testTweets, classifierType);
        for (int i = 0; i < tweetVectors.length; i++) {
            TweetVector currentTweet = testingInstances[i];
            classifier.addToInstanceList(currentTweet.getFeatures(), currentTweet.getName(), currentTweet.getLabel());
        }
        classifier.evaluate(classifier.instances);
    }

    /*
        From a path to a file containing tweet ids, tweet labels, and tweet texts separated by null characters,
        construct and train a classifier, then use it to classify any given tweet data

        Args:
        0 - path to a file containing training tweets, one in each line (with its id, label, and text separated by double spaces)
        1 - path to a file containing test tweets, with the same format as the tweets in args[0]
        2 - path to a file where the human vs. non-human classifier will be stored
        3 - path to a file where the <relevant life event> vs. <non-relevant life event> classifier will be stored
        4 - path to a file where the self vs. other classifier will be stored
    */
    public static void main (String[] args) throws IOException, ClassNotFoundException {
        //get the training tweets
        ArrayList<String[]> trainingTweets = readTweetsGetFeatures.getTweets(args[0]);
        ArrayList<String[]> testTweets = readTweetsGetFeatures.getTweets(args[1]);

        runClassifier(trainingTweets, testTweets, args[2], "HumanVsNonHuman");
        //runClassifier(trainingTweets, testTweets, args[3], "EventVsNonEvent");
        //runClassifier(trainingTweets, testTweets, args[4], "SelfVsOther");

    }
}

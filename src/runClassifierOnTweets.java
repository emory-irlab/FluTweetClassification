import java.io.*;
import java.util.*;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import edu.berkeley.nlp.classify.MaximumEntropyClassifier;

/**
 * Created by Alec Wolyniec on 4/26/16.
 */
public class runClassifierOnTweets {

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
        InstanceList testInstances = classifier.split(classifier.instances);
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
        classifier.evaluate(testInstances);
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
        //get the training tweets
        ArrayList<String[]> HvNTweets = readTweetsGetFeatures.getTweets(args[0]);
        runClassifier(HvNTweets, args[1], "HumanVsNonHuman");
        //ArrayList<String[]> EvNETweets = readTweetsGetFeatures.getTweets(args[2]);
        //runClassifier(trainingTweets, args[3], "EventVsNonEvent");
        //ArrayList<String[]> SvOTweets = readTweetsGetFeatures.getTweets(args[4]);
        //runClassifier(trainingTweets, args[5], "SelfVsOther");

    }
}

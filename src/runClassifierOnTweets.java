import java.io.*;
import java.util.*;

/**
 * Created by Alec Wolyniec on 4/26/16.
 */
public class runClassifierOnTweets {
    /*
        From a path to a file containing tweet ids, tweet labels, and tweet texts separated by null characters,
        construct and train a classifier, then use it to classify any given tweet data

        Args:
        0 - path to a file containing training tweets, one in each line (with its id, label, and text separated by null characters)
        1 - path to a file where the classifier will be stored
        2 - path to a file containing tweets to be classified, with the same format as the tweets in args[0]
     */
    public static void main (String[] args) throws IOException, ClassNotFoundException {
        //get the training tweets
        ArrayList<String[]> tweets = readTweetsGetFeatures.getTweets(args[0]);
        TweetVector[] tweetVectors = readTweetsGetFeatures.getVectorModelsFromTweets(tweets);

        /*
        //make the classifier
        SelfVsOtherClassifier classifier = new SelfVsOtherClassifier(args[1]);
        //train the classifier 10 tweets at a time
        int tweetCounter = 0;
        for (int i = 0; i < tweetVectors.length; i++) {
            //add the current tweet
            TweetVector currentTweet = tweetVectors[i];
            classifier.addToInstanceList(currentTweet.getFeatures(), currentTweet.getName(), currentTweet.getLabel());
            tweetCounter++;
            if (tweetCounter == 10) {
                tweetCounter = 0;
                classifier.trainClassifier(classifier.instances);
            }
        }
        */
        //get the tweets to be classified
        //train
    }
}

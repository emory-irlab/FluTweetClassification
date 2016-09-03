import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

import edu.stanford.nlp.util.Pair;

import cc.mallet.types.ArrayListSequence;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Instance;
import cc.mallet.types.Labeling;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVPrinter;

public class runClassifierOnTweets {
	
	/*TODO:
	 *     Check documentation for AUC metric
	 *     CSV out the labelins and plot in spreadsheet
	 * */

    public static long startRunTime;
    public static long endRunTime;
    //static final int nCores = Runtime.getRuntime().availableProcessors() - 1;
    static final int nCores = 12;
    //private static int nCores = 1;

    //vectorizes and classifies a tweet using the given classifier, and returns its label
    public static Pair<String, Double> classify (String[] tweet, String classifierType, MaxEntClassification classifier) throws InterruptedException, IOException, ClassNotFoundException {
        //initialize the vector with the profile pic link, username, name, description, and tweet parameters set
        TweetVector tweetVector = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4]);
        //get features
        tweetVector = readTweetsGetFeatures.getVectorModelForTweet(tweetVector, classifierType, nCores);

        //make an instance out of the tweetVector
        classifier.addToInstanceList(tweetVector.getFeatures(), tweetVector.getName(), tweetVector.getLabel());
        Instance tweetInstance = classifier.instances.get(0);
        classifier.clearInstances();

        //classify, get the label and confidence
        return classifier.getLabelAndConfidence(tweetInstance, "null_class");
    }

    /*
        From a path to a file containing tweet ids, tweet labels, and tweet texts separated by null characters,
        construct and train a classifier, then use it to classify any given tweet data
        Args:
        0 - path to a file containing unlabeled tweets to be classified
    */
    public static void main (String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        //for test
        //System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("data/StdOutput.txt"))));

/*
        //String dataPathHvN = "C:\\Users\\AshMo\\Documents\\IR Lab-Classification\\tweet_person_vs_organization.csv";
        //String classifierPathHvN = "C:\\Users\\AshMo\\Documents\\IR Lab-Classification\\HvNClassifierFile.txt";
        String dataPathEvN = "C:\\Users\\AshMo\\Documents\\IR Lab-Classification\\twitterLifeEventExtracted1000.csv";
        String classifierPathEvN = "C:\\Users\\AshMo\\Documents\\IR Lab-Classification\\EvNClassifierFile.txt";
        //String[] labels = {"Human", "Not Human"};
        startRunTime = System.currentTimeMillis();
*/

        //begin with tweets to be classified
        String pathToUnlabeledTweets = args[0];
        //initialize the classifiers
        MaxEntClassification humanNonHuman = new MaxEntClassification("classifiers/"+readTweetsGetFeatures.humanNonHumanClassifierName+".txt", nCores);
        MaxEntClassification event = new MaxEntClassification("classifiers/"+readTweetsGetFeatures.eventClassifierName+".txt", nCores);
        MaxEntClassification selfOther = new MaxEntClassification("classifiers/"+readTweetsGetFeatures.selfOtherClassifierName+".txt", nCores);

        //get tweets
        ArrayList<String[]> tweetsToRun = TweetParser.getTweets(pathToUnlabeledTweets);

        /*TEMP*/int counter = 0;
        /*TEMP*/long startTime = System.currentTimeMillis();
        //classify each tweet and collect if it is labeled with an event
        for (String[] tweet: tweetsToRun) {
        /*TEMP*/counter++;

            String[] labels = new String[3];
            double[] confidences = new double[3];

            //put it through the human-nonhuman classifier (confidence level: 0.8)
            Pair<String, Double> humanResults = classify(tweet, readTweetsGetFeatures.humanNonHumanClassifierName, humanNonHuman);
            labels[0] = humanResults.first();
            confidences[0] = humanResults.second();

            //If it's human,
            //put it through the event classifier (confidence level: ?)
            if (labels[0].equals("person")) {
                Pair<String, Double> eventResults = classify(tweet, readTweetsGetFeatures.eventClassifierName, event);
                labels[1] = eventResults.first();
                confidences[1] = eventResults.second();

                //If it receives an event label,
                if (!labels[1].equals("null_class") && !labels[1].equals("none_of_the_above")) {
                    //Put it through the self-other classifier (confidence level: ?)
                    Pair<String, Double> selfResults = classify(tweet, readTweetsGetFeatures.selfOtherClassifierName, selfOther);
                    labels[2] = selfResults.first();
                    confidences[2] = selfResults.second();

                    //if it receives a self or other label,
                    if (labels[2].equals("self") || labels[2].equals("other")) {
                        //append it to a file based on self-other label and event (initialize this file if it
                        //does not yet exist)
                        File putativeFile = new File("classifiedTweets/"+labels[1]+"-"+labels[2]+".csv");
                        BufferedWriter writer = new BufferedWriter(new FileWriter(putativeFile, true));
                        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.RFC4180);

                        if (!putativeFile.exists()) {
                            printer.print("link to profile pic");
                            printer.print("username");
                            printer.print("name");
                            printer.print("profile description");
                            printer.print("tweet");
                            printer.print("human or organization");
                            printer.print("confidence");
                            printer.print("event");
                            printer.print("confidence");
                            printer.print("event happened to self or other");
                            printer.print("confidence");
                        }

                        //new line
                        printer.println();
                        //write out all tweet fields except for "label" (this is a feature used for training/testing only)
                        for (int i = 0; i < tweet.length; i++) {
                            String field = tweet[i];
                            //"label" is at index 5
                            if (i != 5) {
                                printer.print(field);
                            }
                        }
                        //write out all labels
                        for (int i = 0; i < labels.length; i++) {
                            printer.print(labels[i]);
                            printer.print(confidences[i]);
                        }
                        printer.close();
                    }
                }
            }
        }
        System.out.println("Total time to get "+counter+" tweets: "+((double)System.currentTimeMillis() - startTime)/1000+" seconds");
    }
}

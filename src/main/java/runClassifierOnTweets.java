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
    //private static int nCores = Runtime.getRuntime().availableProcessors();
    private static int nCores = 1;

    /*
        Loads a classifier from a file and runs it on a set of tweets. Returns the ones that are labeled with the desired
        class, according to the specified confidence threshold
     */
    /*
    public static ArrayList<String[]> runClassifierAndGetTweetsByLabel(ArrayList<String[]> testTweets, String pathToClassifier, String classifierType, String desiredClass, double confidenceThreshold)
    throws IOException, ClassNotFoundException, InterruptedException {
        //classifier and output variables
        MaxEntClassification classifier = new MaxEntClassification(pathToClassifier, nCores);
        ArrayList<String[]> outputTweets = new ArrayList<String[]>();

        //get the tweets to be labeled, generate features for them, and store them as Instances in a dummy classifier
        TweetVector[] tweetVectors = readTweetsGetFeatures.getVectorModelsFromTweets(testTweets, classifierType);
        MaxEntClassification dummy = new MaxEntClassification("", nCores);
        for (TweetVector tweetVector: tweetVectors) {
            dummy.addToInstanceList(tweetVector.getFeatures(), tweetVector.getName(), tweetVector.getLabel());
        }

        //run the instances through the classifier by confidence level, add the ones labeled as the desired class
        //to the output
        InstanceList instances = dummy.instances;
        for (int i = 0; i < instances.size(); i++) {
            if (classifier.getLabelConfThresholdForDesiredClass(instances.get(i), desiredClass, confidenceThreshold).equals(desiredClass)) {
                outputTweets.add(testTweets.get(i));
            }
        }

        return outputTweets;
    }
    */

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
    public static void main (String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        //for test
        //System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("data/StdOutput.txt"))));

        //String dataPathHvN = "C:\\Users\\AshMo\\Documents\\IR Lab-Classification\\tweet_person_vs_organization.csv";
        //String classifierPathHvN = "C:\\Users\\AshMo\\Documents\\IR Lab-Classification\\HvNClassifierFile.txt";
        String dataPathEvN = "C:\\Users\\AshMo\\Documents\\IR Lab-Classification\\twitterLifeEventExtracted1000.csv";
        String classifierPathEvN = "C:\\Users\\AshMo\\Documents\\IR Lab-Classification\\EvNClassifierFile.txt";
        //String[] labels = {"Human", "Not Human"};
        startRunTime = System.currentTimeMillis();

        //ArrayList<String[]> randomTweets = TweetParser.getUnlabeledTweetEntitiesAndLabel("data/tweets/5kRandomTweets.txt", 2, true, "none_of_the_above");

        //Initialize hash sets once at run time
        //TextFeatures.initializeHashSets();

        //random tweet texts for EvN dataset
        /*
        ArrayList<String[]> randomTweets = TweetParser.getTweets("data/tweets/random_tweets_100k.csv");
        TweetParser.writeTweetEntitiesToFile(randomTweets, "data/tweets/tweet_event_advanced_plus_50k_random.csv", true);
        */

        String pathToHvN = args[1];
        String pathToEvN = args[3];
        String pathToSvO = args[5];

        String pathToTestResults = args[6];
        String pathToTweetsToBeClassified = args[7];
        String pathToPrintClassifiedTweets = args[8];

        //get the training tweets
        String pathToHvNTweets = args[0];
        String pathToEvNTweets = args[2];
        String pathToSvOTweets = args[4];
        //ArrayList<String[]> HvNTweets = TweetParser.getTweets(args[0]);
        //ArrayList<String[]> EvNTweets = TweetParser.getTweets(args[2]);
        //ArrayList<String[]> SvOTweets = TweetParser.getTweets(args[4]);

        //get vectors from the training tweets and add them to the classifier's instance list
        MaxEntClassification classifier = new MaxEntClassification(pathToHvN, nCores);
        TweetVector[] trainingTweetVectors = readTweetsGetFeatures.getVectorModelsFromTweets(pathToHvNTweets, "HumanVsNonHuman", nCores);

        //VARIOUS OPTIONS FOR RUNNING THE CLASSIFIER

        //train + test
         //cross-validation test
        classifier.crossValidate(5, trainingTweetVectors, pathToTestResults, 10000); //cross-validation must have at least 2 folds
        //classifier.crossValidate(5, trainingTweetVectors, pathToTestResults, "null_class", 0.5);
        //classifier.crossValidate(5, trainingTweetVectors, pathToTestResults, "null_class", new double[] {0.5, 0.6, 0.7, 0.8, 0.9});
         //split multiple times test
        //classifier.runNSplits(5, trainingTweetVectors, pathToTestResults);

        //just train
        //classifier.makeInstancesAndTrain(pathToEvNTweets, "EventVsNonEvent");

        //save
        //classifier.saveClassifier(new File(pathToEvN));

        //obtain tweets to be classified
        //ArrayList<String[]> toBeClassified = TweetParser.getTweets(pathToTweetsToBeClassified);

        //run the tweets to be classified
        //ArrayList<String[]> outputtedTweets =
                //runClassifierAndGetTweetsByLabel(toBeClassified, pathToHvN, "HumanVsNonHuman", "person", 0.8, "organization");
        //print out the classified tweets
        //util.printAllFieldsOneLinePerEntry(outputtedTweets, pathToPrintClassifiedTweets);

    }
}

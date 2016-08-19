import java.io.*;
import java.util.*;
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
    static final int nCores = Runtime.getRuntime().availableProcessors() - 1;
    //private static int nCores = 1;

    //vectorizes and classifies a tweet using the given classifier, and returns its label
    public static String classify (ArrayList<String> tweet, String classifierType, MaxEntClassification classifier, double confThreshold) throws InterruptedException, IOException, ClassNotFoundException {
        //initialize the vector with the profile pic link, username, name, description, and tweet parameters set
        TweetVector tweetVector = new TweetVector(tweet.get(0), tweet.get(1), tweet.get(2), tweet.get(3), tweet.get(4), "", null);
        //get features
        tweetVector = readTweetsGetFeatures.getVectorModelForTweet(tweetVector, classifierType, nCores);

        //make an instance out of the tweetVector
        classifier.addToInstanceList(tweetVector.getFeatures(), tweetVector.getName(), tweetVector.getLabel());
        Instance tweetInstance = classifier.instances.get(0);
        classifier.clearInstances();

        //classify and get the label
        String label = classifier.getLabelConfThresholdForDesiredClass(tweetInstance, "null_class", confThreshold);
        return label;
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

        BufferedReader tweetReader = new BufferedReader(new FileReader(new File(pathToUnlabeledTweets)));
        CSVParser tweetCSV = new CSVParser(tweetReader, CSVFormat.RFC4180); //could just replace this section with TweetParser.getTweets
        List<CSVRecord> records = tweetCSV.getRecords();
        for (CSVRecord tweet: records) {
            //for each tweet in the data
            if (tweet.size() >= 5) {
                ArrayList<String> tweetStrings = new ArrayList<String>();
                for (int i = 0; i < tweet.size(); i++) {
                    tweetStrings.add(tweet.get(i));
                }

                String[] labels = new String[3];

                //put it through the human-nonhuman classifier (confidence level: 0.8)
                labels[0] = classify(tweetStrings, readTweetsGetFeatures.humanNonHumanClassifierName, humanNonHuman, 0.8);

                //If it's human,
                //put it through the event classifier (confidence level: ?)
                if (labels[0].equals("person")) {
                    labels[1] = classify(tweetStrings, readTweetsGetFeatures.eventClassifierName, event, 0.0);

                    //If it receives an event label,
                    if (!labels[1].equals("null_class") && !labels[1].equals("none_of_the_above")) {
                        //Put it through the self-other classifier (confidence level: ?)
                        labels[2] = classify(tweetStrings, readTweetsGetFeatures.selfOtherClassifierName, selfOther, 0.9);

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
                                printer.print("event");
                                printer.print("event happened to self or other");
                            }

                            //new line
                            printer.println();
                            //write out all tweet fields
                            for (String field: tweetStrings) {
                                printer.print(field);
                            }
                            //write out all labels
                            for (String label: labels) {
                                printer.print(label);
                            }
                        }
                    }
                }

            }
        }
    }
}

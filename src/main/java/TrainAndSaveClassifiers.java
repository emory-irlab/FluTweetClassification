import cc.mallet.classify.MaxEnt;
import org.apache.commons.math.stat.descriptive.rank.Max;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;

/**
 * Created by Alec Wolyniec on 8/17/16.
 *
 * Trains and saves the classifiers to be used by the program
 */
public class TrainAndSaveClassifiers {

    public static void trainAndSaveClassifier(String pathToClassifierFile, String classifierType, String pathToTrainingData/*, String pathToTestData*/) throws ClassNotFoundException, IOException, InterruptedException {
        MaxEntClassification classifier = new MaxEntClassification(pathToClassifierFile, runClassifierOnTweets.nCores);

        //vectorize the training data
        TweetVector[] vectorizedTrainingData = readTweetsGetFeatures.getVectorModelsFromTweets(pathToTrainingData, classifierType, runClassifierOnTweets.nCores);
        classifier.addToInstanceList(vectorizedTrainingData);
        classifier.trainClassifier(classifier.instances);
        classifier.clearInstances();
/* // temporary test
        ArrayList<String[]> tweets = TweetParser.getTweets(pathToTestData);
        for (String[] tweet: tweets) {
            ArrayList<String> tweetArray = new ArrayList<String>();
            int i = 0;
            for (String field: tweet) {
                if (i != 5) {
                    tweetArray.add(field);
                }
                else {
                    tweetArray.add("self");
                }
                i++;
            }

            String expLabel = runClassifierOnTweets.classify(tweetArray, readTweetsGetFeatures.selfOtherClassifierName, classifier, 0.0);
            System.out.println(tweet[4]+": "+expLabel);

        }
*/

        File classifierFile = new File(pathToClassifierFile);
        classifier.saveClassifier(classifierFile);
    }

    /*
        Arguments should appear as follows:

        0 - training data for classifier
     */
    public static void main (String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        //get rid of any old classifiers
        //get rid of any old n-gram model "accepted n-grams" files
        util.emptyDirectory(new File("classifiers"));
        util.emptyDirectory(new File("nGramModels"));


        //human-non-human classifier
        String pathToHvNClassifierFile = "classifiers/"+readTweetsGetFeatures.humanNonHumanClassifierName+".txt";
        File HvNClassifierFile = new File(pathToHvNClassifierFile);
        trainAndSaveClassifier(pathToHvNClassifierFile, readTweetsGetFeatures.humanNonHumanClassifierName, args[0]);

/*
        //event classifier
        String pathToEventClassifierFile = "classifiers/"+readTweetsGetFeatures.eventClassifierName+".txt";
        File eventClassifierFile = new File(pathToEventClassifierFile);
        trainAndSaveClassifier(pathToEventClassifierFile, readTweetsGetFeatures.eventClassifierName, args[0]);
*/
/*
        //self-other classifier
        String pathToSvOClassifierFile = "classifiers/"+readTweetsGetFeatures.selfOtherClassifierName+".txt";
        File SvOClassifierFile = new File(pathToSvOClassifierFile);
        trainAndSaveClassifier(pathToSvOClassifierFile, readTweetsGetFeatures.selfOtherClassifierName, args[0]/*, args[3]*);
*/
    }

}

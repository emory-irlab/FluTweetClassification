import cc.mallet.classify.MaxEnt;
import org.apache.commons.math.stat.descriptive.rank.Max;
import java.io.IOException;
import java.io.File;

/**
 * Created by Alec Wolyniec on 8/17/16.
 *
 * Trains and saves the classifiers to be used by the program
 */
public class TrainAndSaveClassifiers {

    public static void trainAndSaveClassifier(String pathToClassifierFile, String classifierType, String pathToTrainingData) throws ClassNotFoundException, IOException, InterruptedException {
        MaxEntClassification classifier = new MaxEntClassification(pathToClassifierFile, runClassifierOnTweets.nCores);

        //vectorize the training data
        TweetVector[] vectorizedTrainingData = readTweetsGetFeatures.getVectorModelsFromTweets(pathToTrainingData, classifierType, runClassifierOnTweets.nCores);
        classifier.addToInstanceList(vectorizedTrainingData);
        classifier.trainClassifier(classifier.instances);

        File classifierFile = new File(pathToClassifierFile);
        classifier.saveClassifier(classifierFile);
    }

    /*
        Arguments should appear as follows:

        0 - training data for human-nonhuman classifier
        1 - training data for event classifier
        2 - training data for self-other classifier
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

        //event classifier
        String pathToEventClassifierFile = "classifiers/"+readTweetsGetFeatures.eventClassifierName+".txt";
        File eventClassifierFile = new File(pathToEventClassifierFile);
        trainAndSaveClassifier(pathToEventClassifierFile, readTweetsGetFeatures.eventClassifierName, args[1]);

        //self-other classifier
        String pathToSvOClassifierFile = "classifiers/"+readTweetsGetFeatures.selfOtherClassifierName+".txt";
        File SvOClassifierFile = new File(pathToSvOClassifierFile);
        trainAndSaveClassifier(pathToSvOClassifierFile, readTweetsGetFeatures.selfOtherClassifierName, args[2]);
    }

}

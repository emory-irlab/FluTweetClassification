import java.io.IOException;
import java.io.File;
import java.util.ArrayList;

/**
 * Created by Alec Wolyniec on 8/17/16.
 *
 * Trains and saves the classifiers to be used by the program
 */
public class TrainAndSaveClassifiers {

    public static void trainAndSaveClassifier(String pathToClassifierFile, String classifierType, String pathToData, TweetFeatureExtractor tweetFeatureExtractor/*, String pathToTestData*/) throws ClassNotFoundException, IOException, InterruptedException {
        MaxEntClassification classifier = new MaxEntClassification(pathToClassifierFile, runClassifierOnTweets.nCores);

        //get vectors using multithreaded method
        TweetVector[] tweetVectors = tweetFeatureExtractor.getVectorModelsFromTweetsMultithreaded(pathToData, classifierType);

        //add vectors to instance list and train
        classifier.addToInstanceList(tweetVectors);
        classifier.trainClassifier(classifier.instances);
        classifier.clearInstances();

        File classifierFile = new File(pathToClassifierFile);
        classifier.saveClassifier(classifierFile);
    }

    /*
        Arguments should appear as follows:

        0 - training data for human vs non-human
        1 - training data for event
        2 - training data for self-other
     */
    public static void main (String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        //get rid of any old classifiers
        //get rid of any old n-gram model "accepted n-grams" files
        util.emptyDirectory(new File("classifiers"));
        util.emptyDirectory(new File("nGramModels"));

        //get the number of cores needed to run
        int nCores = runClassifierOnTweets.nCores;
        //initialize a feature extractor
        TweetFeatureExtractor tweetFeatureExtractor = new TweetFeatureExtractor(args[1], args[2], nCores);
/*
        //human-non-human classifier
        String pathToHvNClassifierFile = "classifiers/"+ TweetFeatureExtractor.humanNonHumanClassifierName+".txt";
        File HvNClassifierFile = new File(pathToHvNClassifierFile);
        trainAndSaveClassifier(pathToHvNClassifierFile, TweetFeatureExtractor.humanNonHumanClassifierName, args[0], tweetFeatureExtractor);
*/
/*
        //event classifier
        String pathToEventClassifierFile = "classifiers/"+TweetFeatureExtractor.eventClassifierName+".txt";
        File eventClassifierFile = new File(pathToEventClassifierFile);
        trainAndSaveClassifier(pathToEventClassifierFile, TweetFeatureExtractor.eventClassifierName, args[1], tweetFeatureExtractor);
*/

        //self-other classifier
        String pathToSvOClassifierFile = "classifiers/"+TweetFeatureExtractor.selfOtherClassifierName+".txt";
        File SvOClassifierFile = new File(pathToSvOClassifierFile);
        trainAndSaveClassifier(pathToSvOClassifierFile, TweetFeatureExtractor.selfOtherClassifierName, args[2], tweetFeatureExtractor/*, args[3]*/);

    }

}

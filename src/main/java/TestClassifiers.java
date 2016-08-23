import java.io.File;
import java.io.IOException;

/**
 * Created by Alec Wolyniec on 8/17/16.
 */
public class TestClassifiers {

    /*
        Arguments should be set as such:

        0 - path to train/test tweets for the human classifier
        1 - path to train/test tweets for the event classifier
        2 - path to train/test tweets for the self-other classifier
        3 - name of the test results file for the human classifier
        4 - name of the test results file for the event classifier
        5 - name of the test results file for the self-other classifier
     */
    public static void main (String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        //get rid of the old classifiers
        //get rid of the old n-gram model "accepted n-grams" files
        util.emptyDirectory(new File("classifiers"));
        util.emptyDirectory(new File("nGramModels"));


        String pathToHumanTweets = args[0];
        String pathToEventTweets = args[1];
        String pathToSelfOtherTweets = args[2];

        String pathToTestResultsHuman = "data/testResultsHvNClassifier/"+args[3]+".txt";
        String pathToTestResultsEvent = "data/testResultsEventClassifier/"+args[4]+".txt";
        String pathToTestResultsSelfOther = "data/testResultsSvO/"+args[5]+".txt";

        TweetVector[] trainingTweetVectors;


        //human-nonhuman classifier
        MaxEntClassification classifier = new MaxEntClassification("", runClassifierOnTweets.nCores);
/*        trainingTweetVectors = readTweetsGetFeatures.getVectorModelsFromTweets(pathToHumanTweets, readTweetsGetFeatures.humanNonHumanClassifierName, runClassifierOnTweets.nCores);
        classifier.addToInstanceList(trainingTweetVectors);
        //trainingTweetVectors = null;
         //cross-validation tests
        //classifier.crossValidate(5, pathToTestResultsHumanNonHuman); //cross-validation must have at least 2 folds
        classifier.crossValidate(5, pathToTestResultsHuman, "null_class", 0.5);
        //classifier.crossValidate(5, pathToTestResultsHuman, "null_class", new double[] {0.5, 0.6, 0.7, 0.8, 0.9});
         //split multiple times test
        //classifier.runNSplits(1, pathToTestResultsHuman, "null_class", 0.9);
*/

        //event classifier
        classifier = new MaxEntClassification("", runClassifierOnTweets.nCores);
        trainingTweetVectors = readTweetsGetFeatures.getVectorModelsFromTweets(pathToEventTweets, readTweetsGetFeatures.eventClassifierName, runClassifierOnTweets.nCores);
        classifier.addToInstanceList(trainingTweetVectors);
        trainingTweetVectors = null;
         //cross-validation tests
        //classifier.crossValidate(5, pathToTestResultsEvent); //cross-validation must have at least 2 folds
        classifier.crossValidate(5, pathToTestResultsEvent, "null_class", 0.0);
        //classifier.crossValidate(5, pathToTestResultsEvent, "null_class", new double[] {0.0, 0.1, 0.2, 0.3, 0.4, 0.5,
                //0.6, 0.7, 0.8, 0.9});
        //split multiple times test
        //classifier.runNSplits(1, pathToTestResultsEvent, "null_class", 0.9);


/*
        //self-other classifier
        classifier = new MaxEntClassification("", runClassifierOnTweets.nCores);
        trainingTweetVectors = readTweetsGetFeatures.getVectorModelsFromTweets(pathToSelfOtherTweets, readTweetsGetFeatures.selfOtherClassifierName, runClassifierOnTweets.nCores);
        classifier.addToInstanceList(trainingTweetVectors);
        trainingTweetVectors = null;
        //cross-validation tests
        //classifier.crossValidate(5, pathToTestResultsSelfOther); //cross-validation must have at least 2 folds
        classifier.crossValidate(5, pathToTestResultsSelfOther, "null_class", 0.5);
        //classifier.crossValidate(5, pathToTestResultsSelfOther, "null_class", new double[] {0.5, 0.6, 0.7, 0.8, 0.9});
        //split multiple times test
        //classifier.runNSplits(1, pathToTestResultsSelfOther, "null_class", 0.9);
*/
    }
}

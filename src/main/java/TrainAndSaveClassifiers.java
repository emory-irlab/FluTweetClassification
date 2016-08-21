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

    /*
        Arguments should appear as follows:

        0 - training data for human-nonhuman classifier
        1 - training data for event classifier
        2 - training data for self-other classifier
     */
    public static void main (String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        //clean everything up for the new training run
         //delete all of the n-gram model "accepted n-grams" files
        MaxEntClassification classifier;
        File classifierFile;

        //get rid of the old classifiers
        //get rid of the old n-gram model "accepted n-grams" files
        util.emptyDirectory(new File("classifiers"));
        util.emptyDirectory(new File("nGramModels"));


        //human-non-human classifier
        classifier = MaxEntClassification.trainAndReturnClassifier(readTweetsGetFeatures.humanNonHumanClassifierName, args[0]);
        classifierFile = new File("classifiers/"+readTweetsGetFeatures.humanNonHumanClassifierName+".txt");
        /*
        if (classifierFile.exists()) {
            classifierFile.delete();
            classifierFile.createNewFile();
        }
        */
        classifier.saveClassifier(classifierFile);

        //event classifier
        classifier = MaxEntClassification.trainAndReturnClassifier(readTweetsGetFeatures.eventClassifierName, args[1]);
        classifierFile = new File("classifiers/"+readTweetsGetFeatures.eventClassifierName+".txt");
        /*
        if (classifierFile.exists()) {
            classifierFile.delete();
            classifierFile.createNewFile();
        }
        */
        classifier.saveClassifier(classifierFile);

        //self-other classifier
        classifier = MaxEntClassification.trainAndReturnClassifier(readTweetsGetFeatures.selfOtherClassifierName, args[2]);
        classifierFile = new File("classifiers/"+readTweetsGetFeatures.selfOtherClassifierName+".txt");
        /*
        if (classifierFile.exists()) {
            classifierFile.delete();
            classifierFile.createNewFile();
        }
        */
        classifier.saveClassifier(classifierFile);

    }

}

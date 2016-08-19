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

        //human-non-human classifier
        MaxEntClassification classifier = MaxEntClassification.trainAndReturnClassifier(readTweetsGetFeatures.humanNonHumanClassifierName, args[0]);
        classifier.saveClassifier(new File("classifiers/humanNonHuman.txt"));

        //event classifier
        classifier = MaxEntClassification.trainAndReturnClassifier(readTweetsGetFeatures.eventClassifierName, args[1]);
        classifier.saveClassifier(new File("classifiers/event.txt"));

        //self-other classifier
        classifier = MaxEntClassification.trainAndReturnClassifier(readTweetsGetFeatures.selfOtherClassifierName, args[2]);
        classifier.saveClassifier(new File("classifiers/selfOther.txt"));
    }

}

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.pipe.Array2FeatureVector;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.Target2Label;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.Labeling;
import cc.mallet.util.Randoms;

public class SelfVsOtherClassifier {


    //Alphabet of features that StanCore extracted from the data
    Alphabet dataAlphabet = new Alphabet(8);
    //Target Labels pertinent to this classifier
    LabelAlphabet targetAlphabet = new LabelAlphabet();
    public InstanceList instances;
    public Classifier maxEntClassifier;
    public File classifierFile;

    public SelfVsOtherClassifier(String pathToClassifier) throws IOException, ClassNotFoundException {

        classifierFile = new File(pathToClassifier);

        if (!classifierFile.exists()) {
            classifierFile.createNewFile();
        } else {
            maxEntClassifier = loadClassifier(classifierFile);
        }
        //Self
        targetAlphabet.lookupIndex("0", true);
        //Other
        targetAlphabet.lookupIndex("1", true);
        instances = new InstanceList(dataAlphabet, targetAlphabet);
    }

    /*
     * This method takes in a Hashtable, name, and label, converts
     * it to an instance then adds it to the instance list
     */
    public void addToInstanceList(Hashtable table, String name, String label) {

        Enumeration<String> features = table.keys();
        double[] featureValues = new double[table.size()];

        while (features.hasMoreElements()) {
            String featureName = features.nextElement();

            featureValues[dataAlphabet.lookupIndex(featureName, true)] = (double) ((Integer) table.get(featureName)).intValue();
        }
        Instance instance = new Instance(new FeatureVector(dataAlphabet, featureValues), name, label, null);

        instances.add(new Target2Label(this.targetAlphabet).pipe(instance));
    }


    public Classifier trainClassifier(InstanceList trainingInstances) {

        // Here we use a maximum entropy (ie polytomous logistic regression)
        //  classifier. Mallet includes a wide variety of classification
        //  algorithms, see the JavaDoc API for details.

        ClassifierTrainer trainer = new MaxEntTrainer();
        maxEntClassifier = trainer.train(trainingInstances);
        return maxEntClassifier;
    }

    public Classifier loadClassifier(File serializedFile)
            throws FileNotFoundException, IOException, ClassNotFoundException {

        // The standard way to save classifiers and Mallet data
        //  for repeated use is through Java serialization.
        // Here we load a serialized classifier from a file.

        Classifier classifier;

        ObjectInputStream ois =
                new ObjectInputStream (new FileInputStream (serializedFile));
        classifier = (Classifier) ois.readObject();
        ois.close();

        return classifier;
    }

    public void saveClassifier(File serializedFile)
            throws IOException {

        // The standard method for saving classifiers in
        //  Mallet is through Java serialization. Here we
        //  write the classifier object to the specified file.

        ObjectOutputStream oos =
                new ObjectOutputStream(new FileOutputStream (serializedFile));
        oos.writeObject (maxEntClassifier);
        oos.close();
    }

    public void printLabelings(InstanceList testInstances) throws IOException {

        for (int i = 0; i < testInstances.size(); i++) {
            //Given an InstanceList, get the label for each instance that's been classified
            Labeling labeling = maxEntClassifier.classify(testInstances.get(i)).getLabeling();

            // print the labels with their weights in descending order (ie best first)
            for (int rank = 0; rank < labeling.numLocations(); rank++){
                System.out.print(labeling.getLabelAtRank(rank) + ":" +
                        labeling.getValueAtRank(rank) + " ");
            }
            System.out.println();
        }
    }

    public void evaluate(InstanceList testInstances) throws IOException {

        // Create an InstanceList that will contain the test data.
        // In order to ensure compatibility, process instances
        // with the pipe used to process the original training
        // instances.

        Trial trial = new Trial(maxEntClassifier, testInstances);

        /*System.out.println("------------");

        PrintWriter p = new PrintWriter(System.out);

        ((MaxEnt)maxEntClassifier).printRank(p);
        p.close();

        System.out.println("------------");*/

        //printLabelings(maxEntClassifier, testInstances);

        System.out.println("Accuracy: " + trial.getAccuracy());
        System.out.println("F1 for class 'Self': " + trial.getF1(0));
        System.out.println("Precision for class 'Self': " + trial.getPrecision(0));
    }

    public Trial testTrainSplit(InstanceList instances) {

        int TRAINING = 0;
        int TESTING = 1;
        int VALIDATION = 2;

        // The division takes place by creating a copy of the list,
        //  randomly shuffling the copy, and then allocating
        //  instances to each sub-list based on the provided proportions.

        InstanceList[] instanceLists =
                instances.split(new Randoms(),
                        new double[] {0.8, 0.2, 0.0});

        //  The third position is for the "validation" set,
        //  which is a set of instances not used directly
        //  for training, but available for determining
        //  when to stop training and for estimating optimal
        //  settings of nuisance parameters.
        //  Most Mallet ClassifierTrainers can not currently take advantage
        //  of validation sets.

        Classifier classifier = trainClassifier(instanceLists[TRAINING]);
        return new Trial(classifier, instanceLists[TESTING]);
    }
}


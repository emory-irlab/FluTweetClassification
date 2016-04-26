/*
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;
import cc.mallet.util.Randoms;

public class SelfVsOtherClassifier {


    //Mutable instancelist that caps at 10 instances.
    private InstanceList selfVsOther = null;
    //public static Alphabet featureList = null;

    private Classifier maxEnt;

    public SelfVsOtherClassifier(String pathToClassifier) throws IOException, ClassNotFoundException {

		/* Methods allow for mutating its field such that it
		 * can be trained accordingly i.e. selfVsOther.addToInstanceList(TweetVector)
		 *
		 * Its instance list caps at 10
		 *
		 * We'll want to load this classifier upon its construction
		 *

        File classifierFile = new File(pathToClassifier);

        if (!classifierFile.exists()) {
            classifierFile.createNewFile();
        } else {
            this.maxEnt = loadClassifier(classifierFile);
        }
    }
    /*
     * This method takes in a Hashtable, name, and label then adds
     * it to the InstanceList
     *
    public void addToInstanceList(Hashtable table, String name, String label) {

        //Keep the cap of the InstanceList at 10 for computation's sake
        if (selfVsOther.size() >= 10) {
            selfVsOther.clear();
        }

        Enumeration<String> features = table.keys();
        Alphabet dict = new Alphabet();
        int featureIndex = 0;
        double[] featureValues = new double[table.size()];

        while (features.hasMoreElements()) {
            dict.startGrowth();
            featureValues[featureIndex] = (double) table.get(features.nextElement());
        }

        dict.stopGrowth();

        FeatureVector fv = new FeatureVector(dict, featureValues);
        Instance instance = new Instance(fv, name, null, label);

        selfVsOther.add(instance);
    }


    public Classifier trainClassifier(InstanceList trainingInstances) {

        // Here we use a maximum entropy (ie polytomous logistic regression)
        //  classifier. Mallet includes a wide variety of classification
        //  algorithms, see the JavaDoc API for details.

        ClassifierTrainer trainer = new MaxEntTrainer();
        return trainer.train(trainingInstances);
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

    public void saveClassifier(Classifier classifier, File serializedFile)
            throws IOException {

        // The standard method for saving classifiers in
        //  Mallet is through Java serialization. Here we
        //  write the classifier object to the specified file.

        ObjectOutputStream oos =
                new ObjectOutputStream(new FileOutputStream (serializedFile));
        oos.writeObject (classifier);
        oos.close();
    }

    public void printLabelings(Classifier classifier, File file) throws IOException {

        // Create a new iterator that will read raw instance data from
        //  the lines of a file.
        // Lines should be formatted as:
        //
        //   [name] [label] [data ... ]
        //
        //  in this case, "label" is ignored.

        CsvIterator reader =
                new CsvIterator(new FileReader(file),
                        "/0",
                        3, 2, 1);  // (data, label, name) field indices

        // Create an iterator that will pass each instance through
        //  the same pipe that was used to create the training data
        //  for the classifier.
        Iterator instances =
                classifier.getInstancePipe().newIteratorFrom(reader);

        // Classifier.classify() returns a Classification object
        //  that includes the instance, the classifier, and the
        //  classification results (the labeling). Here we only
        //  care about the Labeling.
        while (instances.hasNext()) {
            Labeling labeling = classifier.classify(instances.next()).getLabeling();

            // print the labels with their weights in descending order (ie best first)

            for (int rank = 0; rank < labeling.numLocations(); rank++){
                System.out.print(labeling.getLabelAtRank(rank) + ":" +
                        labeling.getValueAtRank(rank) + " ");
            }
            System.out.println();

        }
    }

    public void evaluate(Classifier classifier, File file) throws IOException {

        // Create an InstanceList that will contain the test data.
        // In order to ensure compatibility, process instances
        //  with the pipe used to process the original training
        //  instances.

        InstanceList testInstances = new InstanceList((classifier).getInstancePipe());

        // Create a new iterator that will read raw instance data from
        //  the lines of a file.
        // Lines should be formatted as:
        //
        //   [name] [label] [data ... ]

        CsvIterator reader =
                new CsvIterator(new FileReader(file),
                        "/0",
                        3, 2, 1);  // (data, label, name) field indices

        // Add all instances loaded by the iterator to
        //  our instance list, passing the raw input data
        //  through the classifier's original input pipe.

        testInstances.addThruPipe(reader);

        Trial trial = new Trial(classifier, testInstances);

        // The Trial class implements many standard evaluation
        //  metrics. See the JavaDoc API for more details.

        System.out.println("Accuracy: " + trial.getAccuracy());

        // precision, recall, and F1 are calculated for a specific
        //  class, which can be identified by an object (usually
        //  a String) or the integer ID of the class

        System.out.println("F1 for class 'good': " + trial.getF1("good"));

        System.out.println("Precision for class '" +
                classifier.getLabelAlphabet().lookupLabel(1) + "': " +
                trial.getPrecision(1));
    }

    public Trial testTrainSplit(InstanceList instances) {

        int TRAINING = 0;
        int TESTING = 1;
        int VALIDATION = 2;

        // Split the input list into training (90%) and testing (10%) lists.
        // The division takes place by creating a copy of the list,
        //  randomly shuffling the copy, and then allocating
        //  instances to each sub-list based on the provided proportions.

        InstanceList[] instanceLists =
                instances.split(new Randoms(),
                        new double[] {0.9, 0.1, 0.0});

        // The third position is for the "validation" set,
        //  which is a set of instances not used directly
        //  for training, but available for determining
        //  when to stop training and for estimating optimal
        //  settings of nuisance parameters.
        // Most Mallet ClassifierTrainers can not currently take advantage
        //  of validation sets.

        Classifier classifier = trainClassifier( instanceLists[TRAINING] );
        return new Trial(classifier, instanceLists[TESTING]);
    }
}
*/



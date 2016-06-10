import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.classify.evaluate.AccuracyCoverage;
import cc.mallet.pipe.Target2Label;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.Labeling;
import cc.mallet.util.Randoms;
import cc.mallet.types.CrossValidationIterator;

public class MaxEntClassification {

	/*
	 * General MaxEnt Classifier capable of multi-classification
	 * via supervised learning. Enter a filename to store the classifier at.
	 * */

    //Alphabet of features that StanCore extracted from the data
    public Alphabet dataAlphabet = new Alphabet(8);
    //Target Labels pertinent to this classifier
    public LabelAlphabet targetAlphabet = new LabelAlphabet();
    public InstanceList instances;
    public Classifier maxEntClassifier;
    public File classifierFile;

    public String[] labels;

    public MaxEntClassification(String pathToClassifier, String[] labels) throws IOException, ClassNotFoundException {

        classifierFile = new File(pathToClassifier);

        if (!classifierFile.exists()) {
            classifierFile.createNewFile();
        } else {
            maxEntClassifier = loadClassifier(classifierFile);
        }

        this.labels = labels;

        /*
         * The index of the label in labels corresponds to the integer-label
         * the classifier will assign.
         * */

        for (int i = 0; i < labels.length; i++) {
            targetAlphabet.lookupIndex(labels[i], true);
        }

        instances = new InstanceList(dataAlphabet, targetAlphabet);
    }

    /*
     * This method takes in a Hashtable, name, and label, converts
     * it to an instance then adds it to the instance list.
     * The hashtable should have features for its keys and an
     * associated integer value for its features.
     */
    public void addToInstanceList(Hashtable<String, Integer> table, String name, String label) {

        Enumeration<String> features = table.keys();
        double[] featureValues = new double[table.size()];

        while (features.hasMoreElements()) {
            String featureName = features.nextElement();
            featureValues[dataAlphabet.lookupIndex(featureName, true)] = (double) ((Integer) table.get(featureName)).intValue();
        }

        Instance instance = new Instance(new FeatureVector(dataAlphabet, featureValues), label, name, null);
        instances.add(new Target2Label(this.targetAlphabet).pipe(instance));
    }

    /*Attempt to retain same target alphabet
     *but remove all current instances.
     * */
    public void clearInstances() {
        instances = new InstanceList(dataAlphabet, targetAlphabet);
    }

    public void evaluate(InstanceList testInstances) throws IOException {

        // Create an InstanceList that will contain the test data.
        // In order to ensure compatibility, process instances
        // with the pipe used to process the original training
        // instances.
        Trial trial = new Trial(maxEntClassifier, testInstances);

        getAreaUnderCurve(trial);

        //printLabelings(testInstances);
        //System.out.println();
        PrintWriter p = new PrintWriter(System.out);
        ((MaxEnt) maxEntClassifier).print(p);
        System.out.println("--------------------");
        System.out.println("ACCURACY: " + trial.getAccuracy());
        System.out.println("--------------------");
        System.out.println();

        for (int i = 0; i < labels.length; i++) {
            System.out.println("Metrics for class " + labels[i]);
            System.out.println("F1: " + trial.getF1(i));
            System.out.println("Precision: " + trial.getPrecision(i));
            System.out.println("Recall: " + trial.getRecall(i));
            System.out.println();
        }

        runClassifierOnTweets.endRunTime = System.currentTimeMillis();
        System.out.println("------------------------------");
        System.out.println("FINISHED RUN TME IN " + (runClassifierOnTweets.endRunTime - (double)runClassifierOnTweets.startRunTime)/1000 + " seconds.");
        System.out.println("------------------------------");
        System.out.println();

        p.close();
    }

    public double getAreaUnderCurve(Trial t) {

        AccuracyCoverage a = new AccuracyCoverage(t, "AUC", "Labelings");

        a.displayGraph();
        return a.cumulativeAccuracy();
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

    public void printLabelings(InstanceList testInstances, String path) throws IOException {

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

    public InstanceList split(InstanceList instances) {

        int TRAINING = 0;
        int TESTING = 1;
        int VALIDATION = 2;

        // The division takes place by creating a copy of the list,
        //  randomly shuffling the copy, and then allocating
        //  instances to each sub-list based on the provided proportions.

        InstanceList[] instanceLists =
                instances.split(new Randoms(),
                        new double[] {0.8, 0.2, 0.0}); //always generates the same sequence of tweets

        //  The third position is for the "validation" set,
        //  which is a set of instances not used directly
        //  for training, but available for determining
        //  when to stop training and for estimating optimal
        //  settings of nuisance parameters.
        //  Most Mallet ClassifierTrainers can not currently take advantage
        //  of validation sets.
        this.instances = instanceLists[TRAINING];
        return instanceLists[TESTING];
    }

    public Classifier trainClassifier(InstanceList trainingInstances) {

        // Here we use a maximum entropy (ie polytomous logistic regression)
        //  classifier. Mallet includes a wide variety of classification
        //  algorithms, see the JavaDoc API for details.

        ClassifierTrainer<MaxEnt> trainer = new MaxEntTrainer();
        maxEntClassifier = trainer.train(trainingInstances);
        return maxEntClassifier;
    }

    /*
            Performs n-fold cross-validation and prints out the results

            DOES NOT WORK
        */
    //somehow only runs once when the evaluate() method is called
    public void crossValidate(int n_folds) throws IOException {
        CrossValidationIterator crossValidationIterator = new CrossValidationIterator(instances, n_folds, new Randoms());
        while (crossValidationIterator.hasNext()) {
            InstanceList[] split = crossValidationIterator.next();
            trainClassifier(split[0]);
            System.out.println();
            System.out.println("NEW TEST:");
            evaluate(split[1]);
        }
    }
}
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
import java.util.ArrayList;

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
	 * TODO: Do approximately 50 trials so as to get an average for 10 confidence thesholds
	 * */

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

	public String header = "Precision, Recall, Confidence Threshold";
	public File precVsRecallFile = new File("src\\precVsRecallFile.csv");
	public FileWriter writer = new FileWriter(precVsRecallFile);
	public CSVPrinter printer = new CSVPrinter(writer, CSVFormat.EXCEL.withHeader(header));

	public MaxEntClassification(String pathToClassifier) throws IOException, ClassNotFoundException {

		classifierFile = new File(pathToClassifier);

        if (!classifierFile.exists()) {
        	classifierFile.createNewFile();
        } else {
        	maxEntClassifier = loadClassifier(classifierFile);
        }

        //this.labels = labels;
        targetAlphabet.startGrowth();

//        for (int i = 0; i < labels.length; i++) {
//        	targetAlphabet.lookupIndex(String.valueOf(i), true);
//        }

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

	public void getDataAboveThreshold(Trial t, InstanceList testInstances, String posLabel, double threshold) throws IOException {

		ArrayList<Double> metrics = new ArrayList<>();

		if (!precVsRecallFile.exists()) {
			precVsRecallFile.createNewFile();
		}

		for (int i = 0; i < testInstances.size(); i++) {
        	//Given an InstanceList, get the label for each instance that's been classified
            Labeling labeling = maxEntClassifier.classify(testInstances.get(i)).getLabeling();

            // print the labels with their weights in descending order (ie best first)
            for (int rank = 0; rank < labeling.numLocations(); rank++) {

            	if (labeling.getLabelAtRank(rank).equals(posLabel)) {
            		if (labeling.getValueAtRank(rank) >= threshold) {
            			metrics.add(t.getPrecision(0));
            			metrics.add(t.getRecall(0));
            			metrics.add(threshold);
            			printer.printRecord(metrics);
            		}
            	}
            }
        }
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
        //PrintWriter p = new PrintWriter(System.out);
    	//((MaxEnt) maxEntClassifier).print(p);
    	System.out.println("--------------------");
        System.out.println("ACCURACY: " + trial.getAccuracy());
        System.out.println("--------------------");
        System.out.println();

        for (int i = 0; i < targetAlphabet.size(); i++) {
        	System.out.println("Metrics for class " + targetAlphabet.lookupLabel(i));
        	System.out.println("F1: " + trial.getF1(i));
        	System.out.println("Precision: " + trial.getPrecision(i));
        	System.out.println("Recall: " + trial.getRecall(i));
        	System.out.println();
        }

        runClassifierOnTweets.endRunTime = System.currentTimeMillis();
        System.out.println("------------------------------");
        System.out.println("FINISHED RUN TME IN " + (runClassifierOnTweets.endRunTime - runClassifierOnTweets.startRunTime)/1000 + " seconds.");
        System.out.println("------------------------------");
        System.out.println();

        //p.close();
    }

	public void getAreaUnderCurve(Trial t) {

		AccuracyCoverage a = new AccuracyCoverage(t, "AUC", "Labelings");

		a.displayGraph();
		System.out.println(a.cumulativeAccuracy());
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

            NOTE: In the evaluate() method, comment out the printLabelings() method call and
            anything to do with a PrintWriter when this is called. Otherwise, output may be suppressed
     */
    public void crossValidate(int n_folds) throws IOException {
        CrossValidationIterator crossValidationIterator = new CrossValidationIterator(instances, n_folds, new Randoms());
        while (crossValidationIterator.hasNext()) {
            InstanceList[] split = crossValidationIterator.next();
            trainClassifier(split[0]);
            System.out.println();
            evaluate(split[1]);
        }
    }

    public void printConfidence(InstanceList testInstances, String desiredClass) {
        int[] trues = new int[6];
        int[] falses = new int[6];
        for (int i = 0; i < testInstances.size(); i++) {
            //Given an InstanceList, get the label for each instance that's been classified
            Labeling labeling = maxEntClassifier.classify(testInstances.get(i)).getLabeling();

            //get the confidence of correct matches and incorrect matches to the desired class
            if (labeling.getLabelAtRank(0).toString().equals(desiredClass)) {
                //if the current test instance was labeled with the desired class, check to see if it was correctly classified
                double confidence = labeling.getValueAtRank(0);
                int[] proper;
                if (testInstances.get(i).getTarget().toString().equals(desiredClass)) {
                    proper = trues;
                } else {
                    proper = falses;
                }
                if (confidence > 0.95) proper[5]++;
                else if (confidence > 0.90) proper[4]++;
                else if (confidence > 0.80) proper[3]++;
                else if (confidence > 0.70) proper[2]++;
                else if (confidence > 0.60) proper[1]++;
                else proper[0]++;
            }
        }
        //print figures
        int truesAll = trues[0]+trues[1]+trues[2]+trues[3]+trues[4]+trues[5];
        int falsesAll = falses[0]+falses[1]+falses[2]+falses[3]+falses[4]+falses[5];
        int sum = truesAll + falsesAll;
        int size = testInstances.size();
        System.out.println();
        System.out.println("Total size of test set:"+size);
        System.out.println("Total number of instances classified as "+desiredClass+": "+sum);

        System.out.println();
        int data60 = truesAll - trues[0];
        int data70 = data60 - trues[1];
        int data80 = data70 - trues[2];
        int data90 = data80 - trues[3];
        int data95 = data90 - trues[4];
        double conf60 = ((double)data60)/(sum - trues[0] - falses[0]);
        double conf70 = ((double)data70)/(sum - trues[0] - trues[1] - falses[0] - falses[1]);
        double conf80 = ((double)data80)/(sum - trues[0] - trues[1] - trues[2] - falses[0] - falses[1] - falses[2]);
        double conf90 = ((double)data90)/(trues[4] + trues[5] + falses[4] + falses[5]);
        double conf95 = ((double)data95)/(trues[5] + falses[5]);
        System.out.println("Precision for this class by confidence threshold: ");
        System.out.println("50%: "+(double)truesAll/sum+" ("+100*((double)truesAll)/size+"% of input data)");
        System.out.println("60%: "+conf60+" ("+100*((double)data60)/size+"% of input data)");
        System.out.println("70%: "+conf70+" ("+100*((double)data70)/size+"% of input data)");
        System.out.println("80%: "+conf80+" ("+100*((double)data80)/size+"% of input data)");
        System.out.println("90%: "+conf90+" ("+100*((double)data90)/size+"% of input data)");
        System.out.println("95%: "+conf95+" ("+100*((double)data95)/size+"% of input data)");
    }
}
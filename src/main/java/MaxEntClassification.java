import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.io.BufferedWriter;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.classify.evaluate.AccuracyCoverage;
import cc.mallet.types.CrossValidationIterator;
import cc.mallet.pipe.Target2Label;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.Labeling;
import cc.mallet.util.Randoms;

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

	public MaxEntClassification(String pathToClassifier) throws IOException, ClassNotFoundException {

		classifierFile = new File(pathToClassifier);

		if (!classifierFile.exists()) {
			classifierFile.createNewFile();
		} else {
			maxEntClassifier = loadClassifier(classifierFile);
		}

		targetAlphabet.startGrowth();

		instances = new InstanceList(dataAlphabet, targetAlphabet);
	}

	/*
	 * This method takes in a Hashtable, name, and label, converts
	 * it to an instance then adds it to the instance list.
	 * The hashtable should have features for its keys and an
	 * associated integer value for its features.
	 */
	public void addToInstanceList(Hashtable<String, Double> table, String name, String label) {
		//test
		if (table.size() > 1000) {
			System.out.print("");
		}

		Enumeration<String> features = table.keys();
		int numberOfNewFeatures = getNumOfNewFeatures(table);
		double[] featureValues = new double[dataAlphabet.size() + numberOfNewFeatures];

		while (features.hasMoreElements()) {
			String featureName = features.nextElement();
			featureValues[dataAlphabet.lookupIndex(featureName, true)] = table.get(featureName);
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

	public Hashtable<String, Hashtable<String, Double>> evaluate(InstanceList testInstances) throws IOException {
		Hashtable<String, Hashtable<String, Double>> output = new Hashtable<String, Hashtable<String, Double>>();

		// Create an InstanceList that will contain the test data.
		// In order to ensure compatibility, process instances
		// with the pipe used to process the original training
		// instances.
		Trial trial = new Trial(maxEntClassifier, testInstances);

		//getAreaUnderCurve(trial);

		//printLabelings(testInstances);
		//System.out.println();
		//PrintWriter p = new PrintWriter("data/featureWeights.txt");
		//p.write("\n");
		//((MaxEnt) maxEntClassifier).print();
		//p.close();

		//first entry is accuracy
		Hashtable<String, Double> accuracy = new Hashtable<String, Double>();
		accuracy.put("Accuracy", trial.getAccuracy());
		output.put("Accuracy", accuracy);

		//other entries are figures for each class
		for (int i = 0; i < targetAlphabet.size(); i++) {
			Hashtable<String, Double> thisClass = new Hashtable<String, Double>();
			thisClass.put("F1", trial.getF1(i));
			thisClass.put("Precision", trial.getPrecision(i));
			thisClass.put("Recall", trial.getRecall(i));
			output.put(targetAlphabet.lookupLabel(i).toString(), thisClass);
		}

		return output;
	}

	/*
        Average results given across several trials
    */
	public Hashtable<String, Hashtable<String, Double>> averageTrialResults(ArrayList<Hashtable<String, Hashtable<String, Double>>> data) {
		Hashtable<String, Hashtable<String, Double>> averagedData = new Hashtable<String, Hashtable<String, Double>>();
		int numTrials = data.size();

		//add up figures from each trial
		for (int i = 0; i < numTrials; i++) {
			Hashtable<String, Hashtable<String, Double>> trial = data.get(i);
			//look into the various types of data in each trial's figures (F1, accuracy, etc)
			Enumeration<String> trialKeys = trial.keys();
			while (trialKeys.hasMoreElements()) {
				String currentDataPtName = trialKeys.nextElement();

				Hashtable<String, Double> dataPoint = trial.get(currentDataPtName);

				//get previous values and add to them, if they exist
				Hashtable<String, Double> oldCumulativeDataPoint;
				if (averagedData.size() < trial.size()) oldCumulativeDataPoint = null;
				else oldCumulativeDataPoint = averagedData.get(currentDataPtName);

				Hashtable<String, Double> newCumulativeDataPoint = new Hashtable<String, Double>(dataPoint.size());
				//add
				Enumeration<String> dataPointKeys = dataPoint.keys();
				while (dataPointKeys.hasMoreElements()) {
					String currentValueName = dataPointKeys.nextElement();

					//add the current value to the running sum
					double dataValue;
					if (oldCumulativeDataPoint == null) dataValue = dataPoint.get(currentValueName);
					else dataValue = oldCumulativeDataPoint.get(currentValueName) + dataPoint.get(currentValueName);

					//get averages if it's the last trial
					if (i == numTrials - 1) dataValue /= numTrials;

					newCumulativeDataPoint.put(currentValueName, dataValue);
				}
				//update the value
				averagedData.put(currentDataPtName, newCumulativeDataPoint);
			}
		}

		return averagedData;
	}

	/*
        Prints the results of an evaluate calculation
     */
	public void printEvaluated(Hashtable<String, Hashtable<String, Double>> input, int nTrials) {
		System.out.println();
		System.out.println("Average results for "+nTrials+" trials:");

		System.out.println("--------------------");
		System.out.println("ACCURACY: " + input.remove("Accuracy").get("Accuracy"));
		System.out.println("--------------------");

		Enumeration<String> classes = input.keys();

		while (classes.hasMoreElements()) {
			String className = classes.nextElement();
			Hashtable<String, Double> values = input.get(className);
			Enumeration<String> namesOfValues = values.keys();
			System.out.println();
			System.out.println("Metrics for class "+className);
			while(namesOfValues.hasMoreElements()) {
				String currName = namesOfValues.nextElement();
				System.out.println(currName+": "+values.get(currName));
			}
		}
	}

	public void writeEvaluatedToFile(Hashtable<String, Hashtable<String, Double>> input, int nTrials, String path) throws IOException {
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(path)));

		bufferedWriter.newLine();
		bufferedWriter.write("Average results for "+nTrials+" trials:");
		bufferedWriter.newLine();

		bufferedWriter.write("--------------------");
		bufferedWriter.newLine();
		bufferedWriter.write("ACCURACY: " + input.remove("Accuracy").get("Accuracy"));
		bufferedWriter.newLine();
		bufferedWriter.write("--------------------");
		bufferedWriter.newLine();

		Enumeration<String> classes = input.keys();

		while (classes.hasMoreElements()) {
			String className = classes.nextElement();
			Hashtable<String, Double> values = input.get(className);
			Enumeration<String> namesOfValues = values.keys();
			bufferedWriter.newLine();
			bufferedWriter.write("Metrics for class "+className);
			bufferedWriter.newLine();
			while(namesOfValues.hasMoreElements()) {
				String currName = namesOfValues.nextElement();
				bufferedWriter.write(currName+": "+values.get(currName));
				bufferedWriter.newLine();
			}
		}

		bufferedWriter.close();
	}

	/*
	public void evaluate(InstanceList testInstances) throws IOException {

        // Create an InstanceList that will contain the test data.                                         
        // In order to ensure compatibility, process instances                                             
        // with the pipe used to process the original training                                            
        // instances.
		
        Trial trial = new Trial(maxEntClassifier, testInstances);
        
        getAreaUnderCurve(trial);
        
        PrintWriter p = new PrintWriter(System.out);
    	((MaxEnt) maxEntClassifier).print(p);
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
        System.out.println("FINISHED RUN TME IN " + (runClassifierOnTweets.endRunTime - runClassifierOnTweets.startRunTime)/1000 + " SECONDS.");
        System.out.println("------------------------------");
        System.out.println();
        
        p.close();
    }
    */
	
	/*
	 * Method to be used after first training the classifier.
	 * It removes all instances in "instances" that are below
	 * the threshold the user provides.
	 * */

	public void evaluateWithConfidenceThreshold(InstanceList testInstances, double threshold) throws IOException {

		System.out.println("For threshold: " + threshold);

		for (int i = 0; i < testInstances.size(); i++) {
			//Given an InstanceList, get the label for each instance that's been classified
			Labeling labeling = maxEntClassifier.classify(testInstances.get(i)).getLabeling();

			// print the labels with their weights in descending order (ie best first)
			for (int rank = 0; rank < labeling.numLocations(); rank++) {

				if (labeling.getValueAtRank(rank) < threshold) {
					testInstances.remove(i);
				}
			}
		}

		Trial trial = new Trial(maxEntClassifier, testInstances);

		//printDataAboveThresholdCSV(trial, threshold, "person");

		//getAreaUnderCurve(trial);

		PrintWriter p = new PrintWriter(System.out);
		((MaxEnt) maxEntClassifier).print(p);
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
		System.out.println("FINISHED RUN TME IN " + (runClassifierOnTweets.endRunTime - runClassifierOnTweets.startRunTime)/1000 + " SECONDS.");
		System.out.println("------------------------------");
		System.out.println();

		p.close();
	}

	public void getAreaUnderCurve(Trial t) {

		AccuracyCoverage a = new AccuracyCoverage(t, "AUC", "Labelings");

		a.displayGraph();
		System.out.println(a.cumulativeAccuracy());
	}
	/*
    From the given data alphabet, count the number of features in the input table that have not been
    seen before
    */
	public int getNumOfNewFeatures(Hashtable<String, Double> input) {
		int newFeats = 0;
		Enumeration<String> inputKeys = input.keys();
		//check each feature name in the input data for a match in the data alphabet
		while (inputKeys.hasMoreElements()) {
			boolean isInDataAlphabet = false;
			String currentFeature = inputKeys.nextElement();
			Iterator it = dataAlphabet.iterator();
			while (it.hasNext()) {
				if (it.next().toString().equals(currentFeature)) {
					isInDataAlphabet = true;
					break;
				}
			}
			if (!isInDataAlphabet) newFeats++;
		}

		return newFeats;
	}

	/*
        Performs n-fold cross-validation and prints out the results
    */
	public void crossValidate(int n_folds) throws IOException {
		CrossValidationIterator crossValidationIterator = new CrossValidationIterator(instances, n_folds, new Randoms());
		ArrayList<Hashtable<String, Hashtable<String, Double>>> resultsOverTrials = new ArrayList<Hashtable<String, Hashtable<String, Double>>>(n_folds);
		while (crossValidationIterator.hasNext()) {
			InstanceList[] split = crossValidationIterator.next();
			trainClassifier(split[0]);
			System.out.println();
			System.out.println("NEW TEST:");
			resultsOverTrials.add(evaluate(split[1]));
		}
		//printEvaluated(averageTrialResults(resultsOverTrials), n_folds);
		writeEvaluatedToFile(averageTrialResults(resultsOverTrials), n_folds, "data/testResults.txt");
	}

	/*
		Runs n trials on the data
	 */
	public void runNTrials(int n) throws IOException {
		ArrayList<Hashtable<String, Hashtable<String, Double>>> resultsOverTrials = new ArrayList<Hashtable<String, Hashtable<String, Double>>>(n);

		for (int i = 0; i < n; i++) {
			InstanceList testInstances = split(instances);
			trainClassifier(instances);
			saveClassifier(classifierFile);

			Hashtable<String, Hashtable<String, Double>> results = evaluate(testInstances);
			resultsOverTrials.add(results);
		}
		clearInstances();
		writeEvaluatedToFile(averageTrialResults(resultsOverTrials), n, "data/testResults.txt");
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
						new double[] {0.5, 0.5, 0.0}); //better than 0.8, 0.2 split

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
}
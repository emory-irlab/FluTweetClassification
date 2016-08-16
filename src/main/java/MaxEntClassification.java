import java.io.*;
import java.util.*;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntTrainer;
import cc.mallet.classify.Trial;
import cc.mallet.classify.evaluate.AccuracyCoverage;
import cc.mallet.types.*;
import cc.mallet.pipe.Target2Label;
import cc.mallet.util.Randoms;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import cc.mallet.classify.NaiveBayesEMTrainer;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.classify.NaiveBayes;

public class MaxEntClassification {
	
	/*
	 * General MaxEnt Classifier capable of multi-classification
	 * via supervised learning. Enter a filename to store the classifier at.
	 * */

	//Alphabet of features that StanCore extracted from the data
	//REMEMBER YOU CHANGED new Alphabet(8);
	private Alphabet dataAlphabet = new Alphabet();
	//Target Labels pertinent to this classifier
	private LabelAlphabet targetAlphabet = new LabelAlphabet();
	private InstanceList trainingInstances;
	//public Classifier maxEntClassifier;
	public MaxEnt maxEntClassifier = null; //perhaps it should just be a Classifier
	//public NaiveBayes maxEntClassifier = null;
	private File classifierFile;
	private int nCores;
	private String path;
	private ArrayList<String> labelSet;
	private ClassifierTrainer<MaxEnt> trainer;
	//private NaiveBayesTrainer trainer;

	public MaxEntClassification(String pathToClassifier, int nCores) throws IOException, ClassNotFoundException {
		path = pathToClassifier.substring(0, pathToClassifier.length()-4);
		classifierFile = new File(pathToClassifier);

		/*
		if (!classifierFile.exists()) {
			classifierFile.createNewFile();
		} else {
			maxEntClassifier = loadClassifier(classifierFile);
		}
		*/
		if (classifierFile.exists()) {
			maxEntClassifier = (MaxEnt)loadClassifier(classifierFile);
			//maxEntClassifier = (NaiveBayes)loadClassifier(classifierFile);
		}

		targetAlphabet.startGrowth();

		trainingInstances = new InstanceList(dataAlphabet, targetAlphabet);

		this.nCores = nCores;

		labelSet = new ArrayList<String>();
	}

	public MaxEntClassification(File classFile, int nCores) throws IOException, ClassNotFoundException {

		classifierFile = classFile;

		/*
		if (!classifierFile.exists()) {
			classifierFile.createNewFile();
		} else {
			maxEntClassifier = loadClassifier(classifierFile);
		}
		*/
		if (classifierFile.exists()) {
			maxEntClassifier = (MaxEnt)loadClassifier(classifierFile);
			//maxEntClassifier = (NaiveBayes)loadClassifier(classifierFile);
		}

		targetAlphabet.startGrowth();

		trainingInstances = new InstanceList(dataAlphabet, targetAlphabet);

		this.nCores = nCores;
	}

	/*
		==============
	    |   METHODS  |
	    ==============
	 */

	/*
		Adds a set of TweetVectors to the instance list
	 */
	private void addToInstanceList(TweetVector[] tweetVectors) {
		for (int i = 0; i < tweetVectors.length; i++) {
			//add the current tweet
			TweetVector currentTweet = tweetVectors[i];
			addToInstanceList(currentTweet.getFeatures(), currentTweet.getName(), currentTweet.getLabel());
		}
	}

	/*
    	Adds a TweetVector to the instance list
 	*/
	private void addToInstanceList(TweetVector tweetVector) {
		addToInstanceList(tweetVector.getFeatures(), tweetVector.getName(), tweetVector.getLabel());
	}

	/*
	 * This method takes in a Hashtable, name, and label, converts
	 * it to an instance then adds it to the instance list.
	 * The hashtable should have features for its keys and an
	 * associated integer value for its features.
	 */
	private void addToInstanceList(Hashtable<String, Double> table, String name, String label) {
		//test
		Enumeration<String> features = table.keys();
		int numberOfNewFeatures = getNumOfNewFeatures(table);
		double[] featureValues = new double[dataAlphabet.size() + numberOfNewFeatures];

		while (features.hasMoreElements()) {
			String featureName = features.nextElement();
			featureValues[dataAlphabet.lookupIndex(featureName, true)] = table.get(featureName);
		}

		Instance instance = new Instance(new FeatureVector(dataAlphabet, featureValues), label, name, null);

		trainingInstances.add(new Target2Label(targetAlphabet).pipe(instance));
	}

	/*
		Adds a given Instance to the InstanceList while maintaining a size of 1
	 */
	private void addToInstanceList(Instance instance) {

		//ensures the list is always of size 1000 or fewer
		/*
		if (trainingInstances.size() >= 1000) {
			for (int i = 0; i < trainingInstances.size(); i++) {
				trainingInstances.remove(i);
			}
		}
		trainingInstances.add(new Target2Label(this.targetAlphabet).pipe(instance));
		*/
/*
		if (trainingInstances.size() == 0) {
			trainingInstances.add(new Target2Label(this.targetAlphabet).pipe(instance));
		}
		else {
			trainingInstances.setInstance(0, new Target2Label(this.targetAlphabet).pipe(instance));
		}
		*/

		trainingInstances.add(new Target2Label(this.targetAlphabet).pipe(instance));
	}

	//adds a given Instance to the InstanceList. If the InstanceList exceeds the batch size, train the classifier using it
	public void addToInstanceListAndTrainBatchwise(TweetVector tweetVector, int batchSize) {
		addToInstanceList(tweetVector);
		if (trainingInstances.size() >= batchSize) {
			train();
		}
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
					if (oldCumulativeDataPoint == null) {
						if (dataPoint.get(currentValueName) == null) {
							dataValue = 0.0;
						}
						else {
							dataValue = dataPoint.get(currentValueName);
						}
					}
					else {
						if (dataPoint.get(currentValueName) == null) {
							dataValue = oldCumulativeDataPoint.get(currentValueName);
						}
						else {
							dataValue = oldCumulativeDataPoint.get(currentValueName) + dataPoint.get(currentValueName);
						}
					}

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

	public void clearAlphabets() {
		dataAlphabet = new Alphabet();
		targetAlphabet = new LabelAlphabet();
	}

	public void clearClassifier() { maxEntClassifier = null; }

	/*
		Attempt to retain same target alphabet
 		but remove all current instances.
    */
	public void clearInstances() {
		trainingInstances = new InstanceList(dataAlphabet, targetAlphabet);
	}

	/*
        Performs n-fold cross-validation and prints out the results

        Removed because the evaluate method doesn't mesh well with incremental training
    */
	/*
	public void crossValidate(int n_folds, String pathToResultsFile) throws IOException, InterruptedException, ClassNotFoundException {
		CrossValidationIterator crossValidationIterator = new CrossValidationIterator(trainingInstances, n_folds, new Randoms());
		ArrayList<Hashtable<String, Hashtable<String, Double>>> resultsOverTrials = new ArrayList<Hashtable<String, Hashtable<String, Double>>>(n_folds);

		ArrayList<MaxEntTestRunThread> threads = new ArrayList<MaxEntTestRunThread>();
		while (crossValidationIterator.hasNext()) {
			InstanceList[] split = crossValidationIterator.next();
			InstanceList training = split[0];
			InstanceList testing = split[1];

			trainClassifier(training, true);
			System.out.println();
			System.out.println("NEW TEST:");

			resultsOverTrials.add(evaluate(testing));
		}
		//printTestResults(averageTrialResults(resultsOverTrials), n_folds);
		writeTestResultsToFile(averageTrialResults(resultsOverTrials), n_folds, pathToResultsFile, false);
	}
	*/

	/*
    Performs n-fold cross-validation with the specified confidence threshold for the specified class.
    Prints out the results
 */
	public void crossValidate(int n_folds, TweetVector[] trainingTweetVectors, String pathToResultsFile, int batchSize) throws IOException, InterruptedException, ClassNotFoundException {
		ArrayList<Hashtable<String, Hashtable<String, Double>>> resultsOverTrials = new ArrayList<Hashtable<String, Hashtable<String, Double>>>(n_folds);
		//get folds
		double[] proportions = new double[n_folds];
		double unit = ((double)1)/n_folds;
		for (int i = 0; i < n_folds; i++) {
			proportions[i] = unit;
		}
		ArrayList<ArrayList<TweetVector>> folds = split(trainingTweetVectors, proportions);

		//give each fold a turn to be the test fold, each test fold is one trial
		for (int i = 0; i < folds.size(); i++) {
			//initialize training and testing tweets
			ArrayList<TweetVector> trainingTweets = new ArrayList<TweetVector>();
			ArrayList<TweetVector> testingTweets = folds.get(i);
			for (int j = 0; j < folds.size(); j++) {
				if (j != i) {
					ArrayList<TweetVector> currentFold = folds.get(j);
					for (TweetVector currentVector: currentFold) {
						//make an instance and add it to the list
						trainingTweets.add(currentVector);
					}
				}
			}

			//new training session, clear the classifier, initialize the alphabets (which also initializes the instances)
			clearClassifier();
			initializeAlphabets(trainingTweets);

			//train using the training folds
			for (int j = 0; j < trainingTweets.size(); j++) {
				addToInstanceListAndTrainBatchwise(trainingTweets.get(j), batchSize);
			}
			//train any incomplete batches
			train();

/*
			for (int j = 0; j < folds.size(); j++) {
				//index i is the test fold, train on the others
				if (j != i) {
					ArrayList<TweetVector> currentFold = folds.get(j);
					for (TweetVector currentVector: currentFold) {
						//make an instance and add it to the list

						train(makeInstance(currentVector));
					}
				}
			}
*/

			//test using the testing fold
			Hashtable<String, Hashtable<String, Double>> results = evaluateWithConfThreshold(testingTweets.toArray(new TweetVector[testingTweets.size()]), "null_class", 0.0);
			resultsOverTrials.add(results);
			//write out the results for the current trial
			//ensures that the results of these trials aren't appended to any other file
			if (i == 0) {
				writeTestResultsToFile(results, 1, pathToResultsFile, false);
			}
			else {
				writeTestResultsToFile(results, 1, pathToResultsFile, true);
			}
		}
		writeTestResultsToFile(averageTrialResults(resultsOverTrials), n_folds, pathToResultsFile, true);

		//clear everything because this is a train-test framework
		//clearAlphabets();
		clearInstances();
		clearClassifier();

		/*
		CrossValidationIterator crossValidationIterator = new CrossValidationIterator(trainingInstances, n_folds, new Randoms());

		while (crossValidationIterator.hasNext()) {
			InstanceList[] split = crossValidationIterator.next();
			InstanceList training = split[0];
			InstanceList testing = split[1];

			trainClassifier(training, true);
			saveClassifier(classifierFile);
			System.out.println();
			System.out.println("NEW TEST:");

			//resultsOverTrials.add(evaluateWithConfThreshold(testing, nullClass, confidenceThreshold));
			writeTestResultsToFile(resultsOverTrials.get(resultsOverTrials.size() - 1), 1, pathToResultsFile, true);
		}
		//printTestResults(averageTrialResults(resultsOverTrials), n_folds);
		writeTestResultsToFile(averageTrialResults(resultsOverTrials), n_folds, pathToResultsFile, true);
		*/
	}

	/*
		Performs n-fold cross-validation with the specified confidence threshold for the specified class.
		Prints out the results
	 */
	public void crossValidate(int n_folds, TweetVector[] trainingTweetVectors, String pathToResultsFile, String nullClass, double confidenceThreshold, int batchSize) throws IOException, InterruptedException, ClassNotFoundException {
		ArrayList<Hashtable<String, Hashtable<String, Double>>> resultsOverTrials = new ArrayList<Hashtable<String, Hashtable<String, Double>>>(n_folds);
		//get folds
		double[] proportions = new double[n_folds];
		double unit = ((double)1)/n_folds;
		for (int i = 0; i < n_folds; i++) {
			proportions[i] = unit;
		}
		ArrayList<ArrayList<TweetVector>> folds = split(trainingTweetVectors, proportions);

		//give each fold a turn to be the test fold, each test fold is one trial
		for (int i = 0; i < folds.size(); i++) {
			//initialize training and testing fields
			ArrayList<TweetVector> trainingTweets = new ArrayList<TweetVector>();
			ArrayList<TweetVector> testingTweets = folds.get(i);
			for (int j = 0; j < folds.size(); j++) {
				if (j != i) {
					ArrayList<TweetVector> currentFold = folds.get(j);
					for (TweetVector currentVector: currentFold) {
						//make an instance and add it to the list
						trainingTweets.add(currentVector);
					}
				}
			}

			//new training session, clear the classifier, initialize the alphabets (which also initializes the instances)
			clearClassifier();
			initializeAlphabets(trainingTweets);

			//train using the training folds
			//train using the training folds
			for (int j = 0; j < trainingTweets.size(); j++) {
				addToInstanceListAndTrainBatchwise(trainingTweets.get(j), batchSize);
			}
			//train any incomplete batches
			train();

			//test using the testing fold
			Hashtable<String, Hashtable<String, Double>> results = evaluateWithConfThreshold(testingTweets.toArray(new TweetVector[testingTweets.size()]), nullClass, confidenceThreshold);
			resultsOverTrials.add(results);
			//write out the results for the current trial
			//ensures that the results of these trials aren't appended to any other file
			if (i == 0) {
				writeTestResultsToFile(results, 1, pathToResultsFile, false);
			}
			else {
				writeTestResultsToFile(results, 1, pathToResultsFile, true);
			}
		}
		writeTestResultsToFile(averageTrialResults(resultsOverTrials), n_folds, pathToResultsFile, true);

		//clear everything because this is a train-test framework
		//clearAlphabets();
		clearClassifier();
		clearInstances();

		/*
		CrossValidationIterator crossValidationIterator = new CrossValidationIterator(trainingInstances, n_folds, new Randoms());

		while (crossValidationIterator.hasNext()) {
			InstanceList[] split = crossValidationIterator.next();
			InstanceList training = split[0];
			InstanceList testing = split[1];

			trainClassifier(training, true);
			saveClassifier(classifierFile);
			System.out.println();
			System.out.println("NEW TEST:");

			//resultsOverTrials.add(evaluateWithConfThreshold(testing, nullClass, confidenceThreshold));
			writeTestResultsToFile(resultsOverTrials.get(resultsOverTrials.size() - 1), 1, pathToResultsFile, true);
		}
		//printTestResults(averageTrialResults(resultsOverTrials), n_folds);
		writeTestResultsToFile(averageTrialResults(resultsOverTrials), n_folds, pathToResultsFile, true);
		*/
	}

	/*
    	Performs n-fold cross-validation with multiple specified confidence thresholds for the specified class.
    	Prints out the results
 	*/
	public void crossValidate(int n_folds, TweetVector[] trainingTweetVectors, String pathToResultsFileBase, String nullClass, double[] confidenceThresholds, int batchSize) throws IOException, InterruptedException, ClassNotFoundException {
		ArrayList<ArrayList<Hashtable<String, Hashtable<String, Double>>>> resultsOverTrialsForAllThresholds = new ArrayList<ArrayList<Hashtable<String, Hashtable<String, Double>>>>();
		//add base results containers for each confidence threshold
		for (int i = 0; i < confidenceThresholds.length; i++) {
			resultsOverTrialsForAllThresholds.add(new ArrayList<Hashtable<String, Hashtable<String, Double>>>(n_folds));
		}

		//get folds
		double[] proportions = new double[n_folds];
		double unit = ((double)1)/n_folds;
		for (int i = 0; i < n_folds; i++) {
			proportions[i] = unit;
		}
		ArrayList<ArrayList<TweetVector>> folds = split(trainingTweetVectors, proportions);

		//give each fold a turn to be the test fold, each test fold is one trial
		for (int i = 0; i < folds.size(); i++) {
			//initialize training and testing fields
			ArrayList<TweetVector> trainingTweets = new ArrayList<TweetVector>();
			ArrayList<TweetVector> testingTweets = folds.get(i);
			for (int j = 0; j < folds.size(); j++) {
				if (j != i) {
					ArrayList<TweetVector> currentFold = folds.get(j);
					for (TweetVector currentVector: currentFold) {
						//make an instance and add it to the list
						trainingTweets.add(currentVector);
					}
				}
			}

			//new training session, clear the classifier, initialize the alphabets (which also initializes the instances)
			clearClassifier();
			initializeAlphabets(trainingTweets);

			//train using the training folds
			//train using the training folds
			for (int j = 0; j < trainingTweets.size(); j++) {
				addToInstanceListAndTrainBatchwise(trainingTweets.get(j), batchSize);
			}
			//train any incomplete batches
			train();

			//test using the testing fold, one round for each threshold
			for (int j = 0; j < confidenceThresholds.length; j++) {
				//get info for the current threshold
				double currentThreshold = confidenceThresholds[j];
				ArrayList<Hashtable<String, Hashtable<String, Double>>> resultsAllTrialsCurrentThreshold = resultsOverTrialsForAllThresholds.get(j);
				int stoppingPoint = pathToResultsFileBase.length() - 4;
				String currentPathToResultsFile = pathToResultsFileBase.substring(0, stoppingPoint) + currentThreshold + pathToResultsFileBase.substring(stoppingPoint);

				//get results
				Hashtable<String, Hashtable<String, Double>> resultsOneTrialCurrentThreshold = evaluateWithConfThreshold(testingTweets.toArray(new TweetVector[testingTweets.size()]), nullClass, currentThreshold);
				//write out the results for the current trial for this threshold
				if (i == 0) {
					writeTestResultsToFile(resultsOneTrialCurrentThreshold, 1, currentPathToResultsFile, false);
				}
				else {
					writeTestResultsToFile(resultsOneTrialCurrentThreshold, 1, currentPathToResultsFile, true);
				}
				//add to cumulative results
				resultsAllTrialsCurrentThreshold.add(resultsOneTrialCurrentThreshold);
			}

		}
		//write out the cumulative results for all thresholds over all trials
		for (int j = 0; j < confidenceThresholds.length; j++) {
			double currentThreshold = confidenceThresholds[j];
			int stoppingPoint = pathToResultsFileBase.length() - 4;
			String currentPathToResultsFile = pathToResultsFileBase.substring(0, stoppingPoint) + currentThreshold + pathToResultsFileBase.substring(stoppingPoint);
			writeTestResultsToFile(averageTrialResults(resultsOverTrialsForAllThresholds.get(j)), n_folds, currentPathToResultsFile, true);
		}

		//clear everything because this is a train-test framework
		//clearAlphabets();
		clearClassifier();
		clearInstances();

		/*
		CrossValidationIterator crossValidationIterator = new CrossValidationIterator(trainingInstances, n_folds, new Randoms());
		//add base results containers for each confidence threshold
		for (int i = 0; i < confidenceThresholds.length; i++) {
			resultsOverTrialsForAllThresholds.add(new ArrayList<Hashtable<String, Hashtable<String, Double>>>(n_folds));
		}

		//run a number of trials equal to the number of folds
		while (crossValidationIterator.hasNext()) {
			InstanceList[] split = crossValidationIterator.next();
			InstanceList training = split[0];
			InstanceList testing = split[1];

			trainClassifier(training, true);
			saveClassifier(classifierFile);
			System.out.println();
			System.out.println("NEW TEST:");

			//run an evaluation round for each confidence threshold
			for (int i = 0; i < confidenceThresholds.length; i++) {
				ArrayList<Hashtable<String, Hashtable<String, Double>>> resultsOverTrialsThisThreshold = resultsOverTrialsForAllThresholds.get(i);
				double thisConfidenceThreshold = confidenceThresholds[i];
				int stoppingPoint = pathToResultsFileBase.length() - 4;
				String thisPathToResultsFile = pathToResultsFileBase.substring(0, stoppingPoint) + thisConfidenceThreshold + pathToResultsFileBase.substring(stoppingPoint);

				//resultsOverTrialsThisThreshold.add(evaluateWithConfThreshold(testing, nullClass, thisConfidenceThreshold));
				writeTestResultsToFile(resultsOverTrialsThisThreshold.get(resultsOverTrialsThisThreshold.size() - 1), 1, thisPathToResultsFile, true);
			}
		}
		//print out results for each confidence threshold
		for (int i = 0; i < confidenceThresholds.length; i++) {
			ArrayList<Hashtable<String, Hashtable<String, Double>>> resultsOverTrialsThisThreshold = resultsOverTrialsForAllThresholds.get(i);
			double thisConfidenceThreshold = confidenceThresholds[i];
			int stoppingPoint = pathToResultsFileBase.length() - 4;
			String thisPathToResultsFile = pathToResultsFileBase.substring(0, stoppingPoint) + thisConfidenceThreshold + pathToResultsFileBase.substring(stoppingPoint);

			//printTestResults(averageTrialResults(resultsOverTrials), n_folds);
			writeTestResultsToFile(averageTrialResults(resultsOverTrialsThisThreshold), n_folds, thisPathToResultsFile, true);
		}
		*/
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
	 *
	 *  NOTE: Recall, and thus F1, for the "person" class may be incorrect here, as will precision, recall, and
		F1 for the "organization" class
	 * */
	/*
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
	*/

	public Hashtable<String, Hashtable<String, Double>> evaluate(InstanceList testInstances) throws IOException {
		Hashtable<String, Hashtable<String, Double>> output = new Hashtable<String, Hashtable<String, Double>>();
		// Create an InstanceList that will contain the test data.
		// In order to ensure compatibility, process instances
		// with the pipe used to process the original training
		// instances.
		Trial trial = new Trial(maxEntClassifier, testInstances);

		//getAreaUnderCurve(trial);

		//printLabelings(testInstances);
		// System.out.println();
		//PrintWriter p = new PrintWriter("data/featureWeights.txt");
		// p.write("\n");
		((MaxEnt) maxEntClassifier).print();
		//((NaiveBayes) maxEntClassifier).print();
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
    Returns a hashtable containing accuracy and performance metrics for the given test instances. Requires that each instance
    is classified with a confidence above the specified threshold, or else it will be classified as the nullClass.
*/
	public Hashtable<String, Hashtable<String, Double>> evaluateWithConfThreshold(TweetVector[] testTweetVectors, String nullClass, double confThreshold) throws IOException, InterruptedException, ClassNotFoundException {
		//get classification figures and calculate performance metrics from them
		Hashtable<String, Hashtable<String, Integer>> classificationResults = getClassificationFigures(testTweetVectors, confThreshold, nullClass);
		int instancesOfClass;
		int classifiedAsClass;
		int correctlyClassifiedAsClass;
		double precision;
		double recall;
		double F1;
		Hashtable<String, Double> accuracy = new Hashtable<String, Double>();

		Hashtable<String, Hashtable<String, Double>> results = new Hashtable<String, Hashtable<String, Double>>();
		Enumeration<String> classes = classificationResults.keys();
		while (classes.hasMoreElements()) {
			String currClass = classes.nextElement();

			//get figures
			Hashtable<String, Integer> figuresForClass = classificationResults.get(currClass);
			instancesOfClass = figuresForClass.get("instances");
			classifiedAsClass = figuresForClass.get("classified as");
			correctlyClassifiedAsClass = figuresForClass.get("correctly classified as");

			//update accuracy, as this takes into account all classes
			if (accuracy.size() == 0) { //initialize if this is the first class
				accuracy.put("Accuracy", (double)correctlyClassifiedAsClass);
			}
			else { //otherwise, update
				accuracy.put("Accuracy", ((double)correctlyClassifiedAsClass) + accuracy.get("Accuracy"));
			}

			Hashtable<String, Double> metrics = new Hashtable<String, Double>();
			if (classifiedAsClass > 0) {
				precision = ((double) correctlyClassifiedAsClass) / classifiedAsClass;
			}
			else {
				precision = 0.0;
			}
			if (instancesOfClass > 0) {
				recall = ((double) correctlyClassifiedAsClass) / instancesOfClass;
			}
			else {
				recall = 0.0;
			}
			F1 = (2 * precision * recall) / (precision + recall);
			if (precision == 0.0 && recall == 0.0) {
				F1 = 0.0;
			}
			metrics.put("Precision", precision);
			metrics.put("Recall", recall);
			metrics.put("F1", F1);
			results.put(currClass, metrics);

			/*
			System.out.println("ORIGINAL: "+testInstances.size()+ " TRIMMED: "+trimmedTestInstances.size());

			//test run using the trimmed instance list
			Hashtable<String, Hashtable<String, Double>> resultsOfTrial = testRun(trimmedTestInstances);
			testRun(trimmedTestInstances);
			*/

		}
		//finally get accuracy
		accuracy.put("Accuracy", accuracy.get("Accuracy")/testTweetVectors.length);
		results.put("Accuracy", accuracy);

		return results;
	}

	public void getAreaUnderCurve(Trial t) {
		AccuracyCoverage a = new AccuracyCoverage(t, "AUC", "Labelings");

		a.displayGraph();
		System.out.println(a.cumulativeAccuracy());
	}

	public Hashtable<String, Hashtable<String, Integer>> getClassificationFigures(TweetVector[] testTweetVectors, double confThreshold, String nullClass) {
		Hashtable<String, Hashtable<String, Integer>> results = new Hashtable<String, Hashtable<String, Integer>>();

		//Get the number of instances of each class, the number of times an instance is classified as each class,
		//and the number of times an instance is correctly classified as each class
		//for (int j = 0; j < testInstances.size(); j++) {
		for (TweetVector tweetVector: testTweetVectors) {
			//make an instance out of it
			//make an instance
			Instance instance = makeInstance(tweetVector);

			//get the correct label
			//String correctLabel = instance.getLabeling().toString();
			String correctLabel = instance.getTarget().toString();
			//get the label given by the classifier, unless the label is the specified class and its confidence
			//is below the threshold (in which case the label with the second highest confidence is chosen), or
			//the label is not the specified class, but the confidence for the specified class is above the threshold
			//(in which case the label is the specified class)
			//String experimentalLabel = classifier.maxEntClassifier.classify(instance).getLabeling().getLabelAtRank(0).toString();
			String experimentalLabel = getLabelConfThresholdForDesiredClass(instance, nullClass, confThreshold);

			//initialize fields if necessary, set all figures to 0
			if (!results.containsKey(correctLabel)) {
				Hashtable<String, Integer> figures = new Hashtable<String, Integer>();
				figures.put("instances", 0);
				figures.put("classified as", 0);
				figures.put("correctly classified as", 0);
				results.put(correctLabel, figures);
			}
			if (!results.containsKey(experimentalLabel)) {
				Hashtable<String, Integer> figures = new Hashtable<String, Integer>();
				figures.put("instances", 0);
				figures.put("classified as", 0);
				figures.put("correctly classified as", 0);
				results.put(experimentalLabel, figures);
			}

			//update fields
			Hashtable<String, Integer> correctFigures = results.get(correctLabel);
			Hashtable<String, Integer> experimentalFigures = results.get(experimentalLabel);
			//add one instance to the correct label
			correctFigures.put("instances", correctFigures.get("instances") + 1);
			if (correctLabel.equals(experimentalLabel)) {
				//add one "classified as" and one "correctly classified as" to the correct label
				correctFigures.put("classified as", correctFigures.get("classified as") + 1);
				correctFigures.put("correctly classified as", correctFigures.get("correctly classified as") + 1);
			}
			else {
				//add one "classified as" to the experimental label
				experimentalFigures.put("classified as", experimentalFigures.get("classified as") + 1);
			}
			results.put(correctLabel, correctFigures);
			results.put(experimentalLabel, experimentalFigures);

		}

		return results;
	}

	/*
    Labels a given Instance as desiredClass if the classifier's confidence rating for that class meets or exceeds
    the confidence threshold. If the confidence rating for desiredClass is below the threshold but the Instance
    is labeled as desiredClass, label the Instance with the highest-confidence class that isn't desiredClass.
	*
	public String getLabelConfThresholdForDesiredClass(Instance instance, String desiredClass, double conf) {
		Labeling labeling = maxEntClassifier.classify(instance).getLabeling();
		for (int rank = 0; rank < labeling.numLocations(); rank++) {
			//find the confidence level for desiredClass
			if (labeling.getLabelAtRank(rank).toString().equals(desiredClass)) {
				//if it exceeds the threshold, label the instance as the desired class
				if (labeling.getValueAtRank(rank) >= conf) {
					return desiredClass;
				}
				//if it doesn't meet the threshold but is classified as the desired class anyway, label the instance
				//with a different class
				else if (rank == 0){
					return labeling.getLabelAtRank(1).toString();
				}
				else {
					return labeling.getLabelAtRank(0).toString();
				}
			}

					/*
					if (labeling.getValueAtRank(rank) >= confidenceThreshold) {
						//trimmedTestInstances.add(testInstances.get(i));
					}
					*
		}
		//by default, return the label returned by the classifier
		return labeling.getLabelAtRank(0).toString();
	}
	*/

	/*
		Labels a given Instance. If the classifier's suggested labeling is made with a confidence
		level below the threshold (conf), instead label the Instance as the nullClass
	*/
	public String getLabelConfThresholdForDesiredClass(Instance instance, String nullClass, double conf) {
		Labeling labeling = maxEntClassifier.classify(instance).getLabeling();
		if (labeling.getValueAtRank(0) >= conf) {
			return labeling.getLabelAtRank(0).toString();
		}
		else {
			return nullClass;
		}
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

	public void initializeAlphabets(ArrayList<TweetVector> trainingTweetVectors) {
		//clearAlphabets();
		for (TweetVector tweetVector: trainingTweetVectors) {
			addToInstanceList(tweetVector);
		}
		clearInstances();
	}

	public void initializeAlphabets(TweetVector[] trainingTweetVectors) {
		//clearAlphabets();
		for (TweetVector tweetVector: trainingTweetVectors) {
			addToInstanceList(tweetVector);
		}
		clearInstances();
	}

	public Classifier loadClassifier(File serializedFile)
			throws FileNotFoundException, IOException, ClassNotFoundException {

		// The standard way to save classifiers and Mallet data
		//  for repeated use is through Java serialization.
		// Here we load a serialized classifier from a file.

		Classifier classifier = null;

		try {
			ObjectInputStream ois =
					new ObjectInputStream(new FileInputStream(serializedFile));
			classifier = (Classifier) ois.readObject();
			ois.close();
		}
		catch(EOFException e) {

		}

		return classifier;
	}

	//makes an instance out of a TweetVector, using the existing data and label alphabets
	public Instance makeInstance(TweetVector tweetVector) {
		Hashtable<String, Double> table = tweetVector.getFeatures();
		String label = tweetVector.getLabel();
		String name = tweetVector.getName();

		Enumeration<String> features = table.keys();
		int numberOfNewFeatures = getNumOfNewFeatures(table);
		double[] featureValues = new double[dataAlphabet.size() + numberOfNewFeatures];

		while (features.hasMoreElements()) {
			String featureName = features.nextElement();
			featureValues[dataAlphabet.lookupIndex(featureName, true)] = table.get(featureName);
		}

		Instance instance = new Instance(new FeatureVector(dataAlphabet, featureValues), label, name, null);
		return instance;
	}

	/*
		Makes instances out of all tweets in a path-to-tweet file and trains the classifier on them, one instance
		at a time
	 */
	public void makeInstancesAndTrain(TweetVector[] tweetVectors, String classifierType, int batchSize) throws IOException, FileNotFoundException, InterruptedException {
		//for each tweet
		//set up Stanford CoreNLP object for annotation
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		//new training session, clear the classifier, the alphabets, and all instances. Initialize alphabets with the
		//training tweets
		clearAlphabets();
		clearClassifier();
		clearInstances();
		initializeAlphabets(tweetVectors);

		//train batchwise
		for (TweetVector tweetVector: tweetVectors) {
			addToInstanceListAndTrainBatchwise(tweetVector, batchSize);
		}
		//train any untrained batches
		train();
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

	/*
		Prints the results of a testRun calculation
	*/
	public void printTestResults(Hashtable<String, Hashtable<String, Double>> input, int nTrials) {
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


	/*
    	Runs n trials on the data, requiring that each instance be classified at or above the threshold confidence
    	level.
 	*/
	public void runNSplits(int n, TweetVector[] tweetVectors, String pathToResultsFile, String nullClass, double confThreshold, int batchSize) throws IOException, InterruptedException, ClassNotFoundException {
		ArrayList<Hashtable<String, Hashtable<String, Double>>> resultsOverTrials = new ArrayList<Hashtable<String, Hashtable<String, Double>>>(n);

		/*
		//build up data alphabet/label alphabet, but don't actually train the classifier yet
		//PROBABLY USELESS
		//also clear any classifier and instance list that may already be there
		clearInstances();
		clearClassifier();
		for (TweetVector tweetVector: tweetVectors) {
			addToInstanceList(tweetVector);
		}
		*/

		//get splits, and train/test a classifier on each
		for (int i = 0; i < n; i++) {
			//get splits
			ArrayList<ArrayList<TweetVector>> thisSplit = split(tweetVectors, new double[]{0.8, 0.2});
			TweetVector[] trainingTweetVectors = thisSplit.get(0).toArray(new TweetVector[1]);
			TweetVector[] testTweetVectors = thisSplit.get(1).toArray(new TweetVector[1]);

			//new training session, clear the classifier, initialize the alphabets (which also initializes the instances)
			clearClassifier();
			initializeAlphabets(trainingTweetVectors);

			//train classifier
			//train using the training tweets
			for (TweetVector trainingTweetVector: trainingTweetVectors) {
				//train batchwise
				addToInstanceListAndTrainBatchwise(trainingTweetVector, batchSize);
			}
			//train any untrained batches
			train();

			//test, save results
			Hashtable<String, Hashtable<String, Double>> results = evaluateWithConfThreshold(testTweetVectors, nullClass, confThreshold);
			resultsOverTrials.add(results);
			//write out the results for the current trial
			if (i == 0) {
				writeTestResultsToFile(results, 1, pathToResultsFile, false);
			}
			else {
				writeTestResultsToFile(results, 1, pathToResultsFile, true);
			}

		}
		//clear everything as this is a train-test framework
		//clearAlphabets();
		clearClassifier();
		clearInstances();

		writeTestResultsToFile(averageTrialResults(resultsOverTrials), n, pathToResultsFile, true);
	}

	/*
    	Runs n trials on the data
 	*/
	public void runNSplits(int n, TweetVector[] tweetVectors, String pathToResultsFile, int batchSize) throws IOException, InterruptedException, ClassNotFoundException {
		ArrayList<Hashtable<String, Hashtable<String, Double>>> resultsOverTrials = new ArrayList<Hashtable<String, Hashtable<String, Double>>>(n);

		//initialize new data and label alphabets
/*
		//build up data alphabet/label alphabet, but don't actually train the classifier yet
		//PROBABLY USELESS
		clearInstances();
		clearClassifier();
		for (TweetVector tweetVector: tweetVectors) {
			addToInstanceList(tweetVector);
		}
*/
		//get splits, and train/test a classifier on each
		for (int i = 0; i < n; i++) {
			//get splits
			ArrayList<ArrayList<TweetVector>> thisSplit = split(tweetVectors, new double[]{0.8, 0.2});
			TweetVector[] trainingTweetVectors = thisSplit.get(0).toArray(new TweetVector[1]);
			TweetVector[] testTweetVectors = thisSplit.get(1).toArray(new TweetVector[1]);

			//new training session, clear the classifier, initialize the alphabets (which also initializes the instances)
			clearClassifier();
			initializeAlphabets(trainingTweetVectors);

			//train classifier
			//train using the training tweets
			for (TweetVector trainingTweetVector: trainingTweetVectors) {
				//train batchwise
				addToInstanceListAndTrainBatchwise(trainingTweetVector, batchSize);
			}
			//train any untrained batches
			train();

			//test, save results
			Hashtable<String, Hashtable<String, Double>> results = evaluateWithConfThreshold(testTweetVectors, "null_class", 0.0);
			resultsOverTrials.add(results);
			//write out the results for the current trial
			if (i == 0) {
				writeTestResultsToFile(results, 1, pathToResultsFile, false);
			}
			else {
				writeTestResultsToFile(results, 1, pathToResultsFile, true);
			}
		}
		//clear everything, as this is a train-test framework
		//clearAlphabets();
		clearClassifier();
		clearInstances();

		writeTestResultsToFile(averageTrialResults(resultsOverTrials), n, pathToResultsFile, true);
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

	public void setTargetAlphabet(String target) {
		targetAlphabet.lookupIndex(target, true);
		targetAlphabet.lookupIndex("Not " + target + " class", true);
		targetAlphabet.stopGrowth();
	}

	/*
	public InstanceList split(InstanceList instances) throws IOException {
		try {
			int TRAINING = 0;
			int TESTING = 1;
			int VALIDATION = 2;

			// The division takes place by creating a copy of the list,
			//  randomly shuffling the copy, and then allocating
			//  instances to each sub-list based on the provided proportions.

/*		InstanceList[] instanceLists =
				instances.split(new Randoms(),
						new double[] {0.5, 0.5, 0.0});*
//better than 0.8, 0.2 split
			Randoms randoms = new Randoms();
			int cutoffPt = instances.size() * 4 / 5;
			ArrayList<Integer> arr = new ArrayList<Integer>();

			java.util.Random random = new Random();
			random.nextInt(instances.size());
			while (arr.size() < instances.size()) {
				int proposed = random.nextInt(instances.size());
				int seen = 0;
				for (Integer intY : arr) {
					if (intY == proposed) {
						break;
					}
					seen++;
				}
				if (seen == arr.size()) {
					arr.add(proposed);
				}
			}

			InstanceList newThingy = new InstanceList(dataAlphabet, targetAlphabet);
			//add everything just because
			for (int i = 0; i < instances.size(); i++) {
				newThingy.add(instances.get(i));
			}
			for (int i = 0; i < arr.size(); i++) {
				newThingy.set(i, instances.get(arr.get(i)));
			}
/*
		InstanceList[] instanceLists =
				instances.split(new Randoms(),
						new double[] {0.8, 0.2, 0.0});
						*
			instances = newThingy;
			InstanceList[] instanceLists = instances.splitInOrder(new double[]{0.8, 0.2, 0.0});
			//  The third position is for the "validation" set,
			//  which is a set of instances not used directly
			//  for training, but available for determining
			//  when to stop training and for estimating optimal
			//  settings of nuisance parameters.
			//  Most Mallet ClassifierTrainers can not currently take advantage
			//  of validation sets.
			this.instances = instanceLists[TRAINING];

			return instanceLists[TESTING];
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}*/

	/*
		Splits an array of tweet vectors based on the given proportions
	 */
	private ArrayList<ArrayList<TweetVector>> split(TweetVector[] allVectors, double[] proportions) {
		//make sure the proportions add up to 1.0
		double sum = 0.0;
		for (double proportion: proportions) {
			sum += proportion;
		}
		if (Math.abs(sum - 1.0) >= 0.0001) {
			System.err.println("ERROR in MaxEntClassification.split: values in double[] proportions do not add up to 1.0");
			System.exit(1);
		}
		ArrayList<ArrayList<TweetVector>> splitVectors = new ArrayList<ArrayList<TweetVector>>();

		//shuffle the array
		util.shuffleTweetVectors(allVectors);

		//divide up the array based on the given proportions
		int lastEnd = 0;
		int newEnd;
		for (int i = 0; i < proportions.length; i++) {
			ArrayList<TweetVector> thisSection = new ArrayList<TweetVector>();

			//if it's the last set of proportions, make sure everything is added to it
			if (i == proportions.length - 1) {
				newEnd = allVectors.length;
			}
			else {
				int tweetsForThisSection = (int)(proportions[i] * allVectors.length);
				newEnd = lastEnd + tweetsForThisSection;
			}

			//add the necessary vectors
			for (int j = lastEnd; j < newEnd; j++) {
				thisSection.add(allVectors[j]);
			}

			//update
			lastEnd = newEnd;
			splitVectors.add(thisSection);
		}

		return splitVectors;
	}


	/*
	public Hashtable<String, Hashtable<String, Double>> testRun(InstanceList testInstances) throws IOException {
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
	}*/

	/*
    	Returns a hashtable containing accuracy and performance metrics for the given test instances. Multithreads to
    	improve speed
    */
	/*
	public Hashtable<String, Hashtable<String, Double>> testRun(InstanceList testInstances, String desiredClass, double confThreshold) throws IOException, InterruptedException, ClassNotFoundException {
		Hashtable<String, Hashtable<String, Double>> results = new Hashtable<String, Hashtable<String, Double>>();

		Hashtable<String, Hashtable<String, Integer>> figuresFromThreads = new Hashtable<String, Hashtable<String, Integer>>();

		//split the task into several threads, each classifying a sub-section of the test instances
		ArrayList<MulticlassMaxEntTestThread> threads = new ArrayList<MulticlassMaxEntTestThread>();

		//split the InstanceList
		double coreProportion = 1.0 / nCores;
		double[] proportions = new double[nCores];
		for (int i = 0; i < nCores; i++) {
			proportions[i] = coreProportion;
		}
		InstanceList[] sections = testInstances.split(new Randoms(), proportions);

		//create and run threads
		for (InstanceList list: sections) {
			InstanceList newList = new InstanceList(list.getDataAlphabet(), list.getTargetAlphabet());
			for (Instance i: list) {
				newList.add(i);
			}

			MulticlassMaxEntTestThread thread = new MulticlassMaxEntTestThread("thread", newList, new MaxEntClassification(classifierFile, nCores), desiredClass, confThreshold);
			threads.add(thread);
			thread.start();
		}

		/*
		int unit = testInstances.size() / nCores;
		int lastStart = 0;
		int lastEnd = unit;
		for (int i = 0; i < nCores; i++) {
			//since the instances may not be exactly divisible into nCores sections, put all remainders into the last thread
			if (i == nCores - 1) {
				lastEnd = testInstances.size();
			}

			//create the next thread
			InstanceList thisThread = new InstanceList()

			//set the pointers for the next thread
			lastStart = lastEnd;
			lastEnd += unit;
		}

		for (int i = unit; i < testInstances.size(); i += unit) {

			lastStart = i;
		}
		*

		//run the various threads, wait for them to finish, and collect their data
		for (MulticlassMaxEntTestThread thread: threads) {
			thread.thread.join();

			//each thread contains figures for various classes; add them to the cumulative figures in figuresFromThreads
			Enumeration<String> classes = thread.results.keys();
			while (classes.hasMoreElements()) {
				//add figures from each class in the thread's figures. If the cumulative does not have this class,
				//initialize an entry for this class's figures. Otherwise, add to the existing entry
				String currClass = classes.nextElement();

				//If the cumulative does not have this class, initialize an entry for this class's figures.
				Hashtable<String, Integer> currClassFigures = thread.results.get(currClass);
				if (!figuresFromThreads.containsKey(currClass)) {
					figuresFromThreads.put(currClass, currClassFigures);
				}
				//Otherwise, add to the existing entry
				else {
					//get the cumulative figure for this class
					Hashtable<String, Integer> cumulativeFiguresForCurrClass = figuresFromThreads.get(currClass);

					//add all values in currClassFigures to those in cumulativeFiguresForCurrClass
					Enumeration<String> figureNames = currClassFigures.keys();
					while(figureNames.hasMoreElements()) {
						String currFig = figureNames.nextElement();
						int newCumulativeValue = cumulativeFiguresForCurrClass.get(currFig) + currClassFigures.get(currFig);
						cumulativeFiguresForCurrClass.put(currFig, newCumulativeValue);
					}
				}
			}
		}

		//calculate performance metrics from figures for each class
		int instancesOfClass;
		int classifiedAsClass;
		int correctlyClassifiedAsClass;
		double precision;
		double recall;
		double F1;
		Hashtable<String, Double> accuracy = new Hashtable<String, Double>();

		//ACCURACY
		/*
		Hashtable<String, Double> accuracy = new Hashtable<String, Double>();
		accuracy.put("Accuracy", (((double)correctlyClassifiedAsDesired) + correctlyClassifiedAsAlt)/testInstances.size());
		results.put("Accuracy", accuracy);
		*

		Enumeration<String> classes = figuresFromThreads.keys();
		while (classes.hasMoreElements()) {
			String currClass = classes.nextElement();

			//get figures
			Hashtable<String, Integer> figuresForClass = figuresFromThreads.get(currClass);
			instancesOfClass = figuresForClass.get("instances");
			classifiedAsClass = figuresForClass.get("classified as");
			correctlyClassifiedAsClass = figuresForClass.get("correctly classified as");

			//update accuracy, as this takes into account all classes
			if (accuracy.size() == 0) { //initialize if this is the first class
				accuracy.put("Accuracy", (double)correctlyClassifiedAsClass);
			}
			else { //otherwise, update
				accuracy.put("Accuracy", ((double)correctlyClassifiedAsClass) + accuracy.get("Accuracy"));
			}

			Hashtable<String, Double> metrics = new Hashtable<String, Double>();
			if (classifiedAsClass > 0) {
				precision = ((double) correctlyClassifiedAsClass) / classifiedAsClass;
			}
			else {
				precision = 0.0;
			}
			if (instancesOfClass > 0) {
				recall = ((double) correctlyClassifiedAsClass) / instancesOfClass;
			}
			else {
				recall = 0.0;
			}
			F1 = (2 * precision * recall) / (precision + recall);
			if (precision == 0.0 && recall == 0.0) {
				F1 = 0.0;
			}
			metrics.put("Precision", precision);
			metrics.put("Recall", recall);
			metrics.put("F1", F1);
			results.put(currClass, metrics);

			/*
			System.out.println("ORIGINAL: "+testInstances.size()+ " TRIMMED: "+trimmedTestInstances.size());

			//test run using the trimmed instance list
			Hashtable<String, Hashtable<String, Double>> resultsOfTrial = testRun(trimmedTestInstances);
			testRun(trimmedTestInstances);
			*

		}
		//finally get accuracy
		accuracy.put("Accuracy", accuracy.get("Accuracy")/testInstances.size());
		results.put("Accuracy", accuracy);

		return results;
	}
	*/

	/*
	//trains the classifier using the given instances. Continues training an existing classifier if it is already there,
	//if trainOld is set to true
	public Classifier trainClassifier(InstanceList trainingInstances, boolean trainOld) {
		// Here we use a maximum entropy (ie polytomous logistic regression)
		//  classifier. Mallet includes a wide variety of classification
		//  algorithms, see the JavaDoc API for details.
		if (maxEntClassifier == null || !trainOld) {
			//trainer = new MaxEntTrainer();
			//maxEntClassifier = trainer.train(trainingInstances);
		}
		else {
			//ClassifierTrainer<MaxEnt> trainer = new MaxEntTrainer(maxEntClassifier);
			//maxEntClassifier = trainer.train(trainingInstances);
		}

		return maxEntClassifier;
	}
	*/

	//trains the classifier using the given trainingInstances. Continues training an existing classifier if it is
	//already there. Also gets rid of the current training instances
	private Classifier train() {
		//InstanceList instanceList = new InstanceList(dataAlphabet, targetAlphabet);
		//addToInstanceList(instance, instanceList);

		if (maxEntClassifier == null) {
			//trainer = new NaiveBayesTrainer();
			trainer = new MaxEntTrainer();
			//maxEntClassifier = trainer.train(trainingInstances);
		}
		//else {
			//ClassifierTrainer<MaxEnt> trainer = new MaxEntTrainer(maxEntClassifier);
			//maxEntClassifier = trainer.train(trainingInstances);
		//}
		maxEntClassifier = trainer.train(trainingInstances);
		clearInstances();
		return maxEntClassifier;
	}

	public void writeTestResultsToFile(Hashtable<String, Hashtable<String, Double>> input, int nTrials, String path, boolean append) throws IOException {
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(path), append));

		bufferedWriter.newLine();
		bufferedWriter.write("Average results for "+nTrials+" trials:");
		bufferedWriter.newLine();

		bufferedWriter.write("--------------------");
		bufferedWriter.newLine();
		bufferedWriter.write("ACCURACY: " + input.get("Accuracy").get("Accuracy"));
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

}

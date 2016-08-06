import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;
import java.util.Hashtable;

/**
 * Created by Alec Wolyniec on 7/20/16.
 */
public class MulticlassMaxEntTestThread implements Runnable {
    public Thread thread;
    private String threadName;

    //parameters
    InstanceList testInstances;
    MaxEntClassification classifier;
    //results
    Hashtable<String, Hashtable<String, Integer>> results;
    String nullClass;
    double confThreshold;

    MulticlassMaxEntTestThread(String name, InstanceList test, MaxEntClassification classy, String nullC, double conf) {
        threadName = name;
        testInstances = test;
        classifier = classy;
        //System.out.println("Creating thread "+threadName);
        results = new Hashtable<String, Hashtable<String, Integer>>();
        nullClass = nullC;
        confThreshold = conf;
    }

    public void run() {
        //System.out.println("Running thread "+threadName);
        try {
            //Get the number of instances of each class, the number of times an instance is classified as each class,
            //and the number of times an instance is correctly classified as each class
            for (int j = 0; j < testInstances.size(); j++) {
                Instance instance = testInstances.get(j);

                //get the correct label
                String correctLabel = instance.getLabeling().toString();
                //get the label given by the classifier, unless the label is the specified class and its confidence
                //is below the threshold (in which case the label with the second highest confidence is chosen), or
                //the label is not the specified class, but the confidence for the specified class is above the threshold
                //(in which case the label is the specified class)
                //String experimentalLabel = classifier.maxEntClassifier.classify(instance).getLabeling().getLabelAtRank(0).toString();
                String experimentalLabel = classifier.getLabelConfThresholdForDesiredClass(instance, nullClass, confThreshold);

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

            Thread.sleep(1);

        } catch(InterruptedException e) {
            System.out.println("Thread "+threadName+" interrupted");
        } /*catch(IOException e) {
            System.out.println("IOException");
        }*/
        System.out.println("Thread "+threadName+" exiting");
    }

    public void start() {
        //System.out.println("Starting thread "+threadName);
        if (thread == null) {
            thread = new Thread(this, threadName);
            thread.start();
        }
    }
}

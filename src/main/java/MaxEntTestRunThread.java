import java.util.Hashtable;

import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import org.apache.commons.math.stat.descriptive.rank.Max;
import java.io.IOException;

/**
 * Created by Alec Wolyniec on 7/18/16.
 */
public class MaxEntTestRunThread implements Runnable {
    public Thread thread;
    private String threadName;

    //parameters
    InstanceList testInstances;
    MaxEntClassification classifier;
    String desiredClass;
    String altClass;
    double confidenceThreshold;

    //test results
    public int desiredInstances = 0;
    public int altInstances = 0;
    public int classifiedAsDesired = 0;
    public int classifiedAsAlt = 0;
    public int correctlyClassifiedAsDesired = 0;
    public int correctlyClassifiedAsAlt = 0;

    MaxEntTestRunThread(String name, InstanceList test, MaxEntClassification classy, String desired, double conf, String alt) {
        threadName = name;
        testInstances = test;
        classifier = classy;
        //System.out.println("Creating thread "+threadName);
        desiredClass = desired;
        confidenceThreshold = conf;
        altClass = alt;
    }

    public void run() {
        //System.out.println("Running thread "+threadName);
        try {

            //if the confidence with which an instance is classified as "person" meets or exceeds the threshold,
            //label it as "person". Otherwise, label it as "organization". Calculate the accuracy, recall, precision, and
            //F1 of the trial from this data
            for (int j = 0; j < testInstances.size(); j++) {
                Instance instance = testInstances.get(j);

                //get the correct label
                String correctLabel = instance.getLabeling().toString();

                //get the label given by the classifier
                String experimentalLabel = classifier.getLabelForInstanceIfThresholdMet(instance, desiredClass, confidenceThreshold);

                //get inferences from this data
                if (correctLabel.equals(desiredClass)) {
                    desiredInstances++;

                    if (experimentalLabel.equals(desiredClass)) {
                        correctlyClassifiedAsDesired++;
                        classifiedAsDesired++;
                    }
                    else {
                        classifiedAsAlt++;
                    }
                }
                else if (correctLabel.equals(altClass)) {
                    altInstances++;

                    if (experimentalLabel.equals(altClass)) {
                        correctlyClassifiedAsAlt++;
                        classifiedAsAlt++;
                    }
                    else {
                        classifiedAsDesired++;
                    }
                }
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

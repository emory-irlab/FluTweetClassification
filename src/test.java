import org.apache.commons.math.stat.descriptive.rank.Max;

import javax.xml.soap.Text;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 * Created by Alec Wolyniec on 4/17/16.
 */
public class test {

    // [ { Accuracy: {Accuracy: 0}, Eggman: {Precision: 0, Recall: 0}, Sonic: {Precision: 0, Recall: 0} }, { Accuracy: {Accuracy: 1}, Eggman: {Precision: 1, Recall: 1}, Sonic: {Precision: 1, Recall: 1} }, {Accuracy: {Accuracy: 0.5}, Eggman: {Precision: 3, Recall: 3}, Sonic: {Precision: 3, Recall: 3} } ]

    // { Accuracy: {Accuracy: 0.5}, Eggman: {Precision: 4/3, Recall: 4/3}, Sonic: {Precision: 4/3, Recall: 4/3} }

    public static void main (String[] args) {
        /*
        ArrayList<Hashtable<String, Hashtable<String, Double>>> data = new ArrayList<Hashtable<String, Hashtable<String, Double>>>();

        Hashtable<String, Hashtable<String, Double>> t1 = new Hashtable<String, Hashtable<String, Double>>();

        Hashtable<String, Double> acc = new Hashtable<String, Double>();
        acc.put("Accuracy", 0.0);
        t1.put("Accuracy", acc);

        Hashtable<String, Double> egg = new Hashtable<String, Double>();
        egg.put("Precision", 0.0);
        egg.put("Recall", 0.0);
        t1.put("Eggman", egg);

        Hashtable<String, Double> son = new Hashtable<String, Double>();
        son.put("Precision", 0.0);
        son.put("Recall", 0.0);
        t1.put("Sonic", son);

        data.add(t1);


        Hashtable<String, Hashtable<String, Double>> t2 = new Hashtable<String, Hashtable<String, Double>>();

        acc = new Hashtable<String, Double>();
        acc.put("Accuracy", 1.0);
        t2.put("Accuracy", acc);

        egg = new Hashtable<String, Double>();
        egg.put("Precision", 1.0);
        egg.put("Recall", 1.0);
        t2.put("Eggman", egg);

        son = new Hashtable<String, Double>();
        son.put("Precision", 1.0);
        son.put("Recall", 1.0);
        t2.put("Sonic", son);

        data.add(t2);


        Hashtable<String, Hashtable<String, Double>> t3 = new Hashtable<String, Hashtable<String, Double>>();

        acc = new Hashtable<String, Double>();
        acc.put("Accuracy", 0.5);
        t3.put("Accuracy", acc);

        egg = new Hashtable<String, Double>();
        egg.put("Precision", 3.0);
        egg.put("Recall", 3.0);
        t3.put("Eggman", egg);

        son = new Hashtable<String, Double>();
        son.put("Precision", 3.0);
        son.put("Recall", 3.0);
        t3.put("Sonic", son);

        data.add(t3);

        MaxEntClassification.printEvaluated(MaxEntClassification.averageTrialResults(data), 3);
        */
    }
}


import org.apache.commons.math.stat.descriptive.rank.Max;
import java.io.*;

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

    public static void main (String[] args) throws IOException {
        String text = "I #like potatoes # and #furthermore...";
        System.out.println(TextFeatures.countInstancesOf(text, TextFeatures.hashtagPattern));
    }
}


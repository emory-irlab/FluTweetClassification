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
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(args[0])));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(args[1])));

        String currentLine;
        ArrayList<String> currentLineFields;
        String currentSplitField;
        while ((currentLine = bufferedReader.readLine()) != null) {
            currentLineFields = new ArrayList<String>();
            String[] currentCommaSplit = currentLine.split(",");
            boolean withinSplitField = false;

            currentSplitField = "";
            for (String split: currentCommaSplit) {
                //if the field begins with \", activate withinSplitField and add to the split fields
                //otherwise, if withinSplitField is active, add to the split fields, and if the field ends with \", deactivate withinSplitField and add the former split field to currentLineFields
                if (( split.length() > 1 && split.substring(0, 2).equals("\\\"") ) || withinSplitField) {
                    withinSplitField = true;
                    currentSplitField += split.replace("\\\"", "");
                    if (split.length() > 1 && split.substring(split.length() - 2).equals("\\\"")) {
                        withinSplitField = false;
                        currentLineFields.add(currentSplitField);
                    }
                }
                //otherwise, add to currentLineFields and re-initialize currentSplitField
                else {
                    currentLineFields.add(split);
                    currentSplitField = "";
                }
            }

            //write the fields out in tsv format
            if (currentLineFields.size() != 6) {
                System.out.println("wot, length is "+currentLineFields.size());
            }
            for (String field: currentLineFields) {
                bufferedWriter.write(field+"\t");
            }
            bufferedWriter.newLine();
        }

        bufferedWriter.close();
    }
}


import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Created by Alec Wolyniec on 9/9/16.
 */
public class splitFile {
    public static void main(String[] args) throws FileNotFoundException, IOException {
        File inputFile = new File(args[0]);
        int numFiles = Integer.parseInt(args[1]);
        int numRecordsInInputFile = 0;

        //get the number of lines in the input file
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(reader);
        for (CSVRecord record: records) {
            numRecordsInInputFile++;
        }
        reader.close();

        //create the unit
        int outputFileLineUnit = numRecordsInInputFile / numFiles;
        int lastStart = 0;

        //create the file segmentation
        ArrayList<Integer[]> startEndIndices = new ArrayList<Integer[]>();
        for (int i = 0; i < numFiles; i++) {
            Integer[] startEndForThisFile = new Integer[2];
            startEndForThisFile[0] = lastStart;
            if (i == numFiles - 1) {
                startEndForThisFile[1] = numRecordsInInputFile;
            }
            else {
                startEndForThisFile[1] = lastStart + outputFileLineUnit;
            }
            startEndIndices.add(startEndForThisFile);

            //increase the count
            lastStart += outputFileLineUnit;
        }

        //start collecting input lines at the first file
        int currentOutputFileNumber = 0;
        File currentOutputFile = new File(args[0].replace(".csv", "")+currentOutputFileNumber+".csv");
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(currentOutputFile));
        CSVPrinter printer = new CSVPrinter(bufferedWriter, CSVFormat.RFC4180);
        //the range of indices to collect for the currentFile
        Integer[] startEndIndicesCurrentFile = startEndIndices.get(0);

        //print the lines in the input file to the various output files
        reader = new BufferedReader(new FileReader(inputFile));
        records = CSVFormat.RFC4180.parse(reader);
        int currentLine = 0;
        for (CSVRecord record: records) {
            //if this line is not within the range for the current file, move to the next file
            if (currentLine >= startEndIndicesCurrentFile[1]) {
                currentOutputFileNumber++;
                currentOutputFile = new File(args[0].replace(".csv", "")+currentOutputFileNumber+".csv");
                bufferedWriter.close();
                bufferedWriter = new BufferedWriter(new FileWriter(currentOutputFile));
                printer = new CSVPrinter(bufferedWriter, CSVFormat.RFC4180);
                startEndIndicesCurrentFile = startEndIndices.get(currentOutputFileNumber);
            }

            //collect the line
            for (int i = 0; i < record.size(); i++) {
                printer.print(record.get(i));
            }
            printer.println();

            //advance the line count
            currentLine++;
        }
        bufferedWriter.close();

    }
}

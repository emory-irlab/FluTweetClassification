import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Alec Wolyniec on 9/9/16.
 */
public class splitFile {
    public static void main(String[] args) throws FileNotFoundException, IOException {
        File inputFile = new File(args[0]);
        int numFiles = Integer.parseInt(args[1]);
        int numLinesInInputFile = 0;

        //get the number of lines in the input file
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        String currentLine;
        while ((currentLine = reader.readLine()) != null) {
            numLinesInInputFile++;
        }

        //create the unit
        int outputFileLineUnit = numLinesInInputFile / numFiles;
        int lastStart = 0;

        //create the file segmentation
        ArrayList<Integer[]> startEndIndices = new ArrayList<Integer[]>();
        for (int i = 0; i < numFiles; i++) {
            Integer[] startEndForThisFile = new Integer[2];
            startEndForThisFile[0] = lastStart;
            if (i == numFiles - 1) {
                startEndForThisFile[1] = numLinesInInputFile;
            }
            else {
                startEndForThisFile[1] = lastStart + outputFileLineUnit;
            }
            startEndIndices.add(startEndForThisFile);
        }

        //create output files and write to them
        for (int i = 0; i < numFiles; i++) {
            File outputFile = new File(args[0].replace(".txt", "")+i+".txt");
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile));

            //add lines to the new file
            reader = new BufferedReader(new FileReader(inputFile));
            int currentLineIndex = 0;

            //get the range of indices to collect
            Integer[] startEndPairHere = startEndIndices.get(i);
            while ((currentLine = reader.readLine()) != null) {
                //collect the line if it's in the range
                if (currentLineIndex < startEndPairHere[1]) {
                    bufferedWriter.write(currentLine);
                    bufferedWriter.newLine();
                }
                else {
                    break;
                }

                currentLineIndex++;
            }


        }
    }
}

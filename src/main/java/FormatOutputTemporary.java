import java.io.*;
import java.util.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 * Created by Alec Wolyniec on 7/29/16.
 */
public class FormatOutputTemporary {

    public static void integrateMissingTweets() throws FileNotFoundException, IOException {
        String[] nmz = {"X_itzPaco_", "ChristianMFH", "martelloomis", "DrewOtter", "Just_Malachi", "Origamigryphon",
        "ChrisNPI", "coolkidfthes", "lulukohime", "MikeeGavin_MCFC", "a__butter", "deceb7", "Lorilliluvsmj", "Baelizean",
        "baysil", "PaulHBeckwith"};
        ArrayList<String> usernames = new ArrayList<String>();
        for (String nm: nmz) {
            usernames.add(nm);
        }

        //go through the labeled file
        BufferedReader labeledReader = new BufferedReader(new FileReader(new File("data/labeled_historical_tweets/job_loss-self-labeled-historical.csv")));
        BufferedReader missingReader = new BufferedReader(new FileReader(new File("data/labeled_historical_tweets/job_loss-self-labeled-missing-historical.csv")));
        BufferedWriter out = new BufferedWriter(new FileWriter(new File("data/labeled_historical_tweets/job_loss-self-labeled-historical-FULL.csv")));

        CSVParser labeledCSV = new CSVParser(labeledReader, CSVFormat.RFC4180);
        List<CSVRecord> labeledData = labeledCSV.getRecords();

        CSVParser missingCSV = new CSVParser(missingReader, CSVFormat.RFC4180);
        List<CSVRecord> missingData = missingCSV.getRecords();

        CSVPrinter printer = new CSVPrinter(out, CSVFormat.RFC4180);

        //add each tweet to the new output file unless the username is on the list (in which case you locate the tweets
        //for that user from the LABELED missing tweets file and add those in their place)
        for (CSVRecord labeledLine: labeledData) {
            String username = labeledLine.get(0);
            //check to see if it matches one of the usernames
            boolean matched = false;
            for (String name: usernames) {
                if (name.equals(username)) {
                    matched = true;
                }
            }

            //branch point
            if (matched) { //username isn't on the list, so just print out the line
                for (int i = 0; i < labeledLine.size(); i++) {
                    System.out.println(labeledLine.get(i));
                }
            }
            //username is on the list - don't print out the line, print out all the user's missing tweets instead
            else {
                for (CSVRecord missingLine: missingData) {
                    String missingUsername = missingLine.get(0);
                    //found a missing tweet for this user
                    if (missingUsername.equals(username)) {
                        //print out all files
                        for (int i = 0; i < missingLine.size(); i++) {
                            System.out.println(missingLine.get(i));
                        }
                    }
                }

                //also remove the username from the list
                usernames.remove(username);
            }
        }

    }



    public static void main(String[] args) throws IOException {
        BufferedReader originalReader = new BufferedReader(new FileReader(new File("data/experimental_tweets/job_loss-self-output.csv")));
        BufferedReader allUserReader = new BufferedReader(new FileReader(new File("data/experimental_tweets/job_loss-self-HISTORICAL.csv")));
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File("data/labeled_historical_tweets/second-partial-job_loss-self-labeled-historical.csv")));

        CSVParser originalCSV = new CSVParser(originalReader, CSVFormat.RFC4180);
        List<CSVRecord> originalData = originalCSV.getRecords();

        CSVParser allUserCSV = new CSVParser(allUserReader, CSVFormat.RFC4180);
        List<CSVRecord> allUserData = allUserCSV.getRecords();

        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.RFC4180);

        int allUserTweetsIndex = 0;
        for (CSVRecord originalLine: originalData) {
            String username = originalLine.get(0);
            String date = originalLine.get(4);
            String type = originalLine.get(8);
            String SvOLabel = originalLine.get(9);
            String recentLabel = originalLine.get(11);

            if (allUserTweetsIndex >= allUserData.size()) {
                break;
            }
            CSVRecord historicalTweet = allUserData.get(allUserTweetsIndex);
            String historicalUsername = historicalTweet.get(0);

            //match
            while (historicalUsername.equals(username)) {
                String historicalText = historicalTweet.get(1);
                String historicalDate = historicalTweet.get(3);

                //write
                printer.print(username);
                printer.print(type);
                printer.print(SvOLabel);
                printer.print(recentLabel);
                printer.print(date);
                printer.print(historicalDate);
                printer.print(historicalText);
                printer.println();

                //onto the next tweet
                allUserTweetsIndex++;
                if (allUserTweetsIndex < allUserData.size()) {
                    historicalTweet = allUserData.get(allUserTweetsIndex);
                    historicalUsername = historicalTweet.get(0);
                }
                else {
                    break;
                }
            }
        }

        writer.close();
    }
}

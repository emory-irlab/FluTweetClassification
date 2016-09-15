import java.io.*;
import java.nio.Buffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Hashtable;
import java.util.Enumeration;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVPrinter;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/*
    From a file containing a tweet ID and a tweet label in every line, separated by tabs, produces a CSV file of tweets
 */
public class TweetParser {
	/*
	 * First arg is path to file with IDs
	 * Second arg is path where tweetFile should be saved
	 * 
	 * */

    /*
        Tweet format (csv):
        profile pic, handle, name, description, tweet, HvN label, EvN label, SvO label
        -data containing commas
     */

    static final String CONSUMER_KEY = "Yn2MuM201XbTBeX1lx8DiEYvA";
    static final String CONSUMER_SECRET = "1dIRD2WmDjlQB7ehT1SQ0VNM8kuo1DhFLRYg9JYCXw598wYIQJ";
    static final String TWITTER_TOKEN = "714776825885564928-Uqc6fVxpqsil3OqIHMQ9f8pkwaDQkzW";
    static final String TWITTER_TOKEN_SECRET = "PjQRk3RjdggBPwjaKMHb5aKgT6SOO2YUjDTtMWkmdAG8T";

    /*
    Get csv-formatted tweets from a path to a file
    Fields should be as follows: profile picture, handle, name, description, tweet, label, date, id, id, id
    */
    public static ArrayList<String[]> getTweets(String pathToTweetFile) throws FileNotFoundException, IOException {
        ArrayList<String[]> tweets = new ArrayList<String[]>();
        BufferedReader in = new BufferedReader(new FileReader(pathToTweetFile));
        Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
        for (CSVRecord record : records) {
            if (record.size() >= 5) {
                //set the size, a maximum of 9
                int entrySize;
                if (record.size() < 10) {
                    entrySize = record.size();
                }
                else {
                    entrySize = 9;
                }

                //collect the fields
                String[] tweetFields = new String[entrySize];
                for (int i = 0; i < entrySize; i++) {
                    tweetFields[i] = record.get(i);
                }
                tweets.add(tweetFields);
            }
        }
        return tweets;
    }

    public static void randomlySampleTweetsFromOneFileAndWriteToAnother(String fileToRead, String fileToWrite, int numTweetsToCollect) throws FileNotFoundException, IOException {
        int numTweetsInFile = 0;
        BufferedReader in = new BufferedReader(new FileReader(fileToRead));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileToWrite, true));
        CSVPrinter printer = new CSVPrinter(bufferedWriter, CSVFormat.RFC4180);
        Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);

        //get the number of tweets in the read file
        for (CSVRecord record : records) {
            if (record.size() >= 1) {
                numTweetsInFile++;
            }
        }

        //get the spacing needed to get the correct number of tweets
        int spacing = numTweetsInFile / numTweetsToCollect;
        System.out.println(spacing);

        //collect the tweets
        int counter = 0;
        in = new BufferedReader(new FileReader(fileToRead));
        records = CSVFormat.RFC4180.parse(in);
        for (CSVRecord record: records) {
            if (record.size() >= 1) {
                //time to collect
                if (counter == spacing) {
                    //collect
                    for (String field: record) {
                        printer.print(field);
                    }
                    printer.println();

                    //reset the counter
                    counter = 0;
                }

                counter++;
            }
        }
        in.close();
        printer.close();
    }

    public static ArrayList<String[]> getTweetsTimExamples(String pathToTweetFile) throws FileNotFoundException, IOException {
        ArrayList<String[]> tweets = new ArrayList<String[]>();
        BufferedReader in = new BufferedReader(new FileReader(pathToTweetFile));

        String currentLine;
        while ((currentLine = in.readLine()) != null) {
            String[] fields = currentLine.split("\\t");
            String[] tweetFields = new String[10];
            if (fields.length == 7) {
                tweetFields[0] = "";
                tweetFields[1] = fields[6];
                tweetFields[2] = fields[5];
                tweetFields[3] = "";
                tweetFields[4] = fields[0];
                tweetFields[5] = "";
                tweetFields[6] = fields[1];
                tweetFields[7] = fields[2];
                tweetFields[8] = fields[3];
                tweetFields[9] = fields[4];
            }
            else if (fields.length == 8) {
                tweetFields[0] = "";
                tweetFields[1] = fields[6];
                tweetFields[2] = fields[5];
                tweetFields[3] = fields[7];
                tweetFields[4] = fields[0];
                tweetFields[5] = "";
                tweetFields[6] = fields[1];
                tweetFields[7] = fields[2];
                tweetFields[8] = fields[3];
                tweetFields[9] = fields[4];
            }
            tweets.add(tweetFields);
        }
        return tweets;
    }

    /*
        Get profile pic links, usernames, names, descriptions, and tweets from a tsv file
    */
    public static ArrayList<String[]> getUnlabeledTweetEntitiesAndLabel(String pathToTSVFile, int column, boolean skipFirstLine, String label) throws FileNotFoundException, IOException {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(CONSUMER_KEY);
        builder.setOAuthConsumerSecret(CONSUMER_SECRET);
        builder.setOAuthAccessToken(TWITTER_TOKEN);
        builder.setOAuthAccessTokenSecret(TWITTER_TOKEN_SECRET);
        Configuration configuration = builder.build();
        TwitterFactory factory = new TwitterFactory(configuration);
        Twitter twitter = factory.getInstance();

        ArrayList<String[]> tweetEntities = new ArrayList<String[]>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(pathToTSVFile)));
        String currentLine;
        if (skipFirstLine) bufferedReader.readLine();

        while ((currentLine = bufferedReader.readLine()) != null) {
            String[] columns = currentLine.split("\\t");
            long tweetID = Long.parseLong(columns[column]);

            try {
                Status s = twitter.showStatus(tweetID);
                String[] info = new String[6];

                if (s == null) {
                    //unsure if needed, but kept for safety
                } else {
                    info[0] = utf8Decode(s.getUser().getProfileImageURL());
                    info[1] = utf8Decode(s.getUser().getScreenName());
                    info[2] = utf8Decode(s.getUser().getName());
                    info[3] = utf8Decode(s.getUser().getDescription());
                    info[4] = utf8Decode(s.getText());
                    info[5] = label;
                    tweetEntities.add(info);
                }
            } catch (TwitterException e) {
                System.err.print("Failed to find tweet: " + e.getMessage());
            }

        }
        return tweetEntities;
    }

    /*
        Get a column of tweet text from a tsv file and give it a label
     */
    public static ArrayList<String[]> getUnlabeledTweetTextAndLabel(String pathToTSVFile, int column, boolean skipFirstLine, String label) throws FileNotFoundException, IOException {
        ArrayList<String[]> tweetEntities = new ArrayList<String[]>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(pathToTSVFile)));
        String currentLine;
        if (skipFirstLine) bufferedReader.readLine();

        while ((currentLine = bufferedReader.readLine()) != null) {
            String[] columns = currentLine.split("\\t");
            String[] info = new String[6];
            info[0] = "";
            info[1] = "";
            info[2] = "";
            info[3] = "";
            info[4] = columns[column];
            info[5] = label;
            tweetEntities.add(info);
        }
        return tweetEntities;
    }

    /*
        Writes several string arrays containing tweet entities to a csv file. Append indicates whether to append
        the tweet entities to the existing content of the file or to start anew
     */
    public static void writeTweetEntitiesToFile(ArrayList<String[]> tweetEntities, String pathToCSVFile, boolean append) throws IOException, FileNotFoundException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(pathToCSVFile), append));
        int counter = 0;
        for (String[] tweetEntity: tweetEntities) {
            if (tweetEntity[4].length() > 1) {
                counter++;
                for (int i = 0; i < tweetEntity.length; i++) {
                    bufferedWriter.write(tweetEntity[i]);
                    if (i < tweetEntity.length - 1) bufferedWriter.write(",");
                }

                if (counter >= 28000) {
                    break;
                }
                bufferedWriter.newLine();
            }
        }
        bufferedWriter.close();
    }

    /*
        limited usage
     */
    public static void getMajorityLabelFromIllnessJobTweetFiles(String pathToInputFile, String pathToOutputFile) throws IOException {
        final int POSITION_OF_USNAME = 27;
        final int POSITION_OF_NAME = 28;
        final int POSITION_OF_PROF = 29;
        final int POSITION_OF_TWEET = 30;
        final int POSITION_OF_LABEL = 35;

        BufferedReader reader = new BufferedReader(new FileReader(new File(pathToInputFile)));
        Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(reader);
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(pathToOutputFile), true));
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.RFC4180);

        String lastTweet = "";
        Hashtable<String, Integer> labelVote = new Hashtable<String, Integer>();
        for (CSVRecord record: records) {
            String tweet = record.get(POSITION_OF_TWEET);

            //if it's a new tweet, take the majority vote, write out the tweet, and start the voting process again
            if (!tweet.equals(lastTweet)) {
                //set the new tweet
                lastTweet = tweet;

                //take the majority vote if these is an odd number of votes
                if (labelVote.size() % 2 != 0) {
                    //get the label with the highest vote
                    Enumeration<String> labels = labelVote.keys();
                    int highestVote = 0;
                    String highestVoteLabel = "";
                    while (labels.hasMoreElements()) {
                        String currLabel = labels.nextElement();

                        if (labelVote.get(currLabel) > highestVote) {
                            highestVote = labelVote.get(currLabel);
                            highestVoteLabel = currLabel;
                        }
                    }

                    //print out the tweet
                    printer.print("");
                    printer.print(record.get(POSITION_OF_USNAME));
                    printer.print(record.get(POSITION_OF_NAME));
                    printer.print(record.get(POSITION_OF_PROF));
                    printer.print(record.get(POSITION_OF_TWEET));
                    printer.print("");
                    writer.write(highestVoteLabel);
                    printer.println();

                    labelVote = new Hashtable<String, Integer>();
                }
            }

            //add the new tweet's label to the vote count
            String label = record.get(POSITION_OF_LABEL);
            if (labelVote.get(label) == null) {
                labelVote.put(label, 1);
            }
            else {
                labelVote.put(label, labelVote.get(label) + 1);
            }

        }

        reader.close();
        printer.close();
    }

    public static String getTweet(String tweetID) {

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(CONSUMER_KEY);
        builder.setOAuthConsumerSecret(CONSUMER_SECRET);
        builder.setOAuthAccessToken(TWITTER_TOKEN);
        builder.setOAuthAccessTokenSecret(TWITTER_TOKEN_SECRET);
        Configuration configuration = builder.build();
        TwitterFactory factory = new TwitterFactory(configuration);
        Twitter twitter = factory.getInstance();


        String decoded = "";

        try {
            Status s = twitter.showStatus(Long.parseLong(tweetID));

            if (s == null) {
                //unsure if needed, but kept for safety
            } else {
                System.out.println("@" + s.getUser().getScreenName()
                        + " - " + s.getText());
                decoded = utf8Decode(s.getText());
            }

        } catch (TwitterException e) {
            System.err.print("Failed to find tweet: " + e.getMessage());
        }
        return decoded;
    }

    public static String utf8Decode(String input) {

        try {
            byte[] bytes = input.getBytes("UTF-8");
            String decoded = new String(bytes, "UTF-8");
            String done = (String) decoded;
            return done;
        }
        catch(UnsupportedEncodingException e) {
            System.out.println(e.getMessage());
            throw new AssertionError("This encoding is unknown.");
        }
    }

    public static void main(String[] args) {
		/*
		 * If not running from command line, then change the var inFile to a string represenation
		 * of the path to the IDs file.
		 * */
        String inFile = args[0];

        BufferedReader reader = null;

        try {
            String currentLine;
            //Create file output object
            File tweetFile = new File(args[1]);

            if (!tweetFile.exists()) {
                tweetFile.createNewFile();
            }

            FileWriter fw = new FileWriter(tweetFile.getAbsoluteFile());
            BufferedWriter tweetToFile = new BufferedWriter(fw);

            reader = new BufferedReader(new FileReader(inFile));

            int count = 0;
            while ((currentLine = reader.readLine()) != null && count < 10) {

                String[] splitLine = currentLine.split("\\t");
                String id = splitLine[0];
                String label = splitLine[1];

                String tweet = getTweet(splitLine[0]);

                //Write UTF-8 encoded tweet text to file
                //This will be replaced as writing the data to the file.
                if (!tweet.equals("")) {
                    tweetToFile.write(id + "\t" + label + "\t" + tweet);
                    tweetToFile.newLine();
                }

                Pattern hashtagPattern = Pattern.compile("#(\\w+)");
                Matcher matcher = hashtagPattern.matcher(tweet);
                List<String> hashtags = new ArrayList<>();

                while (matcher.find()) {
                    hashtags.add(matcher.group(1));
                }

                System.out.println(hashtags);
                count++;
            }
            tweetToFile.close();
        }

        catch (UnsupportedEncodingException e) {
            System.out.println(e.getMessage());
        }

        catch (IOException e) {
            e.printStackTrace();
        }

        finally {

            try {
                if (reader != null) reader.close();
            }

            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /*
        Remove later
     */
    public static void addExtraFieldToTweetsWithoutLabelField(String pathToTweets, int indexToAddSpace) throws FileNotFoundException, IOException {
        File inputFile = new File(pathToTweets);
        File outputFile = new File(pathToTweets.substring(0, pathToTweets.length() - 4)+"-p.csv");

        BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile));

        Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(bufferedReader);
        CSVPrinter printer = new CSVPrinter(bufferedWriter, CSVFormat.RFC4180);

        //go through each tweet, collect and print out fields 0-4, print out an extra field, and then print out the
        //remaining fields
        inputFile.delete();
        for (CSVRecord tweet: records) {
            if (tweet.size() > indexToAddSpace) {
                for (int i = 0; i < indexToAddSpace; i++) {
                    printer.print(tweet.get(i));
                }
                printer.print("");
                for (int i = indexToAddSpace; i < tweet.size(); i++) {
                    printer.print(tweet.get(i));
                }
                printer.println();
            }
        }
        printer.close();
    }

    public static void removeFieldFromTweets(String pathToTweets, int indexToRemove) throws FileNotFoundException, IOException {
        File inputFile = new File(pathToTweets);
        File outputFile = new File(pathToTweets.substring(0, pathToTweets.length() - 4)+"-p.csv");

        BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile));

        Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(bufferedReader);
        CSVPrinter printer = new CSVPrinter(bufferedWriter, CSVFormat.RFC4180);

        //go through each tweet, collect and print out fields 0-4, print out an extra field, and then print out the
        //remaining fields
        inputFile.delete();
        for (CSVRecord tweet: records) {
            if (tweet.size() > indexToRemove) {
                for (int i = 0; i < indexToRemove; i++) {
                    printer.print(tweet.get(i));
                }
                for (int i = indexToRemove + 1; i < tweet.size(); i++) {
                    printer.print(tweet.get(i));
                }
                printer.println();
            }
        }
        printer.close();

    }
}
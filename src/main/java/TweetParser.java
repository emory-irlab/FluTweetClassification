import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
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
    Fields should be as follows: profile picture, handle, name, description, tweet, label
 */
    public static ArrayList<String[]> getTweets(String pathToTweetFile) throws FileNotFoundException, IOException {
        ArrayList<String[]> tweets = new ArrayList<String[]>();
        BufferedReader in = new BufferedReader(new FileReader(pathToTweetFile));
        Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
        for (CSVRecord record : records) {
            String[] tweetFields = new String[6];
            for (int i = 0; i < 6; i++) {
                tweetFields[i] = record.get(i);
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
        for (String[] tweetEntity: tweetEntities) {
            for (int i = 0; i < tweetEntity.length; i++) {
                bufferedWriter.write(tweetEntity[i]);
                if (i < tweetEntity.length - 1) bufferedWriter.write(",");
            }
            bufferedWriter.newLine();
        }
        bufferedWriter.close();
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
}

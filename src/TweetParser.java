import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


public class TweetParser {
	/*
	 * First arg is path to file with IDs
	 * Second arg is path where tweetFile should be saved
	 * 
	 * */

    static final String CONSUMER_KEY = "Yn2MuM201XbTBeX1lx8DiEYvA";
    static final String CONSUMER_SECRET = "1dIRD2WmDjlQB7ehT1SQ0VNM8kuo1DhFLRYg9JYCXw598wYIQJ";
    static final String TWITTER_TOKEN = "714776825885564928-Uqc6fVxpqsil3OqIHMQ9f8pkwaDQkzW";
    static final String TWITTER_TOKEN_SECRET = "PjQRk3RjdggBPwjaKMHb5aKgT6SOO2YUjDTtMWkmdAG8T";

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
}
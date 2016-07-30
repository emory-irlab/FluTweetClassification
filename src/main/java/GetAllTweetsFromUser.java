
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class GetAllTweetsFromUser {

    static String CONSUMER_KEY = "Yn2MuM201XbTBeX1lx8DiEYvA";
    static String CONSUMER_SECRET = "1dIRD2WmDjlQB7ehT1SQ0VNM8kuo1DhFLRYg9JYCXw598wYIQJ";
    static String TWITTER_TOKEN = "714776825885564928-Uqc6fVxpqsil3OqIHMQ9f8pkwaDQkzW";
    static String TWITTER_TOKEN_SECRET = "PjQRk3RjdggBPwjaKMHb5aKgT6SOO2YUjDTtMWkmdAG8T";
    static int cycleCount = 0;

    public static Twitter unauthenticatedTwitter;

    final static long SIX_MONTHS_SECONDS = 15552000;


    public static void main(String[] args) throws FileNotFoundException, IOException, NumberFormatException, TwitterException, ParseException, InterruptedException {

        //Consumer Key, CK Secret, Access Token, AT Secret
           /*
           String[] apmouat = {"Yn2MuM201XbTBeX1lx8DiEYvA", "1dIRD2WmDjlQB7ehT1SQ0VNM8kuo1DhFLRYg9JYCXw598wYIQJ",
                "714776825885564928-Uqc6fVxpqsil3OqIHMQ9f8pkwaDQkzW", "PjQRk3RjdggBPwjaKMHb5aKgT6SOO2YUjDTtMWkmdAG8T"};
           String[] estudent123 = {"azR5BFmHLdnx7ZS0Jj0YmvOHj", "1G95e273y1fKKQdnaWqxC4TcVhaKoNpodUV1DemWetxo4KsM7f",
                "757791428516810755-nfiZKmInRneM4CYuqrLE5uMPgOdymVy", "Zh52dDQ2WfStFQNwlEXp5ItL5JainLCahSzdREvM3rGRv"};
           String[] estudent1234 = {"FxcljWGMjgXw4cknur9AbwCZr", "5VJzg1svPbCa4vaveYz1Ock5GE7rQu3oI3I88RoerlpDp390ru",
                "758357944958226432-17w6VoyB1CfSwQ3dJw3znk4nBO8rhMU", "Dnesa6zepizUMPNmJ7rcsUPknSrJMQmeBN5JNOSUh5hCW"};
           String[] estudent12344 = {"uQq1zkpNbPZNq42jyMN4sHIOX", "Vl1vDOG0F18QH16uO8biWKXUzzcNCssligEUZh5ELpiFtCiE2c",
                "758360309211934721-Jl5HVhBb1hMEBiBJzqiY0dq2MTOvynL", "jVW0ludWjOXwB6qDSZPiSEXCjV3j0WvLCif7b5tH6Xerc"};
           */
        String[] awolyniec = {"dIhEDlcQpMgVv2WnYwJovrIDJ", "458Qvg6dLLyPt4FsecgpR5vIoo2vEw4vICq1VBLPgGg6rXeXKo",
                "725040125231652868-VVar3LOryC57m6SBvc8czGuTb1XZKJ7", "tJiAx7xMfMd6zFjtdyiAN87fOEFKVpO8jlBOVPlRpzGJU"};

        List<String[]> keysAPI = new ArrayList<String[]>();
           /*
           keysAPI.add(apmouat);
           keysAPI.add(estudent123);
           keysAPI.add(estudent1234);
           keysAPI.add(estudent12344);
           */
        keysAPI.add(awolyniec);

        newAPIKeys(keysAPI.get(0));

        File in = new File("data/experimental_tweets/job_loss-self-output.csv");
        BufferedReader inReader = new BufferedReader(new FileReader(in));
        CSVParser parser = new CSVParser(inReader, CSVFormat.RFC4180);
        List<CSVRecord> records = parser.getRecords();

        File writeOut = new File("data/experimental_tweets/job_loss-self-PLUS-HISTORICAL.csv");
        BufferedWriter w = new BufferedWriter(new FileWriter(writeOut));
        CSVPrinter printer = new CSVPrinter(w, CSVFormat.TDF);

        int numAccountsUsed = 1;
        int recordCount = 0;

        //for (CSVRecord record : records) {
	  for (int i = 11; i < records.size(); i++) {
	    CSVRecord record = records.get(i);
            System.out.println("On record " + ++recordCount);

            String user = record.get(0);
            String[] str = record.get(4).split("\\s");
            String date = str[2] + "-" + str[1] + "-" + str[5];
            SimpleDateFormat f = new SimpleDateFormat("dd-MMM-yyyy");
            long relevantTweetDateSeconds = f.parse(date).getTime() / 1000;

            printer.print(user);
            printer.print(record.get(8));
            printer.print(record.get(9));
            printer.print(record.get(11));
            printer.print(record.get(4));

            List<Status> statuses = new ArrayList<Status>();
            System.out.println("Now gathering tweets for user: " + user);
            int pageno = 1;
            while (true) {

                try {
                    int size = statuses.size();
                    Paging page = new Paging(pageno++, 100);
                    statuses.addAll(unauthenticatedTwitter.getUserTimeline(user, page));

                    if (statuses.size() == size) {
                        break;
                    }
                }
                catch (TwitterException e) {

                    //if (e.getErrorCode() == 88 && numAccountsUsed == 4) {
                    if (e.getErrorCode() == 88) {
                        numAccountsUsed = 0;
                            System.out.println("System going to sleep. Cycle: " + ++cycleCount);
                            Thread.sleep(1000 * 930);
                            System.out.println("System waking up.");
			    newAPIKeys(keysAPI.get(numAccountsUsed));
                        
                    }
		    else {
			continue;
		    }
			 /*
	   		 else if(e.getErrorCode() == 88 && numAccountsUsed < 4) {
	   			newAPIKeys(keysAPI.get(numAccountsUsed));
	   			numAccountsUsed++;
	   		 }
			*/
		    /*
                    else {
                        continue;
                    }*/
                }
            }

            System.out.println("Now printing tweets...");
            for (Status s : statuses) {
                    Date histDate = s.getCreatedAt();
		    printer.print(histDate.toString());
                    printer.print(s.getText());
		    printer.println();
		    printer.flush();
	    }
        }

        parser.close();
        printer.close();
    }

    public static void newAPIKeys(String[] keys) {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(keys[0]);
        builder.setOAuthConsumerSecret(keys[1]);
        builder.setOAuthAccessToken(keys[2]);
        builder.setOAuthAccessTokenSecret(keys[3]);
        Configuration configuration = builder.build();
        TwitterFactory factory = new TwitterFactory(configuration);
        unauthenticatedTwitter = factory.getInstance();
    }
}

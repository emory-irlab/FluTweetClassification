import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextFeatures {
	
	/*
	 * TODO: Use of plural personal programs insinuates individuals,
	 * not a single person. 
	 * 
	 * TODO: containsTimes/containsDates
	 * 
	 * */

    public static String[] verbsWithLink = {"follow", "check", "go"};

    /*
       Binary feature to see if the string contains "check out" (or some form of "check" along with "out),
       with at least a non-word character separating the two
    */
    public static int checkOutFeature(String tweet) {
        tweet = tweet.toLowerCase();
        Pattern pattern = Pattern.compile("((check((ing)|(s)|(ed))?)(\\W.*?)out){1}?");
        Matcher matcher = pattern.matcher(tweet);
        //if a "check x out" group has been found
        while (matcher.find()) {
            String spaceBetween = matcher.group(7);
            Pattern pattern2 = Pattern.compile("\\s");
            Matcher matcher2 = pattern2.matcher(spaceBetween);
            int counter = 0;
            //count the number of spaces between "check" and "out"
            while (matcher2.find()) counter++;
            if (counter < 4) return 1;
        }

        /*
        //find either the word check or out first, determine number of words
        //between them using white space
        if (tweet.contains("check"))


        if (tweet.indexOf("check") != -1 && tweet.indexOf("out") != -1) {
            if (tweet.indexOf("check") < tweet.indexOf("out")) {
                return 1;
            }
        }
        */
        return 0;
    }

    public static int containsAt(String tweet) {
        if (tweet.contains("@")) return 1;

        return 0;
    }

    //accuracy for containsMention is a bit lower than for containsAt, when URLs are not already taken
    public static int containsMention(String tweet) {
        int in = tweet.indexOf('@');
        while (in != -1) {
            String after = tweet.substring(Math.min(in+1, tweet.length()), Math.min(in+10, tweet.length()));
            Pattern pattern = Pattern.compile(" ?https?://");
            Matcher matcher = pattern.matcher(after);
            //if there is no http(s):// right after the "@", count as user mention
            if (!matcher.find() || matcher.start() > 0) return 1;

            in = tweet.indexOf('@', in+1);
        }
        return 0;
    }

    public static int containsDeal(String tweet) {

        if (tweet.toLowerCase().contains("deal")) {
            return 1;
        }
        return 0;
    }

    public static int containsLink(String tweet) {

        if (tweet.toLowerCase().contains("link")) {
            return 1;
        }
        return 0;
    }
	
	/*
	 * Fairly primitive method for detecting URLs in a tweet.
	 * Twitter does not seem to do embedded URLs which is pleasant.
	 * 
	 * Reasoning: Corporations or government organizations etc. almost
	 * always put URLs in their Tweets.
	 * */


    public static int containsURL(String tweet) {
        //Regex pattern found at "http://stackoverflow.com/questions/163360/regular-expression-to-match-urls-in-java"
		/*Pattern locateURL = Pattern.compile("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
		Matcher URLFinder = locateURL.matcher(tweet);*/

        Pattern detectURL = Pattern.compile("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
        //Pattern detectURL = Pattern.compile("(http)|(https)|(.com)|(.gov)|(.org)");
        Matcher URLdetector = detectURL.matcher(tweet);

        if (URLdetector.find()) {
            return 1;
        }
        return 0;
    }

    //needs to be fixed
    public static int isQuestionTweet(String tweet) {

        int sentenceCount = 0;
        int questionCount = 0;
        int URLCount = 0;

        if (containsURL(tweet) == 1) {
            tweet = tweet.substring(0, tweet.indexOf("http")); //the URL isn't always at the end
            URLCount++;
        }

        Pattern p = Pattern.compile("[.?!]");
        Matcher endOfSentence = p.matcher(tweet);

        while (endOfSentence.find()) {

            sentenceCount++;

            if (endOfSentence.group().equals("?")) {
                questionCount++;
            }
        }

        if(sentenceCount == 1 && questionCount == 1 && URLCount == 1) {
            return 1;
        }
        return 0;
    }
	
	/*
	 * TODO: Method needs refining, finds character sequences and not necessarily words
	 * 
	 * Counts number of personal plural pronouns
	 * */

    public static int numPluralPersonalPronouns(String tweet) {

        Pattern pluralPersonalPronounsLocator = Pattern.compile("(we)|(us)|(ourselves)|(our)");
        Matcher m = pluralPersonalPronounsLocator.matcher(tweet.toLowerCase());
        int count = 0;

        while (m.find()) {
            count++;
        }
        return count;
    }

    /*
     * TODO: Find way to determine start of URL
     * 		 DO NOT USE METHOD OTHERWISE
     * */
    public static String removeURL(String tweet) {
		/*
		//Regex pattern found at "http://stackoverflow.com/questions/163360/regular-expression-to-match-urls-in-java"
		Pattern locateURL = Pattern.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
		Matcher URLFinder = locateURL.matcher(tweet);*/
        Pattern detectURL = Pattern.compile("((http)|(https)|(.com)|(.gov)|(.org))");
        Matcher URLFinder = detectURL.matcher(tweet);

        if (URLFinder.find()) {
            tweet = tweet.substring(0, URLFinder.start());
        }

        return tweet;
    }
	
	/*
	 * One may already have the index of the URL and would like it removed.
	 * This just skips a few steps of the alternate version.
	 * */

    public static String removeURL(String tweet, int index) {
        return tweet.substring(0, index);
    }

    /**
     * TODO: Rewrite method to find verb in same sentence with link
     * */
	
	/*public static int verbAndLink(String tweet) {
		
		if (containsLink(tweet) == 1) {
			for (String v : verbsWithLink) {
				if (tweet.contains(v)) {
					return 1;
				}
			}
		}
		return 0;
	}*/

}
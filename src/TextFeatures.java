import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextFeatures {
	
	/*
	 * TODO: containsTimes/containsDates
	 *       Names dictionary lookup method
	 * */
	
	public static String[] verbsWithLink = {"follow", "check", "go"};
	public static HashSet<String> firstNames = new HashSet<String>();
	
	//Regex pattern found at "http://stackoverflow.com/questions/163360/regular-expression-to-match-urls-in-java"
	public static Pattern detectURL = Pattern.compile("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
	public static Matcher URLdetector;

	public static int checkOutFeature(String tweet) {
		
		//find either the word check or out first, determine number of words
		//between them using white space
		
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
		return 0;
	}
	
	public static int containsAt(String tweet) {
		if (tweet.contains("@")){
			return 1;
		}
		return 0;
	}
	
	public static int containsDeal(String tweet) {
		
		if (tweet.toLowerCase().contains("deal")) {
			return 1;
		}
		return 0;
	}
	
	/*
	 * Checks if the first name of the user
	 * appears in a large list of human first names.
	 * 
	 * TODO: Test method and make sure it retains the loaded namelist
	 * throughout method calls.
	 * 
	 * */
	
	public static int containsHumanName(String name) {
		
		if (firstNames.isEmpty()) {
			initializeFirstNames();
		}
		
		if (firstNames.contains(name)) {
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
		
		URLdetector = detectURL.matcher(tweet);
		
		if (URLdetector.find()) {
			return 1;
		}
		return 0;
	}
	
	public static int getURLIndex(String tweet) {
		
		URLdetector = detectURL.matcher(tweet);
				
		if (URLdetector.find()) {
			return URLdetector.start();
		}
		return -1;
	}
	
	public static void initializeFirstNames() {
		/*
		 * TODO: Adjust file so it can be run on any computer.
		 * 
		 * */
		File names = new File("..\\src\\FirstNames.txt");
		BufferedReader b;
		
		try {
			b = new BufferedReader(new FileReader(names));
			String currentName = "";
			while ((currentName = b.readLine()) != null) {
				firstNames.add(currentName);
			}
			
		}
		catch(FileNotFoundException f) {
			System.out.println("ERROR: " + f.getMessage());
		}
		catch(IOException e) {
			System.out.println("ERROR: " + e.getMessage());
		}
	}
	
	/*
	 * A "Question Tweet" is one of the following format:
	 * From EMC^2 twitterfeed:
	 * "What should be your first step to modernize your data center?
	 * @guychurchward explains http://emc.im/6010BSrfE"
	 * */
	
	public static int isQuestionTweet(String tweet) {
		
		int sentenceCount = 0;
		int questionCount = 0;
		int URLCount = 0;
		
		if (containsURL(tweet) == 1) {
			tweet = removeURL(tweet);
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
	 * Checks to see if all text in a tweet is uppercase
	 * */
	
	public static int isAllUpperCase(String tweet) {
		
		tweet = removeURL(tweet);
		
		for (int i = 0; i < tweet.length(); i++) {
			if (!Character.isUpperCase(tweet.charAt(i))) {
				return 0;
			}
		}
		return 1;
	}
	
	public static ArrayList<String> getHashtags(String tweet) {
		
		Pattern hashtagPattern = Pattern.compile("#(\\w+)");
		Matcher matcher = hashtagPattern.matcher(tweet);
		ArrayList<String> hashtags = new ArrayList<String>();
		
		while (matcher.find()) {
		    hashtags.add(matcher.group(1));
		}
		return hashtags;
	}
	
	
	/*
	 * TODO: Method needs refining
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
	
	public static String removeURL(String tweet) {
		
		URLdetector = detectURL.matcher(tweet);
//		Pattern detectURL = Pattern.compile("((http)|(https)|(.com)|(.gov)|(.org))");
//		Matcher URLFinder = detectURL.matcher(tweet);
		
		if (URLdetector.find()) {
			tweet = tweet.substring(0, URLdetector.start());
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

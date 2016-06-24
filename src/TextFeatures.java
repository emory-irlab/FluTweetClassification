import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.util.Iterator;

public class TextFeatures {

	static String spaceGroup = "([\\s\\-_]+)";
	static String userMention = "(^|"+spaceGroup+")(@)(\\w+)";

	static String[] verbsWithLink = {"follow", "check", "go"};
	static HashSet<String> firstNames = new HashSet<String>();
	static HashSet<String> lastNames = new HashSet<String>();
	static Pattern userMentionPattern = Pattern.compile("(^|"+spaceGroup+")(@)(\\w+)");
	static Pattern checkOutPattern = Pattern.compile("((check((ing)|(s)|(ed))?)(\\W.*?)out){1}?");
	static Pattern companyNamesPattern = Pattern.compile("");
	static Pattern companyTermsPattern = Pattern.compile("(?i)job(s)?|news|update(s)?");
	//Regex pattern found at "http://stackoverflow.com/questions/163360/regular-expression-to-match-urls-in-java"
	static Pattern detectURL = Pattern.compile("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|](("+spaceGroup+")|$)");
	static Pattern mentionsSocMedia = Pattern.compile("(?i)Facebook|Snapchat|Instagram|Twitter|IG:");
	static Pattern pluralPersonalPronounsLocator = Pattern.compile("(^|[^\\w])((we)|(us)|(ourselves)|(our)|(ours))($|[^\\w])");
	static Pattern timePattern = Pattern.compile("(\\d:\\d)|(?i)(am|pm)|(?i)(Mon|Tues|Wed|Thur|Fri)");
	static Matcher generalMatcher;
	static Pattern firstNamePattern = Pattern.compile("(^([a-zA-Z]+)("+spaceGroup+"|$))");
	static Pattern spaceGroupCounterPattern = Pattern.compile("[^^]"+spaceGroup+"[^$]");
	static Pattern hashtagPattern = Pattern.compile("(#)(\\w+)");
	static Pattern atPattern = Pattern.compile("@ ?([\\w-]+)?");
	static Pattern retweetPattern = Pattern.compile("RT"+userMention);

	public static int checkOutFeature(String tweet) {
		
		//find either the word check or out first, determine number of words
		//between them using white space
		
		tweet = tweet.toLowerCase();
		generalMatcher = checkOutPattern.matcher(tweet);
        //if a "check x out" group has been found
        while (generalMatcher.find()) {
            String spaceBetween = generalMatcher.group(7);
            Pattern pattern2 = Pattern.compile("\\s");
            Matcher matcher2 = pattern2.matcher(spaceBetween);
            int counter = 0;
            //count the number of spaces between "check" and "out"
            while (matcher2.find()) counter++;
            if (counter < 4) return 1;
        }
		return 0;
	}

	public static int containsAlphabeticCharacters(String text) {
		for (int i = 0; i < text.length(); i++)
			if (Character.isAlphabetic(text.charAt(i))) return 1;
		return 0;
	}
	
	public static int containsAt(String tweet) {
		if (tweet.contains("@")){
			return 1;
		}
		return 0;
	}

	//"(?i)" - ignores case while parsing

	public static int containsDeal(String tweet) {

		if (tweet.toLowerCase().contains("deal")) {
			return 1;
		}
		return 0;
	}

	public static int containsDigits(String text) {
		for (int i = (int)'0'; i < (int)'9' + 1; i++) {
			if (text.indexOf((char)i) != -1) return 1;
		}
		return 0;
	}

	public static int containsLink(String tweet) {
		if (tweet.toLowerCase().contains("link")) {
			return 1;
		}
		return 0;
	}

    //accuracy for containsMention is a bit lower than for containsAt
	/*
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
    */

	//revised method
	public static int containsMention(String tweet) {
		generalMatcher = userMentionPattern.matcher(tweet);
		if (generalMatcher.find()) return 1;

		return 0;
	}

	/*
    Check if the tweet contains strings of multiple exclamation points together and strings of
    multiple question marks together
 	*/
	public static int containsMultipleExclamationsQuestions(String tweet) {
		Pattern pattern = Pattern.compile("(!{2,})|(\\?{2,})");
		Matcher matcher = pattern.matcher(tweet);
		if (matcher.find()) {
			return 1;
		}
		return 0;
	}

	public static int containsTimeMark(String tweet) {

		Matcher timeMatcher = timePattern.matcher(tweet);

		if (timeMatcher.find()) {
			return 1;
		}
		return 0;
	}

	/*
    Does it contain a sequence of 2 or more consecutive uppercase characters?
	*/
	public static int containsUpperCaseSequence(String text) {
		int lastOne = 0;
		for (int i = 0; i < text.length(); i++) {
			if (Character.isUpperCase(text.charAt(i))) {
				lastOne++;
				if (lastOne > 1) return 1;
			}
			else lastOne = 0;
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

		generalMatcher = detectURL.matcher(tweet);

		if (generalMatcher.find()) {
			return 1;
		}
		return 0;
	}

	public static int countCompanyTerms(String description) {

		generalMatcher = companyTermsPattern.matcher(description);
		int count = 0;

		while (generalMatcher.find()) {
			count++;
		}
		return count;
	}

	/*
    Counts the number of phrases ending in a single or multiple exclamation points
 	*/
	public static int countExclamationPhrases(String tweet) {
		int count = 0;
		Pattern pattern = Pattern.compile("!+");
		Matcher matcher = pattern.matcher(tweet);
		while (matcher.find()) {
			count++;
		}
		return count;
	}

	/*
    Counts the instances of sequences of space characters (defined as \s, -, and _) surrounded on both sides by
    non-space characters
 	*/
	public static int countSpaceGroups(String text) {
		int counter = 0;
		generalMatcher = spaceGroupCounterPattern.matcher(text);
		while (generalMatcher.find()) {
			counter++;
		}
		return counter;
	}

	/*
	 * Checks if the first name of the user
	 * appears in a large list of human first names.
	 * 
	 * TODO: Test method and make sure it retains the loaded namelist
	 * throughout method calls.
	 * 
	 * */
	
	public static int firstWordIsCommonFirstName(String name) { //possibly take out some chars
        generalMatcher = firstNamePattern.matcher(name);
        if (generalMatcher.find()) name = generalMatcher.group(2).toLowerCase();
		
		if (firstNames.isEmpty()) initializeNames(firstNames, "data/FirstNames.csv");

		Iterator<String> names = firstNames.iterator();
        while (names.hasNext()) {
            String nam = names.next().toLowerCase();
            //System.out.println(nam+", "+name);
            if (nam.equals(name)) return 1;
        }

		return 0;
	}

	public static ArrayList<String> getHashtags(String tweet) {

		Matcher matcher = hashtagPattern.matcher(tweet);
		ArrayList<String> hashtags = new ArrayList<String>();

		while (matcher.find()) {
			hashtags.add(matcher.group(2));
		}
		return hashtags;
	}

	public static int getURLIndex(String tweet) {

		generalMatcher = detectURL.matcher(tweet);

		if (generalMatcher.find()) {
			return generalMatcher.start();
		}
		return -1;
	}
	
	public static void initializeNames(HashSet<String> names, String namePath) {
		/*
		 * TODO: Adjust file so it can be run on any computer.
		 * 
		 * */
		File nameFile = new File(namePath);
		BufferedReader b;
		
		try {
			b = new BufferedReader(new FileReader(nameFile));
			String currentName = "";
			while ((currentName = b.readLine()) != null) {
				names.add(currentName);
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
	 *   TODO: Remove URLs, mentions, and hashtags
	 *
	 * Checks to see if all alphabetical characters in a tweet, excluding URLs, mentions of other users,
	 * and hashtags, are uppercase
	 * */

	public static int isAllUpperCase(String tweet) {

		tweet = removePattern(tweet, detectURL);
		tweet = removePattern(tweet, hashtagPattern);
		tweet = removePattern(tweet, atPattern); //remove all @ groups, which in a tweet without URLS consists of all
		//user mentions and all @ characters with a URL following them

		for (int i = 0; i < tweet.length(); i++) {
			char current = tweet.charAt(i);
			if (Character.isAlphabetic(current) && !Character.isUpperCase(current)) {
				return 0;
			}
		}
		return 1;
	}

	public static boolean isPunctuation(String input) {
		char[] punctuation = {'.', ',', '"', '\'', '(', ')', '[', ']', '!', '?', ';', '`', '{', '}'};
		for (int i = 0; i < input.length(); i++) {
			boolean thisChar = false;
			for (char mark: punctuation) {
				if (input.charAt(i) == mark) {
					thisChar = true;
					break;
				}
			}
			if (!thisChar) return false;
		}
		return true;
	}
	
	/*
	 * A "Question Tweet" is one of the following format:
	 * From EMC^2 twitterfeed:
	 * "What should be your first step to modernize your data center?
	 * @guychurchward explains http://emc.im/6010BSrfE"
	 * */
	
	public static int isSingleQuestionURLTweet(String tweet) {
		
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

			if (endOfSentence.group(0).equals("?")) {
				questionCount++;
			}
		}
		
		if(sentenceCount == 1 && questionCount == 1 && URLCount == 1) {
			return 1;
		}
		return 0;
	}

	/*
    Checks if the last name of the user appears in a list of nearly 6000 US last names
 	*/
	public static int lastWordIsCommonLastName(String name) {
		Pattern pattern = Pattern.compile("[^^]"+spaceGroup+"([a-zA-Z]+)$");
		Matcher matcher = pattern.matcher(name);
		if (matcher.find()) name = matcher.group(2).toLowerCase();

		if (lastNames.isEmpty()) initializeNames(lastNames, "data/LastNames.csv");

		Iterator<String> names = lastNames.iterator();
		while (names.hasNext()) {
			String nam = names.next().toLowerCase();
			//System.out.println(nam+", "+name);
			if (nam.equals(name)) return 1;
		}

		return 0;
	}

	public static int mentionsSocialMedia(String description) {

		int count = 0;
		generalMatcher = mentionsSocMedia.matcher(description);

		while (generalMatcher.find()) {
			count++;
		}
		return count;
	}
	
	/*
	 * Not to be implemented (will likely be incorporated as a word class feature)
	 *
	 * Counts number of personal plural pronouns
	 * */
	/*
	public static int numPluralPersonalPronouns(String tweet) {
		
		Pattern pluralPersonalPronounsLocator = Pattern.compile("(^|[^\\w])((we)|(us)|(ourselves)|(our)|(ours))($|[^\\w])");
		Matcher m = pluralPersonalPronounsLocator.matcher(tweet.toLowerCase());
		int count = 0;
		
		while (m.find()) {
			count++;
		}
		return count;
	}
	*/

	public static String removeAtCharInMentions(String tweet) { return removePatternKeepGroupNumber(tweet, userMentionPattern, 4); }

	public static String removeCharsRepeated3PlusTimes(String tweet) {
		if (tweet.equals("")) return "";

		StringBuilder newTweet = new StringBuilder();
		int numTimesRepeated = 0;
		char lastChar = tweet.charAt(0);
		char currentChar;
		int lastEnd = 0;
		int lastBeginning = 0;
		int off;

		for (int i = 0; i < tweet.length(); i++) {
			currentChar = tweet.charAt(i);
			if (currentChar != lastChar || i == tweet.length() - 1) {
				off = 0;
				if (currentChar == lastChar) {
					numTimesRepeated++;
					off = 1;
				}

				//if it's a sequence of 3+ of the same char, reduce it to a sequence of 2
				if (numTimesRepeated >= 3) {
					newTweet.append(tweet.substring(lastEnd, lastBeginning+2));
					lastEnd = i + off;
				}
				lastChar = currentChar;
				lastBeginning = i;
				numTimesRepeated = 0;
			}
			numTimesRepeated++;
		}
		newTweet.append(tweet.substring(lastEnd));

		return newTweet.toString();
	}

	public static String removeHashtagCharInHashtags(String tweet) { return removePatternKeepGroupNumber(tweet, hashtagPattern, 2); }

	public static String removeHashtags(String tweet) {
		return removePattern(tweet, hashtagPattern);
	}

	public static String removeMentions(String tweet) { return removePattern(tweet, userMentionPattern); }

	public static String removePattern(String tweet, Pattern pattern) {
		if (tweet.equals("")) return "";

		generalMatcher = pattern.matcher(tweet);
		String newTweet = "";

		int lastEnd = 0;
		while (generalMatcher.find()) {
			newTweet += tweet.substring(lastEnd, generalMatcher.start());
			lastEnd = generalMatcher.end();
		}
		newTweet += tweet.substring(lastEnd);
		return newTweet;
	}

	public static String removePatternKeepGroupNumber(String tweet, Pattern pattern, int groupNum) {
		if (tweet.equals("")) return "";

		generalMatcher = pattern.matcher(tweet);
		String newTweet = "";

		int lastEnd = 0;
		while (generalMatcher.find()) {
			newTweet += tweet.substring(lastEnd, generalMatcher.start()) + generalMatcher.group(groupNum);
			lastEnd = generalMatcher.end();
		}
		newTweet += tweet.substring(lastEnd);
		return newTweet;
	}

	public static String removeRetweets(String tweet) { return removePattern(tweet, retweetPattern); }

	public static String removeURL(String tweet) { return removePattern(tweet, detectURL); }
	
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

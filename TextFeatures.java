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

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.Iterator;

public class TextFeatures {
	
	/*
	 * TODO: containsDates
	 *       Description-based
			-Phrases like “follow us”, “tweet us”, “check x out”
			-Hashtags
	 * */
	public static String[] verbsWithLink = {"follow", "check", "go"};
	public static HashSet<String> firstNames = new HashSet<String>();
	public static HashSet<String> lastNames = new HashSet<String>();
	
	public static Pattern checkOutPattern = Pattern.compile("((check((ing)|(s)|(ed))?)(\\W.*?)out){1}?");
	public static Pattern companyNamesPattern = Pattern.compile("");
	public static Pattern companyTermsPattern = Pattern.compile("(?i)job(s)?|news|update(s)?");
	//Regex pattern found at "http://stackoverflow.com/questions/163360/regular-expression-to-match-urls-in-java"
	public static Pattern detectURL = Pattern.compile("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
	public static Pattern firstNamePattern = Pattern.compile("(^([a-zA-Z]+)( |$))");
	public static Pattern mentionsSocMedia = Pattern.compile("(?i)Facebook|Snapchat|Instagram|Twitter");
	public static Pattern pluralPersonalPronounsLocator = Pattern.compile("(^|[^\\w])((we)|(us)|(ourselves)|(our)|(ours))($|[^\\w])");
	public static Pattern timePattern = Pattern.compile("(\\d:\\d)|(?i)(am|pm)|(?i)(Mon|Tues|Wed|Thur|Fri)");
	public static Matcher generalMatcher;
	
	/*
    Pre-defined word classes. Some entries contain special cases, rules specifying that the string to be matched to it
    is not a single word.
    Special cases (checked for in the listed order, cannot be combined):
     1. A single digit between 2-9 before words denotes multi-word features. The number indicates the number of words to search for.
        Note: If there is a feature for an individual word or set of words in a multi-word feature, the detection
        of a multi-word feature will not prevent detection of the sub-feature. Example: The string "the flu" will
        trigger will count as an instance of "the", an instance of "flu", and an instance of "the flu"
     2. "V-" denotes a verb ending. The feature extraction algorithm should match this entry to the ending of a verb
       word being scanned, and not the word itself
 */
	private static String[][] wordClasses = { //need to refine for split words (namely those involving apostrophes)
			{"Infection",
					"getting", "got", "recovered", "have", "having", "had", "has", "catching", "catch", "cured", "infected"},
			{"Possession",
					"bird", "2the flu", "flu", "sick", "epidemic"},
			{"Concern",
					"afraid", "worried", "scared", "fear", "worry", "nervous", "dread", "dreaded", "terrified"},
			{"Vaccination",
					"vaccine", "vaccines", "shot", "shots", "mist", "tamiflu", "jab", "2nasal spray"},
			{"Past Tense",
					"was", "did", "had", "got", "were", "V-ed"},
			{"Present Tense",
					"is", "am", "are", "have", "has", "V-ing"}, //"is" should perhaps take "'s", as in 'it's'
			{"Self",
					"I", "I've", "I'd", "I'm", "im", "my", "me"},
			{"Others",
					"your", "everyone", "you", "it", "its", "u", "her", "he", "she", "he's", "she's", "they", "'re"/* as in "you're"*/,
					"she'll", "he'll", "husband", "wife", "brother", "sister", "people", "kid", "kids", "children",
					"son", "daughter"},
			{"Plural 1P pronouns",
					"we", "our", "ourselves", "ours", "us"},
			{"Follow me",
					"follow", "tweet", "visit"}
	};
	
	public static String spaceGroup = "([\\s\\-_]+)";
	
	

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
	
	public static int containsAt(String tweet) {
		if (tweet.contains("@")){
			return 1;
		}
		return 0;
	}

    //accuracy for containsMention is a bit lower than for containsAt
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
	
	/*
	 * Checks if the first name of the user
	 * appears in a large list of human first names.
	 * 
	 * TODO: Test method and make sure it retains the loaded namelist
	 * throughout method calls.
	 * 
	 * */
	
	public static int containsCommonFirstName(String name) { //need to normalize to lowercase, possibly take out some chars
        
        generalMatcher = firstNamePattern.matcher(name);
        if (generalMatcher.find()) name = generalMatcher.group(2).toLowerCase();
		
		if (firstNames.isEmpty()) {
			initializeNames(firstNames, "src\\FirstNames.txt");
		}
		Iterator<String> names = firstNames.iterator();
        while (names.hasNext()) {
            String nam = names.next().toLowerCase();
            //System.out.println(nam+", "+name);
            if (nam.equals(name)) return 1;
        }
		return 0;
	}	/*
	 * Checks if the first name of the user
	 * appears in a large list of human first names.
	 * 
	 * TODO: Test method and make sure it retains the loaded namelist
	 * throughout method calls.
	 * 
	 * */
	
	public static int firstWordIsCommonFirstName(String name) { //possibly take out some chars
        Pattern pattern = Pattern.compile("(^([a-zA-Z]+)("+spaceGroup+"|$))");
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) name = matcher.group(2).toLowerCase();
		
		if (firstNames.isEmpty()) initializeNames(firstNames, "data/FirstNames.csv");

		Iterator<String> names = firstNames.iterator();
        while (names.hasNext()) {
            String nam = names.next().toLowerCase();
            //System.out.println(nam+", "+name);
            if (nam.equals(name)) return 1;
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
	
	public static int containsLink(String tweet) {
		
		if (tweet.toLowerCase().contains("link")) {
			return 1;
		}
		return 0;
	}
	
	/*
	 * Methods seeks out use of the word official
	 * This method is primarily for twitter handle/name
	 * */
	
	public static int containsOfficial(String tweet) {
		if (tweet.toLowerCase().contains("official")) {
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
	
	public static int containsTimeMark(String tweet) {
		
		Matcher timeMatcher = timePattern.matcher(tweet);
		
		if (timeMatcher.find()) {
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
    Count the number of words/strings in the given word class
 	*/
	public static int getFeatureForWordClass(CoreLabel[][] phrases, String relevantClassName) {
		//initialize
		int counter = 0;
		String[] relevantWordClass = new String[1];
		for (String[] aClass: wordClasses) {
			if (aClass[0].equals(relevantClassName)) {
				relevantWordClass = aClass;
				break;
			}
		}
		if (relevantWordClass.length == 1) {
			System.err.println("ERROR: Word class requested, "+relevantClassName+", does not exist.");//change to exception
			System.exit(1);
		}
		//go over each phrase
		for (CoreLabel[] phrase: phrases) {
			//go through each word in the phrase
			for (int i = 0; i < phrase.length; i++) {
				CoreLabel token = phrase[i];
				String stringInPhrase = token.get(CoreAnnotations.TextAnnotation.class);
				String stringInPhrasePOS = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
				//System.out.println(stringInPhrase);

				//get words to match
				for (int k = 1; k < relevantWordClass.length; k++) {
					String stringInPhraseCopy = stringInPhrase; //use this when referring to the input token
					String stringToMatch = relevantWordClass[k];
					//Alter the string to match and the copy of the input token if this is a special case

					//Special case 1: Multiple words are to be scanned
					int possibleNum = (int)stringToMatch.charAt(0) - '0';
					if (possibleNum > 1 && possibleNum < 10) {
						stringToMatch = stringToMatch.substring(1);
						StringBuilder buildMatch = new StringBuilder(stringInPhraseCopy);
						int parallelCount = i;
						//peek at the next words in the phrase, add them to the string to match
						while (possibleNum > 1) {
							parallelCount++;
							if (parallelCount == phrase.length) break;
							buildMatch.append(" ");
							buildMatch.append(phrase[parallelCount].get(CoreAnnotations.TextAnnotation.class));
							possibleNum--;
						}
						stringInPhraseCopy = buildMatch.toString();
						//if the multi-word phrase is found, make sure the words inside it are not scanned
						if (stringToMatch.equals(stringInPhraseCopy)) i = parallelCount;
					}
					//Special case 2: The string to be matched is a verb ending, so just compare endings
					else if (stringToMatch.length() > 1 && stringToMatch.substring(0, 2).equalsIgnoreCase("V-") && stringInPhrasePOS.charAt(0) == 'V') {
						stringToMatch = stringToMatch.substring(2);
						int startIndex = stringInPhrase.length() - stringToMatch.length();
						if (startIndex > 0) {
							stringInPhraseCopy = stringInPhraseCopy.substring(stringInPhrase.length() - stringToMatch.length());
						}
					}
					//match
					if (stringToMatch.equalsIgnoreCase(stringInPhraseCopy)) {
						counter++;
						//System.out.println("Matched string "+stringInPhraseCopy+" from base string "+stringInPhrase+" to string "+stringToMatch+" in word class "+relevantWordClass[0]);
					}
				}
			}
		}
		return counter;
	}

	/*
        Counts the number of phrases ending in a single or multiple exclamation points
     */
	public static int getFeatureForNumberOfExclamationPhrases(String tweet) {
		int count = 0;
		Pattern pattern = Pattern.compile("!+");
		Matcher matcher = pattern.matcher(tweet);
		while (matcher.find()) {
			count++;
		}
		return count;
	}

	/*
        Checks to see how many times the tweet contains strings of multiple exclamation points together and strings of
        multiple question marks together
     */
	public static int getFeatureForMultipleExclamationsQuestions(String tweet) {
		int count = 0;
		Pattern pattern = Pattern.compile("(!{2,})|(\\?{2,})");
		Matcher matcher = pattern.matcher(tweet);
		while (matcher.find()) {
			count++;
		}
		return count;
	}

	public static int containsDigits(String text) {
		for (int i = (int)'0'; i < (int)'9' + 1; i++) {
			if (text.indexOf((char)i) != -1) return 1;
		}
		return 0;
	}

	/*
		Counts the instances of sequences of space characters (defined as \s, -, and _) surrounded on both sides by
		non-space characters
	 */
	public static int countSpaceGroups(String text) {
		int counter = 0;
		Pattern pattern = Pattern.compile("[^^]"+spaceGroup+"[^$]");
		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			counter++;
		}
		return counter;
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
	
	public static int numPluralPersonalPronouns(String tweet) {
		
		generalMatcher = pluralPersonalPronounsLocator.matcher(tweet.toLowerCase());
		int count = 0;
		
		while (generalMatcher.find()) {
			count++;
		}
		return count;
	}
	
	public static String removeURL(String tweet) {
		
		generalMatcher = detectURL.matcher(tweet);
		
		if (generalMatcher.find()) {
			tweet = tweet.substring(0, generalMatcher.start());
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
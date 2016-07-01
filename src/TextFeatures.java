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

import org.apache.commons.csv.CSVParser;

import java.util.Iterator;

public class TextFeatures {

	static String spaceGroup = "([\\s\\-_]+)";
	static String userMention = "(^|"+spaceGroup+")(@)(\\w+)";

	static String[] verbsWithLink = {"follow", "check", "go"};

	//All hashsets are loaded upon run time

	//Dictionary word searches
	public static HashSet<String> firstNames = new HashSet<String>();
	public static HashSet<String> lastNames = new HashSet<String>();
	public static HashSet<String> listOfWordsDeath = new HashSet<String>();
	public static HashSet<String> listOfWordsFamily = new HashSet<String>();
	public static HashSet<String> listOfWordsMarriage = new HashSet<String>();
	//etc.

	//Sentiment analysis hash sets
	//public static HashSet<String> negativeAdjectives = new HashSet<String>();
	//public static HashSet<String> positiveAdjectives = new HashSet<String>();
	//public static HashSet<String> neutralAdjectives = new HashSet<String>();
	public static HashSet<String> negativeAdverbs = new HashSet<String>();
	public static HashSet<String> neutralAdverbs = new HashSet<String>();
	public static HashSet<String> positiveAdverbs = new HashSet<String>();
	public static HashSet<String> negativeEmoticons = new HashSet<String>();
	public static HashSet<String> positiveEmoticons = new HashSet<String>();

	static Pattern atPattern = Pattern.compile("@ ?([\\w-]+)?");
	static Pattern checkOutPattern = Pattern.compile("((check((ing)|(s)|(ed))?)(\\W.*?)out){1}?");
	static Pattern companyNamesPattern = Pattern.compile("");
	static Pattern companyTermsPattern = Pattern.compile("(?i)job(s)?|news|update(s)?");
	//Regex pattern found at "http://stackoverflow.com/questions/163360/regular-expression-to-match-urls-in-java"
	static Pattern detectURL = Pattern.compile("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|](("+spaceGroup+")|$)");
	static Pattern firstNamePattern = Pattern.compile("(^([a-zA-Z]+)("+spaceGroup+"|$))");
	static Pattern firstWordExcludeThe = Pattern.compile("(?![The])(?![\\s|$])?(\\w)+(\\s|$)");
	static Pattern hashtagPattern = Pattern.compile("(#)(\\w+)");
	static Pattern lastNamePattern = Pattern.compile("[^^]"+spaceGroup+"([a-zA-Z]+)$");
	static Pattern mentionsSocMedia = Pattern.compile("(?i)Facebook|Snapchat|Instagram|Twitter|IG:");
	static Pattern multipleExclamationsPattern = Pattern.compile("(!{2,})|(\\?{2,})");
	static Pattern numberOfExclamationsPattern = Pattern.compile("!+");
	static Pattern pluralPersonalPronounsLocator = Pattern.compile("(^|[^\\w])((we)|(us)|(ourselves)|(our)|(ours))($|[^\\w])");
	static Pattern retweetPattern = Pattern.compile("RT"+userMention);
	static Pattern spaceGroupCounterPattern = Pattern.compile("[^^]"+spaceGroup+"[^$]");
	static Pattern subscriptionPhrasePattern = Pattern.compile("(?i)follow (?i)(us)?|(?i)tweet(s)?");
	static Pattern timePattern = Pattern.compile("(\\d:\\d)|(?i)(am|pm)|(?i)(Mon|Tues|Wed|Thur|Fri)");
	static Pattern userMentionPattern = Pattern.compile("(^|"+spaceGroup+")(@)(\\w+)");

	static Matcher generalMatcher;

	/*
     * For each additional dictionary hash set created, it must be added into this method.
     * */

	public static void initializeHashSets() {

		initializeHashSet(TextFeatures.firstNames, "data/hashSets/FirstNames.csv");
		initializeHashSet(TextFeatures.lastNames, "data/hashSets/LastNames.csv");
		initializeHashSet(TextFeatures.listOfWordsFamily, "data/hashSets/FamilyTitles.txt");
		initializeHashSet(TextFeatures.negativeAdverbs, "data/hashSets/negativeAdverbs.txt");
		initializeHashSet(TextFeatures.positiveAdverbs, "data/hashSets/positiveAdverbs.txt");
		initializeHashSet(TextFeatures.neutralAdverbs, "data/hashSets/neutralAdverbs.txt");
		initializeHashSet(TextFeatures.negativeEmoticons, "data/hashSets/negativeEmoticons.txt");
		initializeHashSet(TextFeatures.positiveEmoticons, "data/hashSets/positiveEmoticons.txt");
		//initializeHashSet(TextFeatures.positiveAdjectives, "data/hashSets/positiveAdjectives.txt");
		//initializeHashSet(TextFeatures.negativeAdjectives, "data/hashSets/negativeAdjectives.txt");
		//initializeHashSet(TextFeatures.neutralAdjectives, "data/hashSets/neutralAdjectives.txt");
		//And following, all lists of word classes.

	}

	public static void initializeHashSet(HashSet<String> names, String namePath) {

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
		****************************
		*         Features         *
		* **************************
	 */

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

	/*
    Check if the tweet contains strings of multiple exclamation points together and strings of
    multiple question marks together
 	*/
	public static int containsMultipleExclamationsQuestions(String tweet) {

		generalMatcher = multipleExclamationsPattern.matcher(tweet);
		if (generalMatcher.find()) {
			return 1;
		}
		return 0;
	}

	/*
	 * Checks if the first name of the user
	 * appears in a large list of human first names.
	 *
	 * */

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
	 * Methods seeks out use of the word official
	 * This method is primarily for twitter handle/name
	 * */

	public static int containsOfficial(String tweet) {
		if (tweet.toLowerCase().contains("official")) {
			return 1;
		}
		return 0;
	}

	public static int containsTimeMark(String tweet) {

		generalMatcher = timePattern.matcher(tweet);

		if (generalMatcher.find()) {
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
		Counts the number of instances of a given pattern
	 */
	public static int countInstancesOf(String text, Pattern pattern) {
		int count = 0;
		generalMatcher = pattern.matcher(text);
		while (generalMatcher.find()) {
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
	 * Counts the number of times the description uses the phrases:
	 * Follow (us)
	 * Tweet(s)
	 * CASE INSENSITIVE
	 * */
	public static int countSubscriptionPhrases(String description) {

		int count = 0;
		generalMatcher = subscriptionPhrasePattern.matcher(description);

		while (generalMatcher.find()) {
			count++;
		}
		return count + checkOutFeature(description);
	}

	/*
	 * Checks if the first name of the user
	 * appears in a large list of human first names.
	 * */
	public static int firstWordIsCommonFirstName(String name) { //possibly take out some chars
		generalMatcher = firstNamePattern.matcher(name);
		if (generalMatcher.find()) name = generalMatcher.group(2).toLowerCase();

		if (firstNames.isEmpty()) initializeHashSet(firstNames, "data/hashSets/FirstNames.csv");

		Iterator<String> names = firstNames.iterator();
		while (names.hasNext()) {
			String nam = names.next().toLowerCase();
			//System.out.println(nam+", "+name);
			if (nam.equals(name)) return 1;
		}

		return 0;
	}

	/*
    Checks to see how many times the tweet contains strings of multiple exclamation points together and strings of
    multiple question marks together
	 */
	public static int getFeatureForMultipleExclamationsQuestions(String tweet) {
		int count = 0;

		generalMatcher = multipleExclamationsPattern.matcher(tweet);
		while (generalMatcher.find()) {
			count++;
		}
		return count;
	}

	/*
    Counts the number of phrases ending in a single or multiple exclamation points
	 */
	public static int getFeatureForNumberOfExclamationPhrases(String tweet) {
		int count = 0;

		generalMatcher = numberOfExclamationsPattern.matcher(tweet);
		while (generalMatcher.find()) {
			count++;
		}
		return count;
	}

	public static ArrayList<String> getHashtags(String tweet) {

		generalMatcher = hashtagPattern.matcher(tweet);
		ArrayList<String> hashtags = new ArrayList<String>();

		while (generalMatcher.find()) {
			hashtags.add(generalMatcher.group(1));
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

	/*
	 * Checks to see if all text in a tweet is uppercase
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

	/*
	 * Determines the first word of the name, excluding "The",
	 * and checks if it appears in the description.
	 * */
	public static int isNameInDescription(String name, String description) {

		generalMatcher = firstWordExcludeThe.matcher(name);

		if (generalMatcher.find()) {
			name = generalMatcher.group().toLowerCase();
		}

		if (description.toLowerCase().contains(name)) {
			return 1;
		}
		return 0;
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
	 *
	 * Esoteric and did not show to be much help. Avoiding implementation.
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
		generalMatcher = lastNamePattern.matcher(name);
		if (generalMatcher.find()) name = generalMatcher.group(2).toLowerCase();

		if (lastNames.isEmpty()) initializeHashSet(lastNames, "data/hashSets/LastNames.csv");

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

		generalMatcher = pluralPersonalPronounsLocator.matcher(tweet.toLowerCase());
		int count = 0;

		while (generalMatcher.find()) {
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


	/*
	 * ------------------------MULTICLASS FEATURES------------------*
	/*search_terms = ['baby','birth','birth','became a mom','become a mom','have a kid','had a kid','new job','start working','started working',
	 * 'begin new job','begin working','engaged','popped the question','buy a diamond ring','bought a diamond ring','buying a diamond ring',
	 * 'engagement','have a baby','had a baby','having a baby','birth','became a mother','becoming a mother','became a mom',
	 * 'becoming a mom','have a kid','had a kid','having a kid','married' 'wedding','marry','graduate','graduating','graduated',
	 * 'graduation','trip','get into','got into','accepted into', 'admitted','died','dying','pass away','passed away', 'got a divorce',
	 * 'getting a divorce','separated','death','fall in love','fall for','crush on','have the hots for','fall in love','falling in love',
	 * 'fell in love','in love', 'crush on','fell out of love','broke up','break up','separate','separated','dumped','breakup','fight',
	 * 'argument','argue','fought','accident','robbed','mugged','mug','rob','robbery','fired','lost my job','was laid off','got laid off',
	 * 'lost job','change job','changed job','job loss','pregnant','pregnancy','ill','illness','got cancer','got HIV','got AIDS','got a STD',
	 * 'have cancer','had a heart attack','have a heart disease','have a serious illness', 'diagnosed','cancer','lymphoma','pancretic',
	 * 'cardiac','ill','sick','kicked out','rejected','expelled','reject','rejection','retired','retiring','retirement','move',
	 * 'moving','moved','leave','leaving','left','relocating','relocate','relocated','relocation','travel','travelled','traveling',
	 * 'went to','arrived at','flew to','flying to','arriving','arrive','dumped','rejected','dump','reject','rejection']
	 * */

	public static int countNegativeEmoticons(String tweet) {

		if (negativeEmoticons.isEmpty()) {
			System.out.println("Dictionary negativeEmoticons is not initialized.");
			return 0;
		}

		String[] tokens = tweet.split("\\s");
		int count = 0;

		for (String token : tokens) {
			if (negativeEmoticons.contains(token)) {
				count++;
			}
		}
		return count;
	}


	public static int countPositiveEmoticons(String tweet) {

		if (positiveEmoticons.isEmpty()) {
			System.out.println("Dictionary positiveEmoticons is not initialized.");
			return 0;
		}

		String[] tokens = tweet.split("\\s");
		int count = 0;

		for (String token : tokens) {
			if (positiveEmoticons.contains(token)) {
				count++;
			}
		}
		return count;
	}

	/*
	 * Using a count method because the more of the words in the dict
	 * show up, the better the probability of the tweet being
	 * about that subject.
	 *
	 * Method should likely be rewritten using Stan to avoid this
	 * splitting process
	 *
	 * */

	public static int countWordsInDict(String tweet, HashSet<String> dict) {
		/*
		 * Numerous methods would be called, each with its corresponding dictionary.
		 * A count would be returned for the number of words in the tweet
		 * that appear in the aforementioned dictionary.
		 *
		 * Major_Trip - List of touristy countries, beach, mountains, forests, states,
		 *
		 *
		 * */
		if (dict.isEmpty()) {
			System.out.println("Dictionary is not initialized.");
			return 0;
		}

		int count = 0;
		String[] splitTweet = tweet.toLowerCase().split("\\s");

		for (String s : splitTweet) {
			if (dict.contains(s)) {
				count++;
			}
		}
		return count;
	}

	/*
	 * Seems like Hashtags could be a key feature for determining
	 * the context of the tweet
	 *
	 * Though hashtags should be found, and then the words run
	 * against one of the word class dictionaries.
	 *
	 *
	 * DO NOT IMPLEMENT
	 *
	public static int countHashtagsInDict(String tweet) {
		if (listOfHashTags.isEmpty()) {
			System.out.println("Dictionary listOfHashTags is not initialized.");
		}
		int count = 0;
		//Hashtag pattern returns the hashtag with the word
		generalMatcher = hashtagPattern.matcher(tweet);
		while (generalMatcher.find()) {
			String group = generalMatcher.group().substring(1);
			if (listOfHashTags.contains(group)) {
				count++;
			}
			else {
				//Helps the hashtag dictionary
				listOfHashTags.add(group);
			}
		}
		return count;
	}*/

	public static int mentionsFamily(String tweet) throws IOException {

		if (listOfWordsFamily.isEmpty()) {
			System.out.println("Dictionary is not initialized.");
			return 0;
		}

		String[] splitTweet = tweet.split("\\s");

		for (String s : splitTweet) {
			if (listOfWordsFamily.contains(s.toLowerCase())) {
				return 1;
			}
		}
		return 0;
	}
}
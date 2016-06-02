import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.*;

import javax.xml.soap.Text;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.util.*;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


/**
 * Created by Alec Wolyniec on 4/14/16.
 */

/*
    Methods useful for creating a vector model from each tweet in an input set, representing all of the features
    used in Lamb, Paul, and Dredze 2013
 */
public class readTweetsGetFeatures {
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
                "son", "daughter"}
    };

    /*
        Get tweets from a path to a file
    */
    public static ArrayList<String[]> getTweets(String pathToTweetFile) throws FileNotFoundException, IOException {
        ArrayList<String[]> tweets = new ArrayList<String[]>();
        BufferedReader reader = new BufferedReader(new FileReader(pathToTweetFile));
        String currentLine;
        while ((currentLine = reader.readLine()) != null) {
            String[] split = currentLine.split("\t");
            if (split.length < 3) break;
            String text = "";
            for (int i = 2; i < split.length; i++) {
                text += split[i];
            }
            String[] tweet = {split[0], split[1], text};
            tweets.add(tweet);
        }
        return tweets;
    }

    /*
        From a collection of tweets, set up a Stanford CoreNLP annotator to use, and create a vector model for each
        tweet using the features for the relevant type of classifier
     */
    public static TweetVector[] getVectorModelsFromTweets(ArrayList<String[]> tweets, String classifierType) throws IOException{
        //set up Stanford CoreNLP object for annotation
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //get tweet vector model
        TweetVector[] tweetVectors = new TweetVector[tweets.size()];
        for (int i = 0; i < tweets.size(); i++) {
            tweetVectors[i] = getVectorModelFromTweet(tweets.get(i), pipeline, classifierType);
        }
        return tweetVectors;
    }

    /*
        Generate the vector model of a single tweet. Pre-process, annotate, represent the tweet in terms of phrases,
        then collect phrases

        Input tweet is formatted as follows:
        {name, label, text}
     */
    public static TweetVector getVectorModelFromTweet(String[] tweet, StanfordCoreNLP pipeline, String classifierType) throws IOException {
        TweetVector tweetVector = new TweetVector(tweet[0], tweet[1]);
        CoreLabel[][] phrases = new CoreLabel[1][];
        int numPhrases = 0;
        //annotate with ARK POS tagger (remove emoticons and other twitter stylometry)
        ARKFeatures.loadModelStatically();

        //annotate with Stanford CoreNLP
        Annotation document = new Annotation(tweet[2]);
        pipeline.annotate(document);

        //collect phrases. When a phrase has been completed, collect features from it
        List<CoreLabel> tokens = document.get(TokensAnnotation.class);
        CoreLabel[] phrase = new CoreLabel[1];
        int phraseCounter = 0;
        for (int i = 0; i < tokens.size(); i++) {
            CoreLabel token = tokens.get(i);
            String text = token.get(TextAnnotation.class);

            //Add token to current phrase
            phrase[phraseCounter++] = token;
            if (phraseCounter >= phrase.length) {
                //double array
                CoreLabel[] newPhrase = new CoreLabel[phraseCounter * 2];
                for (int j = 0; j < phrase.length; j++) {
                    newPhrase[j] = phrase[j];
                }
                phrase = newPhrase;
            }

            //start next phrase if the phrase is completed, or if there are no more tokens to collect
            if ((isPunctuation(token.originalText())) || i == tokens.size() - 1) {
                CoreLabel[] noNullPhrase = new CoreLabel[phraseCounter];
                //remove null entries from phrase
                for (int j = 0; j < phraseCounter; j++) {
                    noNullPhrase[j] = phrase[j];
                }
                //add phrase to phrase array
                phrases[numPhrases++] = noNullPhrase;
                //double phrase array if needed
                if (numPhrases == phrases.length) {
                    CoreLabel[][] newPhrases = new CoreLabel[phrases.length * 2][];
                    for (int j = 0; j < phrases.length; j++) {
                        newPhrases[j] = phrases[j];
                    }
                    phrases = newPhrases;
                }
                //new phrase
                phrase = new CoreLabel[1];
                phraseCounter = 0;
            }
        }
        //remove null entries from phrases
        CoreLabel[][] noNullPhrases = new CoreLabel[numPhrases][];
        for (int i = 0; i < numPhrases; i++) {
            noNullPhrases[i] = phrases[i];
        }
        //collect features
        switch (classifierType) {
            case "HumanVsNonHuman": collectFeaturesHumanVsNonHuman(tweetVector, noNullPhrases, tweet[2]); break;
            case "EventVsNonEvent": collectFeaturesEventVsNotEvent(tweetVector, noNullPhrases, tweet[2]); break;
            case "SelfVsOther": collectFeaturesSelfVsOther(tweetVector, noNullPhrases, tweet[2]); break;
        }
        return tweetVector;
    }

    /*
        Obtain all features for the human vs. non-human classifier
    */
    public static void collectFeaturesHumanVsNonHuman(TweetVector tweetVector, CoreLabel[][] phrases, String tweetText) {
        tweetVector.addFeature("Word classes-Self", getFeatureForWordClass(phrases, "Self"));
        tweetVector.addFeature("Word classes-Others", getFeatureForWordClass(phrases, "Others"));
        tweetVector.addFeature("Number of phrases ending in exclamations", getFeatureForNumberOfExclamationPhrases(tweetText));
        tweetVector.addFeature("Multiple exclamations, multiple question marks", getFeatureForMultipleExclamationsQuestions(tweetText));
        tweetVector.addFeature("Check x out string", TextFeatures.checkOutFeature(tweetText));
        tweetVector.addFeature("Mentions of users", TextFeatures.containsAt(tweetText));
        tweetVector.addFeature("The word 'deal'", TextFeatures.containsDeal(tweetText));
        tweetVector.addFeature("The word 'link'", TextFeatures.containsLink(tweetText));
        //tweetVector.addFeature("URL links", TextFeatures.containsURL(tweetText));
        //tweetVector.addFeature("Tweet is a question", TextFeatures.isQuestionTweet(tweetText));
        //tweetVector.addFeature("Personal plural pronouns", TextFeatures.numPluralPersonalPronouns(tweetText));
    }

    /*
        Obtain all features for the life event vs. not life event classifier
     */
    public static void collectFeaturesEventVsNotEvent(TweetVector tweetVector, CoreLabel[][] phrases, String tweetText) {

    }

    /*
        Obtain all features for the self vs. other classifier
     */
    public static void collectFeaturesSelfVsOther (TweetVector tweetVector, CoreLabel[][] phrases, String tweetText) {
        //the number of words/strings in each of the given word classes
        for (String[] aClass: wordClasses) {
            tweetVector.addFeature("Word classes-"+aClass[0], getFeatureForWordClass(phrases, aClass[0]));
        }

        //phrase-based features
        for (CoreLabel[] phrase: phrases) {
            //ArrayList<StringFeatureValuePair> featuresForPhrase = collectFeaturesForPhrase(phrase);
            //for (int i = 0; i < featuresForPhrase.size(); i++) tweetVector.addFeature(featuresForPhrase.get(i));
        }

        //other features

        //test - print tweet
        Hashtable<String, Integer> featureValPairs = tweetVector.getFeatures();
        util.printStringFeaturesIntValuesFromHashtable(featureValPairs);
        //System.out.println("Tweet name: "+tweetVector.getName());
        //System.out.println("Tweet label: "+tweetVector.getLabel());
        //System.out.println();
    }



    /*
        Obtain all features for an individual phrase
     */
    /*
    public static ArrayList<StringFeatureValuePair> collectFeaturesForPhrase(CoreLabel[] phrase) {
        ArrayList<StringFeatureValuePair> featuresForPhrase = new ArrayList<StringFeatureValuePair>();

        //get features based on part-of-speech templates
        ArrayList<StringFeatureValuePair> posTemplateFeatures = collectFeaturesForPhraseTemplates(phrase);
        for (int i = 0; i < posTemplateFeatures.size(); i++) featuresForPhrase.add(posTemplateFeatures.get(i));

        return featuresForPhrase;
    }
    */

    /*
        Obtain all template-based features for a phrase
     */
    /*
    public static ArrayList<StringFeatureValuePair> collectFeaturesForPhraseTemplates(CoreLabel[] phrase) {
        ArrayList<StringFeatureValuePair> featuresForTemplate = new ArrayList<StringFeatureValuePair>();
        //get templates
        String[] templates = getPOSTemplates(phrase);

        return featuresForTemplate;
    }
    */

    /*
        Get specified part-of-speech templates for a single phrase
        Templates: (subject, verb, object), (subject, verb), (subject, object), (verb, object)
        Subject: First noun/pronoun in the phrase
        Verb: First verb after the subject
        Object: Any noun/pronoun after the verb
     */
    public static String[] getPOSTemplates(CoreLabel[] phrase) {
        return new String[0];
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
            System.err.println("ERROR: Word class requested,"+relevantClassName+", does not exist.");
            System.exit(1);
        }
        //go over each phrase
        for (CoreLabel[] phrase: phrases) {
            //go through each word in the phrase
            for (int i = 0; i < phrase.length; i++) {
                CoreLabel token = phrase[i];
                String stringInPhrase = token.get(TextAnnotation.class);
                String stringInPhrasePOS = token.get(PartOfSpeechAnnotation.class);
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
                            buildMatch.append(phrase[parallelCount].get(TextAnnotation.class));
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
}

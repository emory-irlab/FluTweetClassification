import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.*;
import java.util.*;


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
    private static String[][] wordClasses = {
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
                "I", "I've", "I'd", "I'm", "im", "my"},
            {"Others",
                "your", "everyone", "you", "it", "its", "u", "her", "he", "she", "he's", "she's", "they", "you're",
                "she'll", "he'll", "husband", "wife", "brother", "sister", "people", "kid", "kids", "children",
                "son", "daughter"}
    };

    /*
        From a collection of tweets, set up a Stanford CoreNLP annotator to use, and create a vector model for each
        tweet
     */
    public static TweetVector[] getVectorModelsFromTweets(String[] tweets) {
        //set up Stanford CoreNLP object for annotation
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //get tweet vector model
        TweetVector[] tweetVectors = new TweetVector[tweets.length];
        for (int i = 0; i < tweets.length; i++) {
            tweetVectors[i] = getVectorModelFromTweet(tweets[i], pipeline);
        }
        return tweetVectors;
    }

    /*
        Generate the vector model of a single tweet. Pre-process, annotate, represent the tweet in terms of phrases,
        then collect phrases
     */
    public static TweetVector getVectorModelFromTweet(String tweet, StanfordCoreNLP pipeline) {
        TweetVector tweetVector = new TweetVector("test ID");
        CoreLabel[][] phrases = new CoreLabel[1][];
        int numPhrases = 0;
        //annotate with ARK POS tagger (remove emoticons and other twitter stylometry; Stanford CoreNLP separates emoticons)

        //annotate with Stanford CoreNLP
        Annotation document = new Annotation(tweet);
        pipeline.annotate(document);

        //collect phrases. When a phrase has been completed, collect features from it
        /*
            Delimit phrases by any of the following punctuation marks: . , " ' ( ) [ ] ! ? ;
         */
        String[] delimiters = {".", ",", "\"", "\'", "(", ")", "[", "]", "!", "?", ";", "``", "`", "''", "'"}; //may not have all punctuation variants
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

            //start new phrase if the phrase is completed, or if there are no more tokens to collect
            for (String delimiter: delimiters) {
                if (text.equals(delimiter) || i == tokens.size() - 1) {
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
                    break;
                }
            }
        }
        //remove null entries from phrases
        CoreLabel[][] noNullPhrases = new CoreLabel[numPhrases][];
        for (int i = 0; i < numPhrases; i++) {
            noNullPhrases[i] = phrases[i];
        }
        //collect features
        collectFeatures(tweetVector, noNullPhrases);
        return tweetVector;
    }

    /*
        Obtain all features for the tweet vector
     */
    public static void collectFeatures (TweetVector tweetVector, CoreLabel[][] phrases) {

        //the number of words/strings in each of the given word classes
        StringFeatureValuePair[] wordClassFeatures = getWordClassFeatures(phrases);
        for (StringFeatureValuePair feature: wordClassFeatures) tweetVector.addFeature(feature);
        //test getting the features out...issues with typecasting


        //other features
    }

    /*
        Count the number of words/strings in each of the given word classes. Create features accordingly, one for
        each word class
     */
    public static StringFeatureValuePair[] getWordClassFeatures(CoreLabel[][] phrases) {
        StringFeatureValuePair[] wordClassFeatures = new StringFeatureValuePair[8];
        for (int i = 0; i < wordClasses.length; i++) {
            wordClassFeatures[i] = new StringFeatureValuePair("Word Classes-"+wordClasses[i][0], 0);
        }
        //get features for each phrase
        for (CoreLabel[] phrase: phrases) {
            //go through each word in the phrase
            for (int i = 0; i < phrase.length; i++) {
                CoreLabel token = phrase[i];
                String stringInPhrase = token.get(TextAnnotation.class);
                System.out.println(stringInPhrase);
                String stringInPhrasePOS = token.get(PartOfSpeechAnnotation.class);

                //go through each of the word classes, add to the count of the class the word is in, if applicable
                for (int j = 0; j < wordClasses.length; j++) {
                    String stringInPhraseCopy = stringInPhrase; //use this when referring to the input token
                    StringFeatureValuePair relevantWordFeature = wordClassFeatures[j];
                    String[] relevantWordClass = wordClasses[j];
                    //get words to match
                    for (int k = 1; k < relevantWordClass.length; k++) {
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
                            System.out.println("Matched string \""+stringInPhraseCopy+"\" to string \""+stringToMatch+"\" in class "+relevantWordClass[0]);
                            relevantWordFeature.incrementValue(1);
                            break;
                        }
                    }
                }
            }
        }

        //for testing purposes
        /*
        System.out.println("PRINTING WORD CLASS FEATURES:");
        for (StringFeatureValuePair pair: wordClassFeatures) {
            System.out.println(pair.getFeature()+": "+pair.getValue());
        }
        */
        return wordClassFeatures;
    }

    public static void main (String[] args) {
        String exampleTweet1 = "oh I have the flu. It's a majorepidemic. I'm going to Norway. I'm scared";
        String exampleTweet2 = "Over there, in the parking garage. What thing? The detested thing? Mr. Burling?";
        String[] tweets = {exampleTweet1, exampleTweet2};
        TweetVector[] tweetsInVectorForm = getVectorModelsFromTweets(tweets);
    }
}

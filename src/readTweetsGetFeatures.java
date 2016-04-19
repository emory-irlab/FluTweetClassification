import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.*;
import java.util.*;


/**
 * Created by Alec Wolyniec on 4/14/16.
 */

/*
    Methods useful for creating a vector model from each tweet in an input set, to include
    all features relevant to the scope of the project
 */
public class readTweetsGetFeatures {
    /*
        Pre-defined word classes.


        Notes:
         - "[2-9]" symbols denote multi-word features. Extraction algorithm should search multiple words accordingly
         - "V-" denotes a verb ending. The feature extraction algorithm should match this entry to the ending of a verb
           word being scanned, and not the word itself
     */
    private static String[][] wordClasses = {
            {"Infection",
                "getting", "got", "recovered", "have", "having", "had", "has", "catching", "catch", "cured", "infected"},
            {"Possession",
                "bird", "2the flu", "sick", "epidemic"},
            {"Concern",
                "afraid", "worried", "scared", "fear", "worry", "nervous", "dread", "dreaded", "terrified"},
            {"Vaccination",
                "vaccine", "vaccines", "shot", "shots", "mist", "tamiflu", "jab", "2nasal spray"},
            {"Past Tense",
                "was", "did", "had", "got", "were", "V-ed"},
            {"Present Tense",
                "is", "am", "are", "have", "has", "V-ing"},
            {"Self",
                "I", "I've", "I'd", "I'm", "im", "my"},
            {"Others",
                "your", "everyone", "you", "it", "its", "u", "her", "he", "she", "he's", "she's", "they", "you're",
                "she'll", "he'll", "husband", "wife", "brother", "sister", "people", "kid", "kids", "children",
                "son", "daughter"}
    };

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

    public static void collectFeatures (TweetVector tweetVector, CoreLabel[][] phrases) {
        //initialize features that are updated for multiple phrases
         //word class features
        StringFeatureValuePair[] wordClassFeatures = new StringFeatureValuePair[8];
        wordClassFeatures[0] = new StringFeatureValuePair("Word Classes-Infection", 0);
        wordClassFeatures[1] = new StringFeatureValuePair("Word Classes-Possession", 0);
        wordClassFeatures[2] = new StringFeatureValuePair("Word Classes-Concern", 0);
        wordClassFeatures[3] = new StringFeatureValuePair("Word Classes-Vaccination", 0);
        wordClassFeatures[4] = new StringFeatureValuePair("Word Classes-Past Tense", 0);
        wordClassFeatures[5] = new StringFeatureValuePair("Word Classes-Present Tense", 0);
        wordClassFeatures[6] = new StringFeatureValuePair("Word Classes-Self", 0);
        wordClassFeatures[7] = new StringFeatureValuePair("Word Classes-Others", 0);

        //get features for each phrase
        for (CoreLabel[] phrase: phrases) {
            //go through each word in the phrase
            for (int i = 0; i < phrase.length; i++) {
                CoreLabel token = phrase[i];
                String text = token.get(TextAnnotation.class);
                String pos = token.get(PartOfSpeechAnnotation.class);

                //word class features (make sure to use equalsIgnoreCase)
                //go through all the word classes and their entries, compare with the token at hand
                for (int j = 0; j < wordClasses.length; j++) {
                    StringFeatureValuePair relevantWordFeature = wordClassFeatures[j];


                }

                //x feature
            }
        }
    }

    public static void main (String[] args) {
        String exampleTweet1 = "We can neither confirm nor deny that this is our first tweet; that is all.";
        String exampleTweet2 = "He said, \"I will sell you that car for $20\".";
        String[] tweets = {exampleTweet1, exampleTweet2};
        TweetVector[] tweetsInVectorForm = getVectorModelsFromTweets(tweets);
    }
}

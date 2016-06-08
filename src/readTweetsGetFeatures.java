import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.*;
import org.apache.commons.csv.*;

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
        Get csv-formatted tweets from a path to a file

        Fields should be as follows: profile picture, handle, name, description, tweet, label
     */
    public static ArrayList<String[]> getTweets(String pathToTweetFile) throws FileNotFoundException, IOException {
        ArrayList<String[]> tweets = new ArrayList<String[]>();
        BufferedReader in = new BufferedReader(new FileReader(pathToTweetFile));
        Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
        for (CSVRecord record : records) {
            String[] tweetFields = new String[6];
            for (int i = 0; i < 6; i++) {
                tweetFields[i] = record.get(i);
            }
            tweets.add(tweetFields);
        }
        return tweets;
    }

    /*
        Get tweets from a path to a file
    */
    /*
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
    */

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
        {profile pic, handle, name, description, tweet, label}
     */
    public static TweetVector getVectorModelFromTweet(String[] tweet, StanfordCoreNLP pipeline, String classifierType) throws IOException {
        String label = toBinaryLabels(tweet[5], classifierType);
        TweetVector tweetVector = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], label);
        CoreLabel[][] phrases = new CoreLabel[1][];
        int numPhrases = 0;
        //annotate with ARK POS tagger (remove emoticons and other twitter stylometry)
        //ARKFeatures.loadModelStatically();

        //annotate with Stanford CoreNLP
        Annotation document = new Annotation(tweet[4]);
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
            if ((TextFeatures.isPunctuation(token.originalText())) || i == tokens.size() - 1) {
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
            case "HumanVsNonHuman":
                collectFeaturesHumanVsNonHuman(tweetVector, noNullPhrases);
                break;
            case "EventVsNonEvent":
                collectFeaturesEventVsNotEvent(tweetVector, noNullPhrases);
                break;
            case "SelfVsOther":
                collectFeaturesSelfVsOther(tweetVector, noNullPhrases);
                break;
        }
        return tweetVector;
    }

    /*
        Obtain all features for the human vs. non-human classifier
    */
    public static void collectFeaturesHumanVsNonHuman(TweetVector tweetVector, CoreLabel[][] phrases) {

        //features based on the user's profile pic

        //features based on the user's username
        String username = tweetVector.getUsername();
        tweetVector.addFeature("Digits in username", TextFeatures.containsDigits(username));

        //features based on the user's name
        String name = tweetVector.getName();
        tweetVector.addFeature("Digits in name", TextFeatures.containsDigits(name));
        tweetVector.addFeature("Common first name", TextFeatures.firstWordIsCommonFirstName(name));
        tweetVector.addFeature("Common last name", TextFeatures.lastWordIsCommonLastName(name));
        tweetVector.addFeature("Space groups-ceiling 3", Math.min(TextFeatures.countSpaceGroups(name), 3));
        tweetVector.addFeature("Upper case sequence", TextFeatures.containsUpperCaseSequence(name));
        tweetVector.addFeature("All upper case", TextFeatures.isAllUpperCase(name));


        //features based on the user's profile description

        //features based on the tweet
        String text = tweetVector.getTweetText();
        tweetVector.addFeature("Word classes-Self", TextFeatures.getFeatureForWordClass(phrases, "Self"));
        tweetVector.addFeature("Word classes-Others", TextFeatures.getFeatureForWordClass(phrases, "Others"));
        //including plural 1p pronouns may decrease accuracy somewhat
        tweetVector.addFeature("Word classes-Plural 1P pronouns", TextFeatures.getFeatureForWordClass(phrases, "Plural 1P pronouns"));
        tweetVector.addFeature("Number of phrases ending in exclamations", TextFeatures.getFeatureForNumberOfExclamationPhrases(text));
        tweetVector.addFeature("Multiple exclamations, multiple question marks", TextFeatures.getFeatureForMultipleExclamationsQuestions(text));
        tweetVector.addFeature("Check x out string", TextFeatures.checkOutFeature(text));
        tweetVector.addFeature("Mentions of users", TextFeatures.containsMention(text));
        tweetVector.addFeature("The word 'deal'", TextFeatures.containsDeal(text));
        tweetVector.addFeature("The word 'link'", TextFeatures.containsLink(text));
        tweetVector.addFeature("URL links", TextFeatures.containsURL(text));
        //tweetVector.addFeature("Tweet consists of a single question with a URL", TextFeatures.isSingleQuestionURLTweet(text));
    }

    /*
        Obtain all features for the life event vs. not life event classifier
     */
    public static void collectFeaturesEventVsNotEvent(TweetVector tweetVector, CoreLabel[][] phrases) {

    }

    /*
        Obtain all features for the self vs. other classifier
     */
    public static void collectFeaturesSelfVsOther (TweetVector tweetVector, CoreLabel[][] phrases) {
        //the number of words/strings in each of the given word classes
        tweetVector.addFeature("Word classes-Infection", TextFeatures.getFeatureForWordClass(phrases, "Infection"));
        tweetVector.addFeature("Word classes-Possession", TextFeatures.getFeatureForWordClass(phrases, "Possession"));
        tweetVector.addFeature("Word classes-Concern", TextFeatures.getFeatureForWordClass(phrases, "Concern"));
        tweetVector.addFeature("Word classes-Vaccination", TextFeatures.getFeatureForWordClass(phrases, "Vaccination"));
        tweetVector.addFeature("Word classes-Past Tense", TextFeatures.getFeatureForWordClass(phrases, "Past Tense"));
        tweetVector.addFeature("Word classes-Present Tense", TextFeatures.getFeatureForWordClass(phrases, "Present Tense"));
        tweetVector.addFeature("Word classes-Self", TextFeatures.getFeatureForWordClass(phrases, "Self"));
        tweetVector.addFeature("Word classes-Others", TextFeatures.getFeatureForWordClass(phrases, "Others"));

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

    public static String toBinaryLabels(String input, String classifierType) {
        switch (classifierType) {
            case "HumanVsNonHuman":
                if (input.equals("person")) return "0";
                if (input.equals("organization")) return "1"; break;
        }
        return "";
    }

}

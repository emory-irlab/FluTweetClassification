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
    private static TweetVector[] tweetVectors;
    private static int idfUpdateCounter = 0;
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
        Pre-processes the tweet to remove all hashtags (but not their text), @ characters in user mentions, URLs,
        retweets, and emoticons. Also converts space groups to single spaces and converts characters repeated 3+ times
        to a sequence of the character repeated only twice ("hellooooooo" becomes "helloo")
     */
    public static String process(String input) {
        input = TextFeatures.removeRetweets(input); //retweets must be removed before at character
        input = TextFeatures.removeHashtagCharInHashtags(input);
        input = TextFeatures.removeAtCharInMentions(input);
        input = TextFeatures.removeURL(input);
        //remove emoticons
        //converts space groups to single spaces
        input = input.replaceAll(TextFeatures.spaceGroup, " ");
        input = TextFeatures.removeCharsRepeated3PlusTimes(input);

        return input;
    }

    /*
        From a collection of tweets, set up a Stanford CoreNLP annotator to use, and create a vector model for each
        tweet using the features for the relevant type of classifier

        Each input tweet is formatted as follows:
        {profile pic, username, name, description, tweet, label}
     */
    public static TweetVector[] getVectorModelsFromTweets(ArrayList<String[]> tweets, String classifierType) throws IOException{
        ArrayList<String> labelSet = new ArrayList<String>(0);
        //set up Stanford CoreNLP object for annotation
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //get tweet vector model
        TweetVector[] tweetVectors = new TweetVector[tweets.size()];
        readTweetsGetFeatures.tweetVectors = tweetVectors;

         //initialize tweet vectors
        //String label = toBinaryLabels(tweet[5], classifierType);
        for (int i = 0; i < tweets.size(); i++) {
            //String label = toBinaryLabels(tweet[5], classifierType);
            String[] tweet = tweets.get(i);
            tweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5], labelSet);
        }

         //fill tweet vectors with features
        for (int i = 0; i < tweets.size(); i++) {
            getVectorModelForTweet(tweetVectors[i], pipeline, classifierType);
        }
        return tweetVectors;
    }

    /*
        Generate the vector model of a single tweet. Get text (process the text of the tweet itself), annotate text,
        represent the annotated tweet in terms of phrases, collect phrases, collect features.
     */
    public static void getVectorModelForTweet(TweetVector tweetVector, StanfordCoreNLP pipeline, String classifierType) throws IOException {
        //get processed text
        String processedTweet = process(tweetVector.getTweetText());

        //annotate fields with Stanford CoreNLP

        //description
        Annotation descriptionDocument = new Annotation(tweetVector.getDescription());
        pipeline.annotate(descriptionDocument);
        CoreLabel[][] descriptionPhrases = getPhrases(descriptionDocument);

        //tweet
        Annotation tweetDocument = new Annotation(processedTweet);
        pipeline.annotate(tweetDocument);
        CoreLabel[][] tweetPhrases = getPhrases(tweetDocument);

        //collect features
        switch (classifierType) {
            case "HumanVsNonHuman":
                collectFeaturesHumanVsNonHuman(tweetVector, descriptionPhrases, tweetPhrases);
                break;
            case "EventVsNonEvent":
                collectFeaturesEventVsNotEvent(tweetVector, tweetPhrases);
                break;
            case "SelfVsOther":
                collectFeaturesSelfVsOther(tweetVector, tweetPhrases);
                break;
        }
    }

    public static CoreLabel[][] getPhrases(Annotation document) {
        CoreLabel[][] phrases = new CoreLabel[1][];
        int numPhrases = 0;

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

        return noNullPhrases;
    }

    /*
        Obtain all features for the human vs. non-human classifier
    */
    public static void collectFeaturesHumanVsNonHuman(TweetVector tweetVector, CoreLabel[][] descriptionPhrases, CoreLabel[][] tweetPhrases) {

        //features based on the user's profile pic

        //features based on the user's username
        String username = tweetVector.getUsername();
        tweetVector.addFeature("Username-Digits", TextFeatures.containsDigits(username));

        //features based on the user's name
        String name = tweetVector.getName();
        tweetVector.addFeature("Name-Digits", TextFeatures.containsDigits(name));
        tweetVector.addFeature("Name-Common first name", TextFeatures.firstWordIsCommonFirstName(name));
        tweetVector.addFeature("Name-Common last name", TextFeatures.lastWordIsCommonLastName(name));
        tweetVector.addFeature("Name-Space groups-ceiling 3", Math.min(TextFeatures.countSpaceGroups(name), 3));
        tweetVector.addFeature("Name-Upper case sequence", TextFeatures.containsUpperCaseSequence(name));
        tweetVector.addFeature("Name-All upper case", TextFeatures.isAllUpperCase(name));

        //features based on the user's profile description
        String description = tweetVector.getDescription();
        tweetVector.addFeature("Description-Word classes-Org. Account Descriptions",
                AnnotationFeatures.getFeatureForWordClass(descriptionPhrases, AnnotationFeatures.orgAccountDescriptionsWordClassName));
        tweetVector.addFeature("Description-Check x out string", TextFeatures.checkOutFeature(description));
        tweetVector.addFeature("Description-Mentions social media", TextFeatures.mentionsSocialMedia(description));
        tweetVector.addFeature("Description-Word classes-Self",
                AnnotationFeatures.getFeatureForWordClass(descriptionPhrases, AnnotationFeatures.selfWordClassName));
        tweetVector.addFeature("Description-Word classes-Plural 1P pronouns",
                AnnotationFeatures.getFeatureForWordClass(descriptionPhrases, AnnotationFeatures.plural1PPronounsWordClassName));
        tweetVector.addFeature("Description-Word classes-2P pronouns",
                AnnotationFeatures.getFeatureForWordClass(descriptionPhrases, AnnotationFeatures._2PPronounsWordClassName));
        tweetVector.addFeature("Description-Word classes-Person punctuation",
                AnnotationFeatures.getFeatureForWordClass(descriptionPhrases, AnnotationFeatures.personPunctuationWordClassName));
        tweetVector.addFeature("Description-Verb count", AnnotationFeatures.verbsCount(descriptionPhrases));

        //features based on the tweet
        String text = tweetVector.getTweetText();
        tweetVector.addFeature("Tweet-Word classes-Self",
                AnnotationFeatures.getFeatureForWordClass(tweetPhrases, AnnotationFeatures.selfWordClassName));
        tweetVector.addFeature("Tweet-Word classes-Others",
                AnnotationFeatures.getFeatureForWordClass(tweetPhrases, AnnotationFeatures.othersWordClassName));
        //including plural 1p pronouns may decrease accuracy somewhat
        tweetVector.addFeature("Tweet-Word classes-Plural 1P pronouns",
                AnnotationFeatures.getFeatureForWordClass(tweetPhrases, AnnotationFeatures.plural1PPronounsWordClassName));
        tweetVector.addFeature("Tweet-Phrases ending in exclamations", TextFeatures.countExclamationPhrases(text));
        tweetVector.addFeature("Tweet-Multiple exclamations, multiple question marks", TextFeatures.containsMultipleExclamationsQuestions(text));
        tweetVector.addFeature("Tweet-Check x out string", TextFeatures.checkOutFeature(text));
        tweetVector.addFeature("Tweet-Other users mentioned?", TextFeatures.containsMention(text));
        tweetVector.addFeature("Tweet-contains at character", TextFeatures.containsAt(text));
        tweetVector.addFeature("Tweet-The word 'deal'", TextFeatures.containsDeal(text));
        tweetVector.addFeature("Tweet-The word 'link'", TextFeatures.containsLink(text));
        tweetVector.addFeature("Tweet-URL links", TextFeatures.containsURL(text));
    }

    /*
        Obtain all features for the life event vs. not life event classifier
     */
    public static void collectFeaturesEventVsNotEvent(TweetVector tweetVector, CoreLabel[][] phrases) throws IOException {
        String text = tweetVector.getTweetText();

        //get idfs from the text
        if (idfUpdateCounter == 0) {
            UnigramModel.updateIDFsFromTweetText(tweetVectors);
            idfUpdateCounter++;
        }
        tweetVector.addFeatures(UnigramModel.getFeaturesTFIDFNoStopWords(phrases));
    }

    /*
        Obtain all features for the self vs. other classifier
     */
    public static void collectFeaturesSelfVsOther (TweetVector tweetVector, CoreLabel[][] phrases) {
        //the number of words/strings in each of the given word classes
        tweetVector.addFeature("Word classes-Infection",
                AnnotationFeatures.getFeatureForWordClass(phrases, AnnotationFeatures.infectionWordClassName));
        tweetVector.addFeature("Word classes-Possession",
                AnnotationFeatures.getFeatureForWordClass(phrases, AnnotationFeatures.possessionWordClassName));
        tweetVector.addFeature("Word classes-Concern",
                AnnotationFeatures.getFeatureForWordClass(phrases, AnnotationFeatures.concernWordClassName));
        tweetVector.addFeature("Word classes-Vaccination",
                AnnotationFeatures.getFeatureForWordClass(phrases, AnnotationFeatures.vaccinationWordClassName));
        tweetVector.addFeature("Word classes-Past Tense",
                AnnotationFeatures.getFeatureForWordClass(phrases, AnnotationFeatures.pastTenseWordClassName));
        tweetVector.addFeature("Word classes-Present Tense",
                AnnotationFeatures.getFeatureForWordClass(phrases, AnnotationFeatures.presentTenseWordClassName));
        tweetVector.addFeature("Word classes-Self",
                AnnotationFeatures.getFeatureForWordClass(phrases, AnnotationFeatures.selfWordClassName));
        tweetVector.addFeature("Word classes-Others",
                AnnotationFeatures.getFeatureForWordClass(phrases, AnnotationFeatures.othersWordClassName));

        //non-word class features over the tweet
        String tweet = tweetVector.getTweetText();
        tweetVector.addFeature("Contains URL", TextFeatures.containsURL(tweet));
        tweetVector.addFeature("Mentions of other users", TextFeatures.containsMention(tweet));
        tweetVector.addFeature("Phrases beginning with verb", AnnotationFeatures.phrasesBeginningWithVerb(phrases));
        tweetVector.addFeature("Phrases beginning with past tense verb", AnnotationFeatures.phrasesBeginningWithPastTenseVerb(phrases));

        //features whose name is defined by the phrase it's in
        for (CoreLabel[] phrase: phrases) {
            collectPhraseDefinedFeatures(tweetVector, phrase);
            //for (int i = 0; i < featuresForPhrase.size(); i++) tweetVector.addFeature(featuresForPhrase.get(i));
        }

        //other features

    }

    /*
        Obtain all features for an individual phrase
     */
    public static void collectPhraseDefinedFeatures(TweetVector tweet, CoreLabel[] phrase) {
        //get features based on part-of-speech templates
        String[] POSTemplates = getPOSTemplates(phrase);


    }

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

    /*
    public static String toBinaryLabels(String input, String classifierType) {
        switch (classifierType) {
            case "HumanVsNonHuman":
                if (input.equals("person")) return "0";
                if (input.equals("organization")) return "1";
            case "SelfVsOther":
                if (input.equals("self")) return "1";
                if (input.equals("other")) return "0";
        }
        return input;
    }
    */

}

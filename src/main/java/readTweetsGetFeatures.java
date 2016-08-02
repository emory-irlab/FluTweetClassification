import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.SystemUtils;

import java.util.*;
import java.io.IOException;

/*
    Methods useful for creating a vector model from each tweet in an input set, representing all of the features
    used in Lamb, Paul, and Dredze 2013
 */
public class readTweetsGetFeatures {
    private static TweetVector[] tweetVectors;
    private static int idfUpdateCounter = 0;
    private static NGramModel tweetTextUnigramModel = null;
    private static NGramModel tweetTextBigramModel = null;
    private static NGramModel tweetTextTrigramModel = null;
    private static TopicFeatureModel topicFeatureModel = null;

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

    public static String process(String input) {
        input = TextFeatures.removeRetweets(input);
        input = TextFeatures.removeHashtagCharInHashtags(input);
        input = TextFeatures.removeAtCharInMentions(input);
        input = TextFeatures.removeURL(input);
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
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //get tweet vector model
        TweetVector[] tweetVectors = new TweetVector[tweets.size()];
        readTweetsGetFeatures.tweetVectors = tweetVectors;

        //initialize fields
        //String label = toBinaryLabels(tweet[5], classifierType);
        for (int i = 0; i < tweets.size(); i++) {
            //String label = toBinaryLabels(tweet[5], classifierType);
            String[] tweet = tweets.get(i);
            tweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5], labelSet);
        }

        //get features
        for (int i = 0; i < tweets.size(); i++) {
            long tweetBeginTime = System.currentTimeMillis();
            getVectorModelForTweet(tweetVectors[i], pipeline, classifierType);
            System.out.println( " total time to get tweet number "+i+" : "+(((double)System.currentTimeMillis()) - tweetBeginTime )/1000 );
        }
        return tweetVectors;
    }

    /*
        Generate the vector model of a single tweet. Pre-process, annotate, represent the tweet in terms of phrases,
        then collect phrases
     */
    public static void getVectorModelForTweet(TweetVector tweetVector, StanfordCoreNLP pipeline, String classifierType) throws IOException {
        //annotate fields with Stanford CoreNLP
        String processedTweet = process(tweetVector.getTweetText());

        //description
        Annotation descriptionDocument = new Annotation(tweetVector.getDescription());
        pipeline.annotate(descriptionDocument);
        CoreLabel[][] descriptionPhrases = getPhrases(descriptionDocument);

        //tweet
        Annotation tweetDocument = new Annotation(processedTweet);
        pipeline.annotate(tweetDocument);
        CoreLabel[][] tweetPhrases = getPhrases(tweetDocument);
        List<CoreMap> tweetSentences = tweetDocument.get(SentencesAnnotation.class);

        //collect features
        switch (classifierType) {
            case "HumanVsNonHuman":
                collectFeaturesHumanVsNonHuman(tweetVector, descriptionPhrases, tweetPhrases);
                break;
            case "EventVsNonEvent":
                collectFeaturesEventVsNotEvent(tweetVector, tweetPhrases, tweetSentences);
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
    public static void collectFeaturesHumanVsNonHuman(TweetVector tweetVector, CoreLabel[][] descriptionPhrases, CoreLabel[][] tweetPhrases) throws IOException {

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
        tweetVector.addFeature("Description-Word classes-Org. account descriptions", AnnotationFeatures.getFeatureForWordClass(descriptionPhrases,
                AnnotationFeatures.orgAccountDescriptionsWordClassName));
        tweetVector.addFeature("Description-Check x out string", TextFeatures.checkOutFeature(description));
        tweetVector.addFeature("Description-Mentions social media", TextFeatures.mentionsSocialMedia(description));
        tweetVector.addFeature("Description-Word classes-Self", AnnotationFeatures.getFeatureForWordClass(descriptionPhrases,
                AnnotationFeatures.selfWordClassName));
        tweetVector.addFeature("Description-Word classes-Plural 1P pronouns", AnnotationFeatures.getFeatureForWordClass(descriptionPhrases,
                AnnotationFeatures.plural1PPronounsWordClassName));
        tweetVector.addFeature("Description-Word classes-2P pronouns", AnnotationFeatures.getFeatureForWordClass(descriptionPhrases,
                AnnotationFeatures._2PPronounsWordClassName));
        tweetVector.addFeature("Description-Word classes-Person punctuation", AnnotationFeatures.getFeatureForWordClass(descriptionPhrases,
                AnnotationFeatures.personPunctuationWordClassName));
        tweetVector.addFeature("Description-Verb count", AnnotationFeatures.verbsCount(descriptionPhrases));

        //features based on the tweet
        String text = tweetVector.getTweetText();
        tweetVector.addFeature("Tweet-Word classes-Self", AnnotationFeatures.getFeatureForWordClass(tweetPhrases,
                AnnotationFeatures.selfWordClassName));
        tweetVector.addFeature("Tweet-Word classes-Others", AnnotationFeatures.getFeatureForWordClass(tweetPhrases,
                AnnotationFeatures.othersWordClassName));
        //including plural 1p pronouns may decrease accuracy somewhat
        tweetVector.addFeature("Tweet-Word classes-Plural 1P pronouns", AnnotationFeatures.getFeatureForWordClass(tweetPhrases,
                AnnotationFeatures.plural1PPronounsWordClassName));
        tweetVector.addFeature("Tweet-Phrases ending in exclamations", TextFeatures.countExclamationPhrases(text));
        tweetVector.addFeature("Tweet-Multiple exclamations, multiple question marks", TextFeatures.containsMultipleExclamationsQuestions(text));
        tweetVector.addFeature("Tweet-Check x out string", TextFeatures.checkOutFeature(text));
        tweetVector.addFeature("Tweet-Other users mentioned?", TextFeatures.containsMention(text));
        tweetVector.addFeature("Tweet-The word 'deal'", TextFeatures.containsDeal(text));
        tweetVector.addFeature("Tweet-The word 'link'", TextFeatures.containsLink(text));
        tweetVector.addFeature("Tweet-URL links", TextFeatures.containsURL(text));
    }

    /*
        Obtain all features for the life event vs. not life event classifier
     */
    public static void collectFeaturesEventVsNotEvent(TweetVector tweetVector, CoreLabel[][] phrases, List<CoreMap> tweetSentences) throws IOException {
        String text = process(tweetVector.getTweetText());
/*
        //unigram features (tf-idf value of each word)
        if (tweetTextUnigramModel == null) {
            tweetTextUnigramModel = new NGramModel(1, tweetVectors, NGramModel.textName, "data/stopwords.txt", 1);
        }
        //tweetVector.addFeatures(tweetTextUnigramModel.getFeaturesForTweetTFIDF(phrases));
        //tf-only test
        tweetVector.addFeatures(tweetTextUnigramModel.getFeaturesForTweetTF(phrases));

        //bigram features (tf-idf value of each word); bigrams must appear at least thrice to be considered
        if (tweetTextBigramModel == null) {
            tweetTextBigramModel = new NGramModel(2, tweetVectors, NGramModel.textName, "", 5);
        }
        //tweetVector.addFeatures(tweetTextBigramModel.getFeaturesForTweetTFIDF(phrases));
        //tf-only test
        tweetVector.addFeatures(tweetTextBigramModel.getFeaturesForTweetTF(phrases)); 

        //trigram features (tf-idf); trigrams must appear at least 3 times across the dataset to be considered
        if (tweetTextTrigramModel == null) {
            tweetTextTrigramModel = new NGramModel(3, tweetVectors, NGramModel.textName, "", 10);
        }
        //tweetVector.addFeatures(tweetTextTrigramModel.getFeaturesForTweetTFIDF(phrases));
	    tweetVector.addFeatures(tweetTextTrigramModel.getFeaturesForTweetTF(phrases));

        //phrase templates
        ArrayList<String> phraseTemplates = AnnotationFeatures.getPhraseTemplates(tweetSentences);
        for (String template: phraseTemplates) {
            tweetVector.addFeature(template, 1.0);
        }
*/
        //topics for tweet
        if (topicFeatureModel == null) {
            topicFeatureModel = new TopicFeatureModel("data/topics/countFile.txt", "data/topics/tweet_composition.txt", "data/stopwords.txt");
        }
        int[] topTopics = topicFeatureModel.getNMostLikelyTopics(3, text);
        for (int topTopic: topTopics) {
            tweetVector.addFeature(Integer.toString(topTopic), 1.0);
        }



        //other features
        //addition 1
        tweetVector.addFeature("Hashtag Count", TextFeatures.countInstancesOf(text, TextFeatures.hashtagPattern));
        tweetVector.addFeature("User Mention Count", TextFeatures.countInstancesOf(text, TextFeatures.userMentionPattern));
        tweetVector.addFeature("URL Count", TextFeatures.countInstancesOf(text, TextFeatures.detectURL));

        //Sentiment analysis
        /*
        tweetVector.addFeature("Positive Adverbs", TextFeatures.countWordsInDict(text, TextFeatures.positiveAdverbs));
        tweetVector.addFeature("Negative Adverbs", TextFeatures.countWordsInDict(text, TextFeatures.negativeAdverbs));
        tweetVector.addFeature("Neutral Adverbs", TextFeatures.countWordsInDict(text, TextFeatures.neutralAdverbs));
        tweetVector.addFeature("Positive Emoticons", TextFeatures.countWordsInDict(text, TextFeatures.positiveEmoticons));
        tweetVector.addFeature("Negative Emoticons", TextFeatures.countWordsInDict(text, TextFeatures.negativeEmoticons));
        tweetVector.addFeature("Mentions Family", TextFeatures.mentionsFamily(text));
        */

        //Stylometry
//        tweetVector.addFeature("Contains Mentions", TextFeatures.containsAt(text));
//        tweetVector.addFeature("Contains URL", TextFeatures.containsURL(text));
//        tweetVector.addFeature("Count Uppercase Words", TextFeatures.countUpperCaseWords(text));
//        tweetVector.addFeature("Count Exclamations", TextFeatures.countExclamationPhrases(text));
//        tweetVector.addFeature("Count Question Marks", TextFeatures.countQuestionMarkGroups(text));
    }

    /*
        Obtain all features for the self vs. other classifier
     */
    public static void collectFeaturesSelfVsOther (TweetVector tweetVector, CoreLabel[][] phrases) throws IOException {
        //the number of words/strings in each of the given word classes
        tweetVector.addFeature("Word classes-Infection", AnnotationFeatures.getFeatureForWordClass(phrases,
                AnnotationFeatures.infectionWordClassName));
        tweetVector.addFeature("Word classes-Possession", AnnotationFeatures.getFeatureForWordClass(phrases,
                AnnotationFeatures.possessionWordClassName));
        tweetVector.addFeature("Word classes-Concern", AnnotationFeatures.getFeatureForWordClass(phrases,
                AnnotationFeatures.concernWordClassName));
        tweetVector.addFeature("Word classes-Vaccination", AnnotationFeatures.getFeatureForWordClass(phrases,
                AnnotationFeatures.vaccinationWordClassName));
        tweetVector.addFeature("Word classes-Past Tense", AnnotationFeatures.getFeatureForWordClass(phrases,
                AnnotationFeatures.pastTenseWordClassName));
        tweetVector.addFeature("Word classes-Present Tense", AnnotationFeatures.getFeatureForWordClass(phrases,
                AnnotationFeatures.presentTenseWordClassName));
        tweetVector.addFeature("Word classes-Self", AnnotationFeatures.getFeatureForWordClass(phrases,
                AnnotationFeatures.selfWordClassName));
        tweetVector.addFeature("Word classes-Others", AnnotationFeatures.getFeatureForWordClass(phrases,
                AnnotationFeatures.othersWordClassName));

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

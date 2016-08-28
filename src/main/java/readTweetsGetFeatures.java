import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.parser.common.NoSuchParseException;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.io.IOException;
import java.io.File;

/*
    Methods useful for creating a vector model from each tweet in an input set, representing all of the features
    used in Lamb, Paul, and Dredze 2013
 */
public class readTweetsGetFeatures {

    //classifier names
    static final String humanNonHumanClassifierName = "humanNonHuman";
    static final String eventClassifierName = "event";
    static final String selfOtherClassifierName = "selfOther";

    private static String dataSource = "";
    private static int idfUpdateCounter = 0;
    private static NGramModel tweetTextUnigramModelEvent = null;
    private static NGramModel tweetTextBigramModelEvent = null;
    private static NGramModel tweetTextTrigramModelEvent = null;

    private static NGramModel tweetTextUnigramModelSvO = null;
    private static NGramModel tweetTextBigramModelSvO = null;
    private static NGramModel tweetTextTrigramModelSvO = null;

    private static TopicFeatureModel topicFeatureModel = null;
    private static StanfordCoreNLP pipeline = null;

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
    public static TweetVector[] getVectorModelsFromTweets(String pathToTweetFile, String classifierType, int nCores) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        ArrayList<String> labelSet = new ArrayList<String>(0);
        //set up Stanford CoreNLP object for annotation, if it hasn't been set up yet
        if (pipeline == null) {
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize, ssplit, pos, lemma, depparse, natlog, openie");
            pipeline = new StanfordCoreNLP(props);
        }

        //get tweets, initialize tweet vectors
        ArrayList<String[]> tweets = TweetParser.getTweets(pathToTweetFile);
        TweetVector[] tweetVectors = new TweetVector[tweets.size()];
        readTweetsGetFeatures.dataSource = pathToTweetFile;
        for (int i = 0; i < tweets.size(); i++) {
            String[] tweet = tweets.get(i);
            tweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5], labelSet);
        }

        //get features for each tweet vector
        for (int i = 0; i < tweets.size(); i++) {
            long tweetBeginTime = System.currentTimeMillis();
            getVectorModelForTweet(tweetVectors[i], classifierType, nCores);
            System.out.println( " total time to get tweet number "+i+" : "+(((double)System.currentTimeMillis()) - tweetBeginTime )/1000 );
        }

        System.out.println("Total time to get "+tweetVectors.length+" tweets: "+(((double)System.currentTimeMillis()) - startTime )/1000+" seconds.");
        return tweetVectors;
    }

    /*
        Generate the vector model of a single tweet. Pre-process, annotate, represent the tweet in terms of phrases,
        then collect phrases
     */
    public static TweetVector getVectorModelForTweet(TweetVector tweetVector, String classifierType, int nCores) throws IOException, InterruptedException {
        //create the Stanford CoreNLP pipeline if it doesn't exist yet
        if (pipeline == null) {
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize, ssplit, pos, lemma, depparse, natlog, openie");
            pipeline = new StanfordCoreNLP(props);
        }

        //annotate fields with Stanford CoreNLP
        String processedTweet = process(tweetVector.getTweetText());

        //description
        Annotation descriptionDocument = new Annotation(tweetVector.getDescription());
        try {
            pipeline.annotate(descriptionDocument);
        }
        catch (NoSuchElementException e) {
            System.out.println(tweetVector.getDescription()+" triggered a NoSuchElementException");
            e.printStackTrace();
        }
        CoreLabel[][] descriptionPhrases = getPhrases(descriptionDocument);

        //tweet
        Annotation tweetDocument = new Annotation(processedTweet);
        try {
            pipeline.annotate(tweetDocument);
        }
        catch (NoSuchElementException e) {
            System.out.println(processedTweet+" triggered a NoSuchElementException");
            e.printStackTrace();
        }
        CoreLabel[][] tweetPhrases = getPhrases(tweetDocument);
        List<CoreMap> tweetSentences = tweetDocument.get(SentencesAnnotation.class);

        //collect features
        switch (classifierType) {
            case humanNonHumanClassifierName:
                collectFeaturesHumanVsNonHuman(tweetVector, descriptionPhrases, tweetPhrases);
                break;
            case eventClassifierName:
                collectFeaturesEventVsNotEvent(tweetVector, tweetPhrases, tweetSentences, nCores);
                break;
            case selfOtherClassifierName:
                collectFeaturesSelfVsOther(tweetVector, tweetPhrases, tweetSentences);
                break;
        }
        return tweetVector;
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
    private static void collectFeaturesHumanVsNonHuman(TweetVector tweetVector, CoreLabel[][] descriptionPhrases, CoreLabel[][] tweetPhrases) throws IOException {

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
    private static void collectFeaturesEventVsNotEvent(TweetVector tweetVector, CoreLabel[][] phrases, List<CoreMap> tweetSentences, int nCores) throws IOException, InterruptedException {
        String text = process(tweetVector.getTweetText());

        //unigram features (tf-idf value of each word)
        if (tweetTextUnigramModelEvent == null) {
            tweetTextUnigramModelEvent = new NGramModel(1, dataSource, NGramModel.textName, eventClassifierName, "data/stopwords.txt", 1);
        }
        //tf-only test
        tweetVector.addFeatures(tweetTextUnigramModelEvent.getFeaturesForTweetTF(phrases));

        //bigram features (tf-idf value of each word); bigrams must appear at least thrice to be considered
        if (tweetTextBigramModelEvent == null) {
            tweetTextBigramModelEvent = new NGramModel(2, dataSource, NGramModel.textName, eventClassifierName, "data/stopwords.txt", 7);
        }
        //tf-only test
        tweetVector.addFeatures(tweetTextBigramModelEvent.getFeaturesForTweetTF(phrases));

        //trigram features (tf-idf); trigrams must appear at least 3 times across the dataset to be considered
        if (tweetTextTrigramModelEvent == null) {
            tweetTextTrigramModelEvent = new NGramModel(3, dataSource, NGramModel.textName, eventClassifierName, "data/stopwords.txt", 10);
        }
	    tweetVector.addFeatures(tweetTextTrigramModelEvent.getFeaturesForTweetTF(phrases));

        //phrase templates
        ArrayList<String> phraseTemplates = AnnotationFeatures.getPhraseTemplates(tweetSentences);
        for (String template: phraseTemplates) {
            tweetVector.addFeature(template, 1.0);
        }

        //topics for tweet
        if (topicFeatureModel == null) {
            topicFeatureModel = new TopicFeatureModel("data/topics/countFileMinusOnes.txt", "data/topics/tweet_composition.txt", "data/stopwords.txt", nCores);
        }
        int[] topTopics = topicFeatureModel.getNMostLikelyTopics(3, text);
        for (int topTopic: topTopics) {
            tweetVector.addFeature(Integer.toString(topTopic), 1.0);
        }

        //other features
        //addition 1
        //tweetVector.addFeature("Hashtag Count", TextFeatures.countInstancesOf(text, TextFeatures.hashtagPattern));
        //tweetVector.addFeature("User Mention Count", TextFeatures.countInstancesOf(text, TextFeatures.userMentionPattern));
        //tweetVector.addFeature("URL Count", TextFeatures.countInstancesOf(text, TextFeatures.detectURL));

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

    public static void collectFeaturesEventVsNotEventParallel(TweetVector tweetVector, CoreLabel[][] phrases) throws IOException {

    }

    /*
        Obtain all features for the self vs. other classifier
     */
    private static void collectFeaturesSelfVsOther (TweetVector tweetVector, CoreLabel[][] phrases, List<CoreMap> tweetSentences) throws IOException {
        //the number of words/strings in each of the given word classes
        tweetVector.addFeature("Word classes-Past Tense", AnnotationFeatures.getFeatureForWordClass(phrases, "Past Tense"));
        tweetVector.addFeature("Word classes-Present Tense", AnnotationFeatures.getFeatureForWordClass(phrases, "Present Tense"));
        tweetVector.addFeature("Word classes-Self", AnnotationFeatures.getFeatureForWordClass(phrases, "Self"));
        tweetVector.addFeature("Word classes-Others", AnnotationFeatures.getFeatureForWordClass(phrases, "Others"));
        tweetVector.addFeature("Count Singular Plural Nouns", AnnotationFeatures.countSingularProperNouns(phrases));
        tweetVector.addFeature("Count Plural Nouns", AnnotationFeatures.countPluralProperNouns(phrases));
        tweetVector.addFeature("Numeric References Count", AnnotationFeatures.numericalReferencesCount(phrases));

        String text = tweetVector.getTweetText();

        //Stylometry
        tweetVector.addFeature("Contains Mentions", TextFeatures.containsAt(text));
        tweetVector.addFeature("Contains URL", TextFeatures.containsURL(text));
        tweetVector.addFeature("Count Uppercase Words", TextFeatures.countUpperCaseWords(text));
        tweetVector.addFeature("Count Question Marks", TextFeatures.countQuestionMarkGroups(text));
        tweetVector.addFeature("Other users mentioned?", TextFeatures.containsMention(text));
        tweetVector.addFeature("Hashtag Count", TextFeatures.countHashtags(text));

        //non-word class features over the tweet
        tweetVector.addFeature("Phrases beginning with verb", AnnotationFeatures.phrasesBeginningWithVerb(phrases));
        tweetVector.addFeature("Phrases beginning with past tense verb", AnnotationFeatures.phrasesBeginningWithPastTenseVerb(phrases));
        tweetVector.addFeature("Count Verbs Following Proper Nouns", AnnotationFeatures.properNounsFollowedByVerb(phrases));

//        //OpenIE features
//        int openIESelf = 0;
//        int openIEOther = 0;

        // Loop over sentences in the document
        for (CoreMap sentence : tweetSentences) {
          // Get the OpenIE triples for the sentence
          Collection<RelationTriple> triples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
          // Print the triples
          for (RelationTriple triple : triples) {

        	  String[] noConfidenceTrip = triple.toString().split("\\s");

        	  String[] subVerbObj =(noConfidenceTrip[1] + " " + noConfidenceTrip[2] + " " + noConfidenceTrip[3]).split("\\s");
        	  tweetVector.addFeature("openIE Self Count - sVO", AnnotationFeatures.countWordsInClassSelf(subVerbObj));
        	  tweetVector.addFeature("openIE Other Count - sVO", AnnotationFeatures.countWordsInClassOther(subVerbObj));

        	  String[] subVerb = (noConfidenceTrip[1] + " " + noConfidenceTrip[2]).split("\\s");
        	  tweetVector.addFeature("openIE Self Count - subVerb", AnnotationFeatures.countWordsInClassSelf(subVerb));
        	  tweetVector.addFeature("openIE Other Count - subVerb", AnnotationFeatures.countWordsInClassOther(subVerb));

        	  String[] subObj = (noConfidenceTrip[1] + " " + noConfidenceTrip[3]).split("\\s");
        	  tweetVector.addFeature("openIE Self Count - subObj", AnnotationFeatures.countWordsInClassSelf(subObj));
        	  tweetVector.addFeature("openIE Other Count - subObj", AnnotationFeatures.countWordsInClassOther(subObj));

        	  String[] verbObj = (noConfidenceTrip[2] + " " + noConfidenceTrip[2]).split("\\s");
        	  tweetVector.addFeature("openIE Self Count - verbObj", AnnotationFeatures.countWordsInClassSelf(verbObj));
        	  tweetVector.addFeature("openIE Other Count - subObj", AnnotationFeatures.countWordsInClassOther(verbObj));

        	  //Subject-Verb-Object tuple
        	  tweetVector.addFeature(noConfidenceTrip[1] + " " + noConfidenceTrip[2] + " " + noConfidenceTrip[3], 1);
        	  //Subject-verb tuple
        	  tweetVector.addFeature(noConfidenceTrip[1] + " " + noConfidenceTrip[2], 1);
        	  //Subject-Object tuple
        	  tweetVector.addFeature(noConfidenceTrip[1] + " " + noConfidenceTrip[3], 1);
        	  //Verb-Object Tuple
        	  tweetVector.addFeature(noConfidenceTrip[2] + " " + noConfidenceTrip[3], 1);

//        	  for (CoreLabel l : triple) {
//
//        		  if (AnnotationFeatures.isInWordClassSelf(l.word())) {
//        			  openIESelf++;
//        		  }
//        		  else if(AnnotationFeatures.isInWordClassOther(l.word())) {
//        			  openIEOther++;
//        		  }
//        	  }
           }
        }

//	    tweetVector.addFeature("openIE Self Count", openIESelf);
//	    tweetVector.addFeature("openIE Other Count", openIEOther);


        //Dredze POS templates
        String[] firstPronounLastNoun = AnnotationFeatures.pairFirstPronounLastNoun(phrases);
        //First pronoun, last noun pair
        if (firstPronounLastNoun[0] != null && firstPronounLastNoun[1] != null) {
        	tweetVector.addFeature("(" + firstPronounLastNoun[0] + "," + " " + firstPronounLastNoun[1] + ")", 1);
            tweetVector.addFeature("fProLNoun in Self", AnnotationFeatures.countWordsInClassSelf(firstPronounLastNoun));
            tweetVector.addFeature("fProLNoun in Other", AnnotationFeatures.countWordsInClassOther(firstPronounLastNoun));
        }

        String[] firstNounPronounLastVerb = AnnotationFeatures.pairFirstPronounOrNounLastVerb(phrases);
        //First noun or pronoun (not counting proper nouns) last verb pair
        if (firstNounPronounLastVerb[0] != null && firstNounPronounLastVerb[1] != null) {
        	tweetVector.addFeature("(" + firstNounPronounLastVerb[0] + "," + " " + firstNounPronounLastVerb[1] + ")", 1);
            tweetVector.addFeature("fPro|NounVerb in Self", AnnotationFeatures.countWordsInClassSelf(firstNounPronounLastVerb));
            tweetVector.addFeature("fPro|NounVerb in Other", AnnotationFeatures.countWordsInClassOther(firstNounPronounLastVerb));
        }

        //unigrams
        if (tweetTextUnigramModelSvO == null) {
            tweetTextUnigramModelSvO = new NGramModel(1, dataSource, NGramModel.textName, selfOtherClassifierName, "data/stopwords.txt", 1);
        }
        tweetVector.addFeatures(tweetTextUnigramModelSvO.getFeaturesForTweetTF(phrases));

        if (tweetTextBigramModelSvO == null) {
            tweetTextBigramModelSvO = new NGramModel(2, dataSource, NGramModel.textName, selfOtherClassifierName, "data/stopwords.txt", 1);
        }
        tweetVector.addFeatures(tweetTextBigramModelSvO.getFeaturesForTweetTF(phrases));
    }

    /*
        Obtain all features for an individual phrase
     */
    public static void collectPhraseDefinedFeatures(TweetVector tweet, CoreLabel[] phrase) {
        //get features based on part-of-speech templates
        String[] POSTemplates = getPOSTemplates(phrase);


    }

    public static int getSentimentScore(Annotation tweetDocument) {

        int mainSentiment = 0;
        int longest = 0;

        for (CoreMap sentence : tweetDocument.get(CoreAnnotations.SentencesAnnotation.class)) {
            Tree tree = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
            int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
            String partText = sentence.toString();
            if (partText.length() > longest) {
                mainSentiment = sentiment;
                longest = partText.length();
            }
        }
        return mainSentiment;
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

}

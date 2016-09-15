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
import java.lang.reflect.Array;
import java.util.*;
import java.io.IOException;
import java.io.File;

/*
    Methods useful for creating a vector model from each tweet in an input set, representing all of the features
    used in Lamb, Paul, and Dredze 2013
 */
public class TweetFeatureExtractor {

    /*
        Sets up a thread that extracts features from tweets
     */
    public static class FeatureExtractionThread implements Runnable {
        public Thread thread;
        private String threadName;

        private String classifierType;
        private TweetFeatureExtractor tweetFeatureExtractor;
        private ArrayList<String[]> tweets;
        private TweetVector[] tweetVectors = null;

       //constructors
        FeatureExtractionThread(TweetFeatureExtractor extractor, String pathToTweets, String type, String name)
                throws IOException {
            tweetFeatureExtractor = extractor;
            tweets = TweetParser.getTweets(pathToTweets);
            threadName = name;
            classifierType = type;
        }
        FeatureExtractionThread(TweetFeatureExtractor extractor, ArrayList<String[]> twts, String type, String name)
            throws IOException {
            tweetFeatureExtractor = extractor;
            tweets = twts;
            threadName = name;
            classifierType = type;
        }

        public TweetVector[] getTweetVectors() {
            return tweetVectors;
        }

        public void run() {
            try {
                tweetVectors = tweetFeatureExtractor.getVectorModelsFromTweets(tweets, classifierType);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Thread  "+threadName+"  exiting");
        }

        public void start() {
            if (thread == null) {
                thread = new Thread(this, threadName);
                thread.start();
            }
        }

    }

   //static copies of fields
    //text field names

    public static final String textName = "text";
    public static final String descriptionName = "description";
    //classifier names
    static final String humanNonHumanClassifierName = "humanNonHuman";
    static final String eventClassifierName = "event";
    static final String selfOtherClassifierName = "selfOther";
    //text model names
    static final String processedTextModelName = "proc";
    static final String processedTextModelNameWHash = "procWHash";
    static final String originalTextModelName = "orig";

    //training data to initialize the n-gram models of each classifier
    private String pathToTrainingTweetsEvent;
    private String pathToTrainingTweetsSelfOther;

    private int nCores;
    private NGramModel tweetTextUnigramModelEvent = null;
    private NGramModel tweetTextBigramModelEvent = null;
    private NGramModel tweetTextTrigramModelEvent = null;

    private NGramModel tweetTextUnigramModelSvO = null;
    private NGramModel tweetTextBigramModelSvO = null;
    private NGramModel tweetTextTrigramModelSvO = null;

    private TopicFeatureModel topicFeatureModel = null;
    private StanfordCoreNLP pipeline = null;

    public TweetFeatureExtractor(String pathEvent, String pathSelfOther, int n) throws IOException, InterruptedException {
        pathToTrainingTweetsEvent = pathEvent;
        pathToTrainingTweetsSelfOther = pathSelfOther;
        nCores = n;

        //initialize the pipeline
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, depparse, natlog, openie");
        pipeline = new StanfordCoreNLP(props);

        //initialize the n-gram models
         //event classifier
        long startTime = System.currentTimeMillis();
        String nameOfTextModelForEventNGrams = processedTextModelNameWHash;
        //tweetTextUnigramModelEvent = new NGramModel(1, pathToTrainingTweetsEvent, textName, nameOfTextModelForEventNGrams, eventClassifierName, "data/stopwordsEventWordsAllowed.txt", 80000, false, nCores);
        tweetTextBigramModelEvent = new NGramModel(2, pathToTrainingTweetsEvent, textName, nameOfTextModelForEventNGrams, eventClassifierName, "data/stopwordsEventWordsAllowed.txt", 62000, false, nCores);
        tweetTextTrigramModelEvent = new NGramModel(3, pathToTrainingTweetsEvent, textName, nameOfTextModelForEventNGrams, eventClassifierName, "data/stopwordsEventWordsAllowed.txt", 21000, false, nCores);
         //self vs other classifier
        String nameOfTextModelForSvONGrams = processedTextModelName;
        tweetTextUnigramModelSvO = new NGramModel(1, pathToTrainingTweetsSelfOther, textName, nameOfTextModelForSvONGrams, selfOtherClassifierName, "data/stopwordsPronounsAllowed.txt", true, nCores);
        tweetTextBigramModelSvO = new NGramModel(2, pathToTrainingTweetsSelfOther, textName, nameOfTextModelForSvONGrams, selfOtherClassifierName, "data/stopwordsPronounsAllowed.txt", true, nCores);
        System.out.println("Time to initialize n-gram models: "+((double)(System.currentTimeMillis() - startTime))/1000+" seconds");

        //initialize the topic feature model
        topicFeatureModel = new TopicFeatureModel("data/topics/countFileMinusOnes.txt", "data/topics/tweet_composition.txt", "data/stopwords.txt", nCores);
    }

    public String process(String input, String modelName) {
        if (modelName.equals(processedTextModelName)) {
            return processProc(input);
        }
        else if (modelName.equals(processedTextModelNameWHash)) {
            return processProcWHash(input);
        }
        else if (modelName.equals(originalTextModelName)) {
            return input;
        }
        else {
            return null; //change into an exception?
        }
    }

    public static String processStatic(String input, String modelName) {
        if (modelName.equals(processedTextModelName)) {
            return processProc(input);
        }
        else if (modelName.equals(processedTextModelNameWHash)) {
            return processProcWHash(input);
        }
        else if (modelName.equals(originalTextModelName)) {
            return input;
        }
        else {
            return null; //change into an exception?
        }
    }

    private static String processProc(String input) {
        input = TextFeatures.removeRetweets(input);
        input = TextFeatures.removeHashtagCharInHashtags(input);
        input = TextFeatures.removeAtCharInMentions(input);
        input = TextFeatures.removeURL(input);
        input = input.replaceAll(TextFeatures.spaceGroup, " ");
        input = TextFeatures.removeCharsRepeated3PlusTimes(input);
        return input;
    }

    private static String processProcWHash(String input) {
        input = TextFeatures.removeRetweets(input);
        input = TextFeatures.removeAtCharInMentions(input);
        input = TextFeatures.removeURL(input);
        input = input.replaceAll(TextFeatures.spaceGroup, " ");
        input = TextFeatures.removeCharsRepeated3PlusTimes(input);
        return input;
    }

    /*
        From a collection of tweets, create a vector model for each
        tweet using the features for the relevant type of classifier
        Each input tweet is formatted as follows:
        {profile pic, username, name, description, tweet, label}
    */
    public TweetVector[] getVectorModelsFromTweets(ArrayList<String[]> tweets, String classifierType) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();

        TweetVector[] tweetVectors = new TweetVector[tweets.size()];
        for (int i = 0; i < tweets.size(); i++) {
            String[] tweet = tweets.get(i);
            tweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5]);
        }

        //get features for each tweet vector
        for (int i = 0; i < tweets.size(); i++) {
            long tweetBeginTime = System.currentTimeMillis();
            getVectorModelForTweet(tweetVectors[i], classifierType);
            System.out.println( " total time to get tweet number "+i+" : "+(((double)System.currentTimeMillis()) - tweetBeginTime )/1000 );
        }

        System.out.println("Total time to get "+tweetVectors.length+" tweets: "+(((double)System.currentTimeMillis()) - startTime )/1000+" seconds.");
        return tweetVectors;
    }

    /*
        From a path to a tweet file, create a vector model for each
        tweet using the features for the relevant type of classifier
        Each input tweet is formatted as follows:
        {profile pic, username, name, description, tweet, label}
     */
    public TweetVector[] getVectorModelsFromTweets(String pathToTweets, String classifierType) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();

        //get tweets, initialize tweet vectors
        ArrayList<String[]> tweets = TweetParser.getTweets(pathToTweets);
        TweetVector[] tweetVectors = new TweetVector[tweets.size()];
        for (int i = 0; i < tweets.size(); i++) {
            String[] tweet = tweets.get(i);
            tweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5]);
        }

        //get features for each tweet vector
        for (int i = 0; i < tweets.size(); i++) {
            long tweetBeginTime = System.currentTimeMillis();
            getVectorModelForTweet(tweetVectors[i], classifierType);
            System.out.println( " total time to get tweet number "+i+" : "+(((double)System.currentTimeMillis()) - tweetBeginTime )/1000 );
        }

        System.out.println("Total time to get "+tweetVectors.length+" tweets: "+(((double)System.currentTimeMillis()) - startTime )/1000+" seconds.");
        return tweetVectors;
    }

    /*
    From a collection of tweets, create a vector model for each
    tweet using the features for the relevant type of classifier
    Each input tweet is formatted as follows:
    {profile pic, username, name, description, tweet, label}

    Uses nCores TweetFeatureExtractors (this and nCores - 1 additional extractors)
    and runs each extractor in a different thread to speed up processing
    */
    public TweetVector[] getVectorModelsFromTweetsMultithreaded(ArrayList<String[]> tweets, String classifierType)
            throws IOException, InterruptedException {
        ArrayList<TweetVector> output = new ArrayList<TweetVector>();

        //Divide the array of tweets it into <nCores> groups
        int unit = tweets.size() / nCores;
        int lastStart = 0;
        ArrayList<ArrayList<String[]>> tweetSegments = new ArrayList<ArrayList<String[]>>(nCores);
        for (int i = 0; i < nCores; i++) {
            ArrayList<String[]> nextSegment = (ArrayList<String[]>)tweets.subList(lastStart, lastStart+unit);
            tweetSegments.add(nextSegment);

            lastStart += unit; //advance the count
        }

        //use <nCores> tweet feature extractors, the current one and <nCores> - 1 additional ones
        ArrayList<TweetFeatureExtractor> tweetFeatureExtractors = new ArrayList<TweetFeatureExtractor>();
        //the first extractor is always this one
        tweetFeatureExtractors.add(this);
        for (int i = 1; i < nCores; i++) {
            TweetFeatureExtractor thisTweetFeatureExtractor = new TweetFeatureExtractor(pathToTrainingTweetsEvent, pathToTrainingTweetsSelfOther, nCores);
            tweetFeatureExtractors.add(thisTweetFeatureExtractor);
        }

        //create threads to vectorize each section of the training data
        ArrayList<TweetFeatureExtractor.FeatureExtractionThread> threads = new ArrayList<TweetFeatureExtractor.FeatureExtractionThread>();
        for (int i = 0; i < nCores; i++) {
            threads.add(new TweetFeatureExtractor.FeatureExtractionThread(tweetFeatureExtractors.get(i), tweetSegments.get(i), classifierType, "thread"+i));
            threads.get(i).start();
        }

        //wait until each thread finishes, then collect its tweet vectors
        for (TweetFeatureExtractor.FeatureExtractionThread thread: threads) {
            thread.thread.join();

            for (TweetVector tweetVector: thread.getTweetVectors()) {
                output.add(tweetVector);
            }
        }

        return output.toArray(new TweetVector[output.size()]);
    }

    /*
    From a path to a tweet file, create a vector model for each
    tweet using the features for the relevant type of classifier
    Each input tweet is formatted as follows:
    {profile pic, username, name, description, tweet, label}

    Uses nCores TweetFeatureExtractors (this and nCores - 1 additional extractors)
    and runs each extractor in a different thread to speed up processing
    */
    public TweetVector[] getVectorModelsFromTweetsMultithreaded(String pathToTweets, String classifierType)
            throws IOException, InterruptedException {
        ArrayList<TweetVector> output = new ArrayList<TweetVector>();

        //get the array of tweets, divide it into <nCores> segments
        ArrayList<String[]> tweets = TweetParser.getTweets(pathToTweets);
        int unit = tweets.size() / nCores;
        int lastStart = 0;
        ArrayList<ArrayList<String[]>> tweetSegments = new ArrayList<ArrayList<String[]>>(nCores);
        for (int i = 0; i < nCores; i++) {

            ArrayList<String[]> nextSegment = new ArrayList<String[]>();
            for (int j = lastStart; j < lastStart + unit; j++) {
                nextSegment.add(tweets.get(j));
            }
            tweetSegments.add(nextSegment);

            lastStart += unit; //advance the count
        }

        //use <nCores> tweet feature extractors, the current one and <nCores> - 1 additional ones
        ArrayList<TweetFeatureExtractor> tweetFeatureExtractors = new ArrayList<TweetFeatureExtractor>();
        //the first extractor is always this one
        tweetFeatureExtractors.add(this);
        for (int i = 1; i < nCores; i++) {
            TweetFeatureExtractor thisTweetFeatureExtractor = new TweetFeatureExtractor(pathToTrainingTweetsEvent, pathToTrainingTweetsSelfOther, nCores);
            tweetFeatureExtractors.add(thisTweetFeatureExtractor);
        }

        //create threads to vectorize each section of the training data
        ArrayList<TweetFeatureExtractor.FeatureExtractionThread> threads = new ArrayList<TweetFeatureExtractor.FeatureExtractionThread>();
        for (int i = 0; i < nCores; i++) {
            threads.add(new TweetFeatureExtractor.FeatureExtractionThread(tweetFeatureExtractors.get(i), tweetSegments.get(i), classifierType, "thread"+i));
            threads.get(i).start();
        }

        //wait until each thread finishes, then collect its tweet vectors
        for (TweetFeatureExtractor.FeatureExtractionThread thread: threads) {
            thread.thread.join();

            for (TweetVector tweetVector: thread.getTweetVectors()) {
                output.add(tweetVector);
            }
        }

        return output.toArray(new TweetVector[output.size()]);
    }

    /*
        Generate the vector model of a single tweet. Pre-process, annotate, represent the tweet in terms of phrases,
        then collect phrases
     */
    public TweetVector getVectorModelForTweet(TweetVector tweetVector, String classifierType) throws IOException, InterruptedException {
        //get processed text models
        String processedTweet = process(tweetVector.getTweetText(), processedTextModelName);

        //get phrase models
        CoreLabel[][] processedDescriptionPhrases = getPhrases(tweetVector.getDescription(), processedTextModelName);
        CoreLabel[][] processedTweetPhrases = getPhrases(tweetVector.getTweetText(), processedTextModelName);
        CoreLabel[][] processedWHashtagsTweetPhrases = getPhrases(tweetVector.getTweetText(), processedTextModelNameWHash);

        //get sentence models
        List<CoreMap> processedTweetSentences = getSentences(tweetVector.getTweetText(), processedTextModelName);

        //collect features
        switch (classifierType) {
            case humanNonHumanClassifierName:
                collectFeaturesHumanVsNonHuman(tweetVector, processedDescriptionPhrases, processedTweetPhrases);
                break;
            case eventClassifierName:
                collectFeaturesEventVsNotEvent(tweetVector, processedTweet, processedWHashtagsTweetPhrases, processedTweetSentences);
                break;
            case selfOtherClassifierName:
                collectFeaturesSelfVsOther(tweetVector, processedTweetPhrases, processedTweetSentences);
                break;
        }
        return tweetVector;
    }

    /*
        Annotates a string of text (processed, if the model name calls for it) into Stanford CoreNLP sentences,
        returns the phrases
     */
    public List<CoreMap> getSentences(String originalInput, String modelName) {
        String processedInput;

        //process the text if the model name is not "original"
        processedInput = process(originalInput, modelName);

        //create an annotation
        Annotation document = new Annotation(processedInput);
        try {
            pipeline.annotate(document);
        }
        catch (NoSuchElementException e) {
            System.out.println(processedInput+" triggered a NoSuchElementException");
            e.printStackTrace();
        }

        return document.get(SentencesAnnotation.class);
    }

    /*
        Annotates a string of text (processed, if the model name calls for it) into phrases delimited by punctuation marks,
        returns the phrases
     */
    public CoreLabel[][] getPhrases(String originalInput, String modelName) {
        String processedInput;

        //process the text if the model name is not "original"
        processedInput = process(originalInput, modelName);

        //create an annotation
        Annotation document = new Annotation(processedInput);
        try {
            pipeline.annotate(document);
        }
        catch (NoSuchElementException e) {
            System.out.println(processedInput+" triggered a NoSuchElementException");
            e.printStackTrace();
        }

        //get phrases from the annotation
        return getPhrases(document);
    }

    /*
        Divides an Annotation into phrases delimited by punctuation marks, returns the phrases
    */
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
    private static void collectFeaturesHumanVsNonHuman(TweetVector tweetVector, CoreLabel[][] processedDescriptionPhrases, CoreLabel[][] processedTweetPhrases) throws IOException {

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
        tweetVector.addFeature("Description-Word classes-Org. account descriptions", AnnotationFeatures.getFeatureForWordClass(processedDescriptionPhrases,
                AnnotationFeatures.orgAccountDescriptionsWordClassName));
        tweetVector.addFeature("Description-Check x out string", TextFeatures.checkOutFeature(description));
        tweetVector.addFeature("Description-Mentions social media", TextFeatures.mentionsSocialMedia(description));
        tweetVector.addFeature("Description-Word classes-Self", AnnotationFeatures.getFeatureForWordClass(processedDescriptionPhrases,
                AnnotationFeatures.selfWordClassName));
        tweetVector.addFeature("Description-Word classes-Plural 1P pronouns", AnnotationFeatures.getFeatureForWordClass(processedDescriptionPhrases,
                AnnotationFeatures.plural1PPronounsWordClassName));
        tweetVector.addFeature("Description-Word classes-2P pronouns", AnnotationFeatures.getFeatureForWordClass(processedDescriptionPhrases,
                AnnotationFeatures._2PPronounsWordClassName));
        tweetVector.addFeature("Description-Word classes-Person punctuation", AnnotationFeatures.getFeatureForWordClass(processedDescriptionPhrases,
                AnnotationFeatures.personPunctuationWordClassName));
        tweetVector.addFeature("Description-Verb count", AnnotationFeatures.verbsCount(processedDescriptionPhrases));

        //features based on the tweet
        String text = tweetVector.getTweetText();
        tweetVector.addFeature("Tweet-Word classes-Self", AnnotationFeatures.getFeatureForWordClass(processedTweetPhrases,
                AnnotationFeatures.selfWordClassName));
        tweetVector.addFeature("Tweet-Word classes-Others", AnnotationFeatures.getFeatureForWordClass(processedTweetPhrases,
                AnnotationFeatures.othersWordClassName));
        //including plural 1p pronouns may decrease accuracy somewhat
        tweetVector.addFeature("Tweet-Word classes-Plural 1P pronouns", AnnotationFeatures.getFeatureForWordClass(processedTweetPhrases,
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
    private void collectFeaturesEventVsNotEvent(TweetVector tweetVector, String processedTweet, CoreLabel[][] processedPhrasesWHash, List<CoreMap> tweetSentences) throws IOException, InterruptedException {
        //unigram features (tf-idf value of each word)
        //tf-only test
        //tweetVector.addFeatures(tweetTextUnigramModelEvent.getFeaturesForTweetTF(processedPhrasesWHash));

        //bigram features (tf-idf value of each word); bigrams must appear at least thrice to be considered
        //tf-only test
        tweetVector.addFeatures(tweetTextBigramModelEvent.getFeaturesForTweetTF(processedPhrasesWHash));

        //trigram features (tf-idf); trigrams must appear at least 3 times across the dataset to be considered
	    tweetVector.addFeatures(tweetTextTrigramModelEvent.getFeaturesForTweetTF(processedPhrasesWHash));

        //phrase templates
        ArrayList<String> phraseTemplates = AnnotationFeatures.getPhraseTemplates(tweetSentences);
        for (String template: phraseTemplates) {
            tweetVector.addFeature(template, 1.0);
        }

        //topics for tweet
        int[] topTopics = topicFeatureModel.getNMostLikelyTopics(3, processedTweet);
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

    public void collectFeaturesEventVsNotEventParallel(TweetVector tweetVector, CoreLabel[][] phrases) throws IOException {

    }

    /*
        Obtain all features for the self vs. other classifier
     */
    private void collectFeaturesSelfVsOther (TweetVector tweetVector, CoreLabel[][] processedTweetPhrases, List<CoreMap> processedTweetSentences) throws IOException {
        String textModelNameNGrams = processedTextModelName;

        //the number of words/strings in each of the given word classes
        tweetVector.addFeature("Word classes-Past Tense", AnnotationFeatures.getFeatureForWordClass(processedTweetPhrases, AnnotationFeatures.pastTenseWordClassName));
        tweetVector.addFeature("Word classes-Present Tense", AnnotationFeatures.getFeatureForWordClass(processedTweetPhrases, AnnotationFeatures.presentTenseWordClassName));
        tweetVector.addFeature("Word classes-Self", AnnotationFeatures.getFeatureForWordClass(processedTweetPhrases, AnnotationFeatures.selfWordClassName));
        tweetVector.addFeature("Word classes-Others", AnnotationFeatures.getFeatureForWordClass(processedTweetPhrases, AnnotationFeatures.othersWordClassName));
        tweetVector.addFeature("Count Singular Plural Nouns", AnnotationFeatures.countSingularProperNouns(processedTweetPhrases));
        tweetVector.addFeature("Count Plural Nouns", AnnotationFeatures.countPluralProperNouns(processedTweetPhrases));
        tweetVector.addFeature("Numeric References Count", AnnotationFeatures.numericalReferencesCount(processedTweetPhrases));

        String text = tweetVector.getTweetText();

        //Stylometry
        tweetVector.addFeature("Contains Mentions", TextFeatures.containsAt(text));
        tweetVector.addFeature("Contains URL", TextFeatures.containsURL(text));
        tweetVector.addFeature("Count Uppercase Words", TextFeatures.countUpperCaseWords(text));
        tweetVector.addFeature("Count Question Marks", TextFeatures.countQuestionMarkGroups(text));
        tweetVector.addFeature("Other users mentioned?", TextFeatures.containsMention(text));
        tweetVector.addFeature("Hashtag Count", TextFeatures.countHashtags(text));

        //non-word class features over the tweet
        tweetVector.addFeature("Phrases beginning with verb", AnnotationFeatures.phrasesBeginningWithVerb(processedTweetPhrases));
        tweetVector.addFeature("Phrases beginning with past tense verb", AnnotationFeatures.phrasesBeginningWithPastTenseVerb(processedTweetPhrases));
        tweetVector.addFeature("Count Verbs Following Proper Nouns", AnnotationFeatures.properNounsFollowedByVerb(processedTweetPhrases));

//        //OpenIE features
//        int openIESelf = 0;
//        int openIEOther = 0;

        // Loop over sentences in the document
        for (CoreMap sentence : processedTweetSentences) {
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
        String[] firstPronounLastNoun = AnnotationFeatures.pairFirstPronounLastNoun(processedTweetPhrases);
        //First pronoun, last noun pair
        if (firstPronounLastNoun[0] != null && firstPronounLastNoun[1] != null) {
        	tweetVector.addFeature("(" + firstPronounLastNoun[0] + "," + " " + firstPronounLastNoun[1] + ")", 1);
            tweetVector.addFeature("fProLNoun in Self", AnnotationFeatures.countWordsInClassSelf(firstPronounLastNoun));
            tweetVector.addFeature("fProLNoun in Other", AnnotationFeatures.countWordsInClassOther(firstPronounLastNoun));
        }

        String[] firstNounPronounLastVerb = AnnotationFeatures.pairFirstPronounOrNounLastVerb(processedTweetPhrases);
        //First noun or pronoun (not counting proper nouns) last verb pair
        if (firstNounPronounLastVerb[0] != null && firstNounPronounLastVerb[1] != null) {
        	tweetVector.addFeature("(" + firstNounPronounLastVerb[0] + "," + " " + firstNounPronounLastVerb[1] + ")", 1);
            tweetVector.addFeature("fPro|NounVerb in Self", AnnotationFeatures.countWordsInClassSelf(firstNounPronounLastVerb));
            tweetVector.addFeature("fPro|NounVerb in Other", AnnotationFeatures.countWordsInClassOther(firstNounPronounLastVerb));
        }

        //unigrams
        tweetVector.addFeatures(tweetTextUnigramModelSvO.getFeaturesForTweetTF(processedTweetPhrases));
        //bigrams
        tweetVector.addFeatures(tweetTextBigramModelSvO.getFeaturesForTweetTF(processedTweetPhrases));
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

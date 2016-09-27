import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.NoSuchElementException;

import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.Pair;

/*
 * Created by Alec Wolyniec on 6/14/16.
 */
public class NGramModel {

    public static class MinOrientedStringDoublePairComparator implements Comparator<Pair<String, Double>>{
        public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
            double double1 = o1.second();
            double double2 = o2.second();

            if (double1 < double2) {
                return -1;
            }
            else if (double1 == double2) {
                return 0;
            }
            else {
                return 1;
            }
        }
    }

    /*
        Thread that calls the method to get n-gram counts from tweets
     */
/*
    public class GetNGramCountsFromTweetsThread implements Runnable {
        public Thread thread;
        public String threadName;

        //data
        private ArrayList<String[]> tweets;
        private StanfordCoreNLP pipeline;
        //results
        public Hashtable<String, Double> nGramCounts;

        GetNGramCountsFromTweetsThread(ArrayList<String[]> twts, String name) {
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
            pipeline = new StanfordCoreNLP(props);

            tweets = twts;

            threadName = name;
        }

        public void run() {
            System.out.println("Thread"+threadName+"running");
            try {
                nGramCounts = getNGramCountsFromTweets(tweets, pipeline);
            } catch(Exception e) {
                e.printStackTrace();
            }
            System.out.println("Thread"+threadName+"exiting");
        }

        public void start() {
            if (thread == null) {
                thread = new Thread(this, threadName);
                thread.start();
            }
        }

    }
*/

    //parameters to specify type
    private int N;
    private boolean stopWords;
    private String acceptedNGramFilePath;
    private boolean lemmatize;
    private String textModelName;

    private int nGramLimit; //the maximum number of n-grams to have in the model
    private ArrayList<String> stopWordList = new ArrayList<String>();
    //private static Hashtable<String, Long> idfStarts = new Hashtable<String, Long>();
    private Hashtable<String, Double> acceptedNGrams = new Hashtable<String, Double>();
    //private String idfFilePath = "data/term_webcounts.txt";
    private long totalDocs;
    private String dataType;
    //private static Pattern wordPattern = Pattern.compile("((\\w+'?\\w+)|\\w)([ !\"#$%&'()*+,-./:;<=>?@_`{|}~])");
    //private int nCores;

    /*
        Initializes an n-gram model from an int specifying the number of words per gram, a
        dataset to collect idfs on, a string indicating the type of data to collect, the type of classifier,
        a path to a file containing stopwords to be used (or an empty string if stopwords are to be included), and
        an the maximum number of unique n-grams allowed to be stored in this model
     */
    public NGramModel(int n, String pathToTweetFields, String dT, String tMN, String classifierName, String stopWordPath, int limit, boolean lm, int nCores) throws IOException, InterruptedException {
        //initialize fields
        N = n;
        textModelName = tMN;
        lemmatize = lm;
        //this.nCores = nCores;
        nGramLimit = limit;
        dataType = dT;
        if (stopWordPath.length() > 0) {
            getStopWordList(stopWordPath);
            stopWords = false;
        }
        else {
            stopWords = true;
        }

        /*
            First check to see if a file of appropriate n-grams for this type of n-gram model (type is determined by n
            and the frequency threshold) already exists. If it does, use those n-grams as the accepted ones. If not,
            initialize and save a file of accepted n-grams for future reference
         */
        acceptedNGramFilePath = "nGramModels/acceptedNGrams/"+classifierName+"-"+n+"-gram_"+nGramLimit+"-limit.txt";
        //load up/initialize the accepted n-grams
        File nGramFile = new File(acceptedNGramFilePath);
        if (nGramFile.exists()) {
            loadAcceptedNGramsFromFile();
        }
        else {
            initializeAcceptedNGramsFromTweetFields(pathToTweetFields/*, nCores*/);
            saveAcceptedNGramsToFile();
        }
    }

    /*
    Initializes an n-gram model from an int specifying the number of words per gram, a
    dataset to collect idfs on, a string indicating the type of data to collect, the type of classifier,
    and a path to a file containing stopwords to be used (or an empty string if stopwords are to be included)

    This constructor is used when all n-grams found in the training data are to be used
    */
    public NGramModel(int n, String pathToTweetFields, String dT, String tMN, String classifierName, String stopWordPath, boolean lm, int nCores) throws IOException, InterruptedException {
        //initialize fields
        N = n;
        textModelName = tMN;
        lemmatize = lm;
        //this.nCores = nCores;
        nGramLimit = -1;
        dataType = dT;
        if (stopWordPath.length() > 0) {
            getStopWordList(stopWordPath);
            stopWords = false;
        }
        else {
            stopWords = true;
        }

        /*
            First check to see if a file of appropriate n-grams for this type of n-gram model (type is determined by n
            and the frequency threshold) already exists. If it does, use those n-grams as the accepted ones. If not,
            initialize and save a file of accepted n-grams for future reference
         */
        acceptedNGramFilePath = "nGramModels/acceptedNGrams/"+classifierName+"-"+n+"-gram_"+nGramLimit+"-limit.txt";
        //load up/initialize the accepted n-grams
        File nGramFile = new File(acceptedNGramFilePath);
        if (nGramFile.exists()) {
            loadAcceptedNGramsFromFile();
        }
        else {
            initializeAcceptedNGramsFromTweetFields(pathToTweetFields/*, nCores*/);
            saveAcceptedNGramsToFile();
        }
    }

    /*
        Gets all n-grams (with the specified n) from a List of CoreLabels representing a text. Stop words are not
        valid tokens if stopWords is false.
     */
    public ArrayList<String> getNGrams(List<CoreLabel> input, boolean stopWords) {
        ArrayList<String> output = new ArrayList<String>();

        /*
            get each possible sequence of n valid tokens in the input, including sequences that place dummy "pre" and
             "post" tokens before and after the input so that tokens such as {pre, 1st word, 2nd word},
             and {2nd to last word, last word, post} are included (but not {pre, pre, 1st word}; only 1 "pre" and 1 "post"
             are allowed per n-gram)
            A valid token must not be a stopword (if stopWords == false) and must contain at least one
            alphabetic character.
         */
        for (int i = -1; i < input.size(); i++) {
            int[] indices = new int[N];
            int indexCounter = 0;

            //starting from index i, find the next N indices that mark valid tokens
            for (int j = i; j < input.size(); j++) {
                if (indexCounter == N) { break; }
                //check to see if the token at this index is valid
                //it's automatically valid if the index is outside of the range of the input (in this case it's a
                //pre or post token)
                if (j < 0 || j > input.size() - 1) {
                    indices[indexCounter] = j;
                    indexCounter++;
                }
                else {
                    //get the text from the tweet and format it
                    String tokenText = lowerCaseTextOrLemmaUnlessProperNoun(input.get(j));
                    if ((stopWords || !isStopWord(tokenText)) && util.containsAlphabeticCharacters(tokenText) == 1) {
                        indices[indexCounter] = j;
                        indexCounter++;
                    }
                }
                //sets the starting point of the next search so that the search always begins at the first token after the
                //current n-gram's first token
                if (indexCounter == 1) { i = indices[0]; }
            }

            //collect the tokens at the indices to make an n-gram
            if (indexCounter == N) {
                StringBuilder nGram = new StringBuilder();
                for (int index : indices) {
                    if (index < 0) {
                        nGram.append("-pre- ");
                    } else if (index > input.size() - 1) {
                        nGram.append("-post- ");
                    } else {
                        //get the text from the tweet and format it
                        nGram.append(lowerCaseTextOrLemmaUnlessProperNoun(input.get(index)));
                        nGram.append(" ");
                    }
                }
                nGram.replace(nGram.length() - 1, nGram.length(), ""); //there will inevitably be an extra space at the end
                output.add(nGram.toString());
            }
        }

        return output;
    }

    /*
        Gets all n-grams (with the specified n) from a matrix of CoreLabels representing a text
     */
    public ArrayList<String> getNGrams(CoreLabel[][] phrases, boolean stopWords) {
        ArrayList<CoreLabel> convertedInput = new ArrayList<CoreLabel>();
        for (CoreLabel[] phrase: phrases) {
            for (CoreLabel token: phrase) {
                convertedInput.add(token);
            }
        }
        return getNGrams(convertedInput, stopWords);
    }

/*
    public Hashtable<String, Double> getNGramCountsFromTweets(ArrayList<String[]> tweets, StanfordCoreNLP pipeline)
            throws IOException {
        Hashtable<String, Double> nGramCounts = new Hashtable<String, Double>();

        //go through each tweet
        for (int i = 0; i < tweets.size(); i++) {
            String[] currentTweet = tweets.get(i);
            //process the text according to the specified model
            String text = returnAppropriateTextFromTweet(currentTweet);

            //annotate to get lemma annotations
            Annotation document = new Annotation(text);
            pipeline.annotate(document);
            List<CoreLabel> tokens = document.get(TokensAnnotation.class);

            //go through all words in the tweet
            /*
            Matcher matchmaker = wordPattern.matcher(text);
            while (matchmaker.find()) {
                String word = matchmaker.group(1).toLowerCase();
            *
            ArrayList<String> nGrams = getNGrams(tokens, stopWords);
            Hashtable<String, Integer> nGramsThisTweet = new Hashtable<String, Integer>();

            //increment the document counts of all nGrams in the tweet
            for (String nGram: nGrams) {

                //if this n-gram has not yet been seen in this tweet (ensures that each n-gram is only counted once
                //per tweet)
                if (nGramsThisTweet.get(nGram) == null) {
                    //Increment the count of the number of documents the n-gram appears in,
                    //or initialize such a count if the n-gram has not yet been seen
                    boolean seenBefore = false;

                    //check to see if the n-gram has been seen yet
                    if (nGramCounts.get(nGram) != null) {
                        seenBefore = true;
                    }

                    if (seenBefore) nGramCounts.put(nGram, nGramCounts.get(nGram) + 1.0);
                    else nGramCounts.put(nGram, 1.0);
                }

                nGramsThisTweet.put(nGram, 1);
            }
        }

        return nGramCounts;
    }
*/

    /*
        generates a list of stop words from a file
    */
    public void getStopWordList(String stopWordFilePath) throws FileNotFoundException, IOException {
        ArrayList<String> stopWords = new ArrayList<String>(0);
        BufferedReader reader = new BufferedReader(new FileReader(new File(stopWordFilePath)));
        //read
        String currentLine;
        //Pattern pattern = Pattern.compile("(\\S+)\\s");
        Pattern pattern = Pattern.compile("(\\S+)");
        Matcher matcher;
        while ((currentLine = reader.readLine()) != null) {
            matcher = pattern.matcher(currentLine);
            while (matcher.find()) {
                stopWords.add(matcher.group(1));
            }
            //stopWords.add(currentLine);
        }

        reader.close();
        stopWordList = stopWords;
    }

    /*
        This method takes idfs from a file
     */
    /*
    public static void initializeIDFS() throws FileNotFoundException, IOException {
        File idfFile = new File(idfFilePath);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(idfFile));
        //the first line is the total number of documents
        totalDocs = Long.parseLong(bufferedReader.readLine());

        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation currentWord;

        /*
            look through the number of documents each word is in. If it appears in more than 1000 documents,
            and it contains at least one alphabetical character, keep a count of the number of times the
            word's lemma appears (lowercase unless it's a proper noun).
         *
        String currentLine;
        while ( (currentLine = bufferedReader.readLine()) != null ) {
            String[] line = currentLine.split("\\t");
            String word = line[0];
            double documentCountOfWord = Double.parseDouble(line[1]);

            currentWord = new Annotation(word);
            pipeline.annotate(currentWord);
            if (currentWord.get(TokensAnnotation.class).size() == 0) continue;
            CoreLabel thisWord = currentWord.get(TokensAnnotation.class).get(0);

            //only use document counts of words that appear in more than 1000 documents and have at least one alphabetic character
            if (Long.parseLong(line[1]) > 1000 && util.containsAlphabeticCharacters(thisWord.originalText()) == 1) {
                String formattedWord = util.lowerCaseLemmaUnlessProperNoun(thisWord);
                //combine lemmas (lowercase unless proper noun), or at least strings with the same lowercase value, into a single entry
                //if the formatted word is already being counted, add the current count to its count
                if (acceptedNGrams.get(formattedWord) != null) {
                    acceptedNGrams.put(formattedWord, documentCountOfWord + acceptedNGrams.get(formattedWord));
                }
                else {
                    acceptedNGrams.put(formattedWord, documentCountOfWord);
                }
            }
        }

        /*
            Check through all lemma document frequencies. Discard any that are less than 10,000, and divide
            total document number by each document count to get each word's idf
         *
        Enumeration<String> lemmaDFs = acceptedNGrams.keys();
        while (lemmaDFs.hasMoreElements()) {
            String currentLemma = lemmaDFs.nextElement();
            double currentDF = acceptedNGramss.get(currentLemma);
            if (currentDF < 10000) {
                acceptedNGrams.remove(currentLemma);
            }
            else {
                acceptedNGrams.put(currentLemma, totalDocs/currentDF);
            }
        }
        bufferedReader.close();
    }
    */

    /*
        Initializes a list of accepted ngrams from tweet data fields. Only maintains a list of n-grams that appear at
        least as many times as specified by the frequency threshold, and excludes stop words if this model is set
        to do so

        Splits the task of initialization into <nCores> threads
     */
/*
    private void initializeAcceptedNGramsFromTweetFields(String pathToTweetDataFile, int nCores) throws IOException, InterruptedException {
        ArrayList<String[]> tweets = TweetParser.getTweets(pathToTweetDataFile);

        //split the data into nCores portions
        int unit = tweets.size() / nCores;
        int lastStart = 0;
        ArrayList<ArrayList<String[]>> tweetSegments = new ArrayList<ArrayList<String[]>>(nCores);
        for (int i = 0; i < nCores; i++) {
            ArrayList<String[]> nextSegment = new ArrayList<String[]>();
            for (int j = lastStart; j < lastStart + unit; j++) {
                nextSegment.add(tweets.get(j));
            }
            tweetSegments.add(nextSegment);

            lastStart += unit; //advance the index
        }

        //put each portion into a thread, and run each thread to get its partial hashtables
        ArrayList<GetNGramCountsFromTweetsThread> threads = new ArrayList<GetNGramCountsFromTweetsThread>();
        for (int i = 0; i < nCores; i++) {
            GetNGramCountsFromTweetsThread thread = new GetNGramCountsFromTweetsThread(tweetSegments.get(i), "thread"+i);
            threads.add(thread);

            thread.start();
        }

        //wait for all of the threads to finish
        for (GetNGramCountsFromTweetsThread thread: threads) {
            thread.thread.join();
        }

        //put the partial hashtables together and combine them into one larger hashtable, not including the n-grams
        //that don't appear at least <threshold> times across all threads
        for (GetNGramCountsFromTweetsThread thread: threads) {

            Hashtable<String, Double> partialHash = thread.nGramCounts;
            //look through each n-gram in this thread
            Enumeration<String> nGramsThisThread = partialHash.keys();
            while (nGramsThisThread.hasMoreElements()) {
                String thisNGram = nGramsThisThread.nextElement();
                //start the count with this thread
                double count = partialHash.remove(thisNGram);

                //count up its instances across all other threads
                for (GetNGramCountsFromTweetsThread otherThread: threads) {
                    if (otherThread.nGramCounts.get(thisNGram) != null) {
                        count += otherThread.nGramCounts.remove(thisNGram);
                    }
                }

                //if it appears enough times, add it to the full hashtable with a dummy value of 1.0
                if (count >= (double)freqThreshold) {
                    acceptedNGrams.put(thisNGram, 1.0);
                }
            }

        }

    }
*/

    /*
    Initializes a list of accepted ngrams from tweet data fields. Only maintains a list of n-grams that appear at
    least as many times as specified by the frequency threshold, and excludes stop words if this model is set
    to do so
 */
    private void initializeAcceptedNGramsFromTweetFields(String pathToTweetDataFile) throws IOException {
        ArrayList<String[]> tweetFields = TweetParser.getTweets(pathToTweetDataFile);

        totalDocs = tweetFields.size();
        //go through all tweets
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        for (int i = 0; i < tweetFields.size(); i++) {
            String[] currentTweet = tweetFields.get(i);
            //process the text according to the specified model
            String text = returnAppropriateTextFromTweet(currentTweet);

            //annotate to get lemma annotations
            Annotation document = new Annotation(text);
            try {
                pipeline.annotate(document);
            }
            //if an exception is thrown due to an issue with collecting the phrase model, slate this tweet to be skipped
            catch (Exception e) {
                System.out.println("Text \""+text+"\" triggered an exception");
                System.out.println("WARNING: One or more tweets in the training data will be skipped");
                e.printStackTrace();
                continue;
            }
            List<CoreLabel> tokens = document.get(TokensAnnotation.class);

            //go through all words in the tweet
            /*
            Matcher matchmaker = wordPattern.matcher(text);
            while (matchmaker.find()) {
                String word = matchmaker.group(1).toLowerCase();
            */
            ArrayList<String> nGrams = getNGrams(tokens, stopWords);
            Hashtable<String, Integer> nGramsThisTweet = new Hashtable<String, Integer>();

            for (String nGram: nGrams) {

                //Increment the count of the number of documents the word appears in
                if (nGramsThisTweet.get(nGram) == null) { //ensures that each n-gram is only counted once per tweet
                    boolean seenBefore = false;
                    if (acceptedNGrams.get(nGram) != null) {
                        seenBefore = true;
                    }

                    //if this n-gram has been seen before, initialize a count. Otherwise, increment its count
                    if (seenBefore) acceptedNGrams.put(nGram, acceptedNGrams.get(nGram) + 1.0);
                    else acceptedNGrams.put(nGram, 1.0);
                }

                nGramsThisTweet.put(nGram, 1);

            }
        }

        //use all n-grams in acceptedNGrams if no n-gram limit has been set, or if
        //the size of acceptedNGrams does not exceed the limit
        if (nGramLimit == -1 || acceptedNGrams.size() <= nGramLimit) {
            return;
        }

        //collect the most common n-grams, if an n-gram limit has been set
        PriorityQueue<Pair<String, Double>> mostCommonNGrams = new PriorityQueue<Pair<String, Double>>(nGramLimit+1, new MinOrientedStringDoublePairComparator());

        //look through all the n-grams seen thus far
        Enumeration<String> keys = acceptedNGrams.keys();
        while (keys.hasMoreElements()) {
            String nGram = keys.nextElement();
            double value = acceptedNGrams.get(nGram);

            //give the n-gram a dummy value of 1 in the hashtable
            acceptedNGrams.put(nGram, 1.0);

            //check to see if it is one of the most common (if it is not, if it is rejected from the priority queue,
            //remove it from acceptedNGrams)
            mostCommonNGrams.add(new Pair<String, Double>(nGram, value));
            //if mostCommonNGrams has more n-grams than the limit permits, the one at the top
            //is not among the most common n-grams in the dataset
            if (mostCommonNGrams.size() > nGramLimit) {
                //remove from mostCommonNGrams and acceptedNGrams
                String notMostCommonNGram = mostCommonNGrams.poll().first();
                acceptedNGrams.remove(notMostCommonNGram);
            }

        }

    }

    /*
    Return the lemma of the inputted token, in all lower case if it is not a proper noun, with
    only the first letter capitalized if it is a proper noun
    */
    public String lowerCaseTextOrLemmaUnlessProperNoun(CoreLabel token) {
        String text;
        if (lemmatize) {
            text = token.get(LemmaAnnotation.class);
        }
        else {
            text = token.originalText();
        }
        String tag = token.tag();
        if (tag.substring(0, Math.min(3, tag.length())).equals("NNP")) {
            StringBuilder output = new StringBuilder();
            output.append(Character.toUpperCase(text.charAt(0)));
            output.append(text.toLowerCase().substring(1));
            return output.toString();
        }
        else {
            return text.toLowerCase();
        }
    }

    public Hashtable<String, Double> getAcceptedNGrams () {
        return acceptedNGrams;
    }

    public long getNumIDFs() {
        return (long)acceptedNGrams.size();
    }

    /*
        Returns the proper text field from the given TweetVector, given the data field
     */
    public String returnAppropriateTextFromTweet(TweetVector tweetVector) {
        String data = "";
        switch (dataType) {
            case TweetFeatureExtractor.textName: data = tweetVector.getTweetText(); break;
            case TweetFeatureExtractor.descriptionName: data = tweetVector.getDescription(); break;
        }
        return TweetFeatureExtractor.processStatic(data, textModelName);
    }

    public String returnAppropriateTextFromTweet(String[] tweetFields) {
        String data = "";
        switch (dataType) {
            case TweetFeatureExtractor.textName: data = tweetFields[4]; break;
            case TweetFeatureExtractor.descriptionName: data = tweetFields[3]; break;
        }
        return TweetFeatureExtractor.processStatic(data, textModelName);
    }

    /*
        Removes stop words from strings using the given stop word guide
     */
    /*
    public String removeStopWords(String input) throws IOException {
        StringBuilder output = new StringBuilder();
        //go through the input string and locate all word entities. Don't include stopwords in the output
        int lastStoppedIndex = 0;
        Matcher matcher = wordPattern.matcher(input);
        while (matcher.find()) {
            output.append(input.substring(lastStoppedIndex, matcher.start()));
            String word = matcher.group(1).toLowerCase();
            //check to see if word is a stopword
            if (!isStopWord(word)) output.append(input.substring(matcher.start(), matcher.end()));
            else if (input.charAt(matcher.end()-1) != ' ') output.append(input.charAt(matcher.end()-1));

            lastStoppedIndex = matcher.end();
        }
        output.append(input.substring(lastStoppedIndex));

        //System.out.println(output);
        return output.toString();
    }
    */

    public boolean isStopWord(String input) {
        for (String stopWord: stopWordList) {
            if (input.equals(stopWord)) return true;
        }
        return false;
    }

    /*
        For the given set of phrases, obtain all n-grams and record the number of times they appear
     */
    public Hashtable<String, Double> getTermFrequenciesForTweet(CoreLabel[][] phrases, boolean stopWords) {
        Hashtable<String, Double> output = new Hashtable<String, Double>();
        ArrayList<String> nGrams = getNGrams(phrases, stopWords);

        for (String nGram: nGrams) {
            //create entry or increment
            if (acceptedNGrams.get(nGram) != null) { //ensure that the ngram appears at least <threshold> times in the dataset
                if (output.get(nGram) == null) output.put(nGram, 1.0);
                else output.put(nGram, output.get(nGram) + 1.0);
            }
        }

        return output;
    }


    /*
        Takes a dictionary of words and their term frequencies, multiplies each word's term frequency by its
         idf
     */
    /*
    public Hashtable<String, Double> convertTweetByIDFs(Hashtable<String, Double> input) throws IOException {
        Hashtable<String, Double> output = new Hashtable<String, Double>();

        assert (acceptedNGrams.size() > 0);
        Enumeration<String> tfKeys = input.keys();
        while (tfKeys.hasMoreElements()) {
            String tfKey = tfKeys.nextElement();

            //get the word's idf. If it is the default value of 0
            // (i.e. it has not appeared in enough documents to be stored), the word will not be in the output
            double idf = 0.0;
            idf = acceptedNGrams.get(tfKey);

            //get tf-idf value
            output.put(tfKey+"-TFIDF", input.get(tfKey) * idf); //the addition of "TFIDF" allows for separate sets
                                                                    //of TF and TF-IDF features to be used
        }


        return output;
    }
    */

    /*
        Gets a dictionary (hash table) of all n-grams in the document (excluding stop words if this model excludes stop words)
        and their tf-idf scores
     */
    /*
    public Hashtable<String, Double> getFeaturesForTweetTFIDF(CoreLabel[][] phrases) throws IOException {
        //collect term frequencies
        Hashtable<String, Double> unigramFeatures = getTermFrequenciesForTweet(phrases, stopWords);

        //divide by idfs
        unigramFeatures = convertTweetByIDFs(unigramFeatures);

        return unigramFeatures;
    }
    */

    /*
        Gets a dictionary (hash table) of all n-grams in the document (excluding stop words if this model excludes them)
        and their tf scores
     */
    public Hashtable<String, Double> getFeaturesForTweetTF(CoreLabel[][] phrases) throws IOException {
        return getTermFrequenciesForTweet(phrases, stopWords);
    }

    /*
        Initializes accepted n-grams
     */
    private void loadAcceptedNGramsFromFile() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(acceptedNGramFilePath)));

        String currentLine;
        while ((currentLine = bufferedReader.readLine()) != null) {
            acceptedNGrams.put(currentLine, 1.0);
        }

        bufferedReader.close();
    }

    /*
        Save the accepted n-grams to a file where each is given its own line
     */
    private void saveAcceptedNGramsToFile() throws IOException {
        File nGramFile = new File(acceptedNGramFilePath);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(nGramFile, false));

        Enumeration<String> accepteds = acceptedNGrams.keys();
        int counter = 0;
        while (accepteds.hasMoreElements()) {
            if (counter != 0) {
                bufferedWriter.newLine();
            }
            counter = 1;
            bufferedWriter.write(accepteds.nextElement());
        }
        bufferedWriter.close();
    }

}

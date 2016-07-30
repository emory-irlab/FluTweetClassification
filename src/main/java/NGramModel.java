import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Hashtable;
import java.util.Enumeration;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.pipeline.*;

/*
 * Created by Alec Wolyniec on 6/14/16.
 */
public class NGramModel {
    //general helper parameters
    public static final String textName = "text";
    public static final String descriptionName = "description";

    //parameters to specify type
    private int N;
    private boolean stopWords;

    private int freqThreshold;
    private ArrayList<String> stopWordList = new ArrayList<String>();
    //private static Hashtable<String, Long> idfStarts = new Hashtable<String, Long>();
    private Hashtable<String, Double> tweetIDFs = new Hashtable<String, Double>();
    //private String idfFilePath = "data/term_webcounts.txt";
    private long totalDocs;
    private String dataType;
    private static Pattern wordPattern = Pattern.compile("((\\w+'?\\w+)|\\w)([ !\"#$%&'()*+,-./:;<=>?@_`{|}~])");

    /*
        Initializes an n-gram model from an int specifying the number of words per gram, a
        dataset to collect idfs on, a string indicating the type of data to collect, a path to a file containing stopwords
        to be used (or an empty string if stopwords are to be included), and an int specifying the minimum number of documents
        an n-gram must appear in within the training data in order to be considered
     */
    public NGramModel(int n, TweetVector[] tweetVectors, String dT, String stopWordPath, int freq) throws IOException {
        N = n;
        freqThreshold = freq;
        dataType = dT;
        if (stopWordPath.length() > 0) {
            getStopWordList(stopWordPath);
            stopWords = false;
        }
        else {
            stopWords = true;
        }
        initializeIDFsFromTweetFields(tweetVectors);
    }

    /*
        Gets all n-grams (with the specified n) from a List of CoreLabels representing a text. Stop words are not
        valid tokens if stopWords is false.
     */
    public ArrayList<String> getNGrams(List<CoreLabel> input, boolean stopWords) {
        ArrayList<String> output = new ArrayList<String>();

        /*
            get each possible sequence of n valid tokens in the input, including sequences that place dummy "pre" and
             "post" tokens before and after the input so that tokens such as {pre, pre, 1st word},
             {pre, 1st word, 2nd word}, and {2nd to last word, last word, post} are included.
            A valid token must not be a stopword (if stopWords == false) and must contain at least one
            alphabetic character.
         */
        for (int i = (1 - N); i < input.size(); i++) {
            int[] indices = new int[N];
            int indexCounter = 0;

            //starting from index i, find the next N indices that mark valid tokens
            for (int j = i; j < (input.size() - 1 + N); j++) {
                if (indexCounter == N) { break; }
                //check to see if the token at this index is valid
                //it's automatically valid if the index is outside of the range of the input (in this case it's a
                //pre or post token)
                if (j < 0 || j > input.size() - 1) {
                    indices[indexCounter] = j;
                    indexCounter++;
                }
                else {
                    String tokenText = util.lowerCaseLemmaUnlessProperNoun(input.get(j));
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
                        nGram.append(util.lowerCaseLemmaUnlessProperNoun(input.get(index)));
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

        stopWordList = stopWords;
    }

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
                if (tweetIDFs.get(formattedWord) != null) {
                    tweetIDFs.put(formattedWord, documentCountOfWord + tweetIDFs.get(formattedWord));
                }
                else {
                    tweetIDFs.put(formattedWord, documentCountOfWord);
                }
            }
        }

        /*
            Check through all lemma document frequencies. Discard any that are less than 10,000, and divide
            total document number by each document count to get each word's idf
         *
        Enumeration<String> lemmaDFs = tweetIDFs.keys();
        while (lemmaDFs.hasMoreElements()) {
            String currentLemma = lemmaDFs.nextElement();
            double currentDF = tweetIDFs.get(currentLemma);
            if (currentDF < 10000) {
                tweetIDFs.remove(currentLemma);
            }
            else {
                tweetIDFs.put(currentLemma, totalDocs/currentDF);
            }
        }
    }
    */

    /*
        Initializes idfs from tweet data fields. Only maintains a list of those that appear at least as many
        times as specified by the frequency threshold, and excludes stop words if this model is set to do so
     */
    private void initializeIDFsFromTweetFields(TweetVector[] tweetVectors) {
        totalDocs = tweetVectors.length;
        //go through all tweets
        for (int i = 0; i < tweetVectors.length; i++) {
            String text = readTweetsGetFeatures.process(returnAppropriateTextForm(tweetVectors[i]));

            //annotate to get lemma annotations
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
            Annotation document = new Annotation(text);
            pipeline.annotate(document);
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
                Enumeration<String> keys = tweetIDFs.keys();
                boolean seenBefore = false;
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    if (key.equals(nGram)) {
                        seenBefore = true;
                        break;
                    }
                }
                if (nGramsThisTweet.get(nGram) == null) { //ensures that each n-gram is only counted once per tweet
                    if (seenBefore) tweetIDFs.put(nGram, tweetIDFs.get(nGram) + 1.0);
                    else tweetIDFs.put(nGram, 1.0);
                }

                nGramsThisTweet.put(nGram, 1);
            }
        }

        //Get idfs from totalDocs and the document count of each word that appears at least as many times
        //as the frequency threshold requires
        Enumeration<String> keys = tweetIDFs.keys();
        int twoCounter = 0;
        int threeCounter = 0;
        int fiveCounter = 0;
        int tenCounter = 0;
        int twentyFiveCounter = 0;
        int fiftyCounter = 0;
        int hundredCounter = 0;
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();

            //counter tests
            if (tweetIDFs.get(key) >= 2.0) {
                twoCounter++;
            }
            if (tweetIDFs.get(key) >= 3.0) {
                threeCounter++;
            }
            if (tweetIDFs.get(key) >= 5.0) {
                fiveCounter++;
            }
            if (tweetIDFs.get(key) >= 10.0) {
                tenCounter++;
            }
            if (tweetIDFs.get(key) >= 25.0) {
                twentyFiveCounter++;
            }
            if (tweetIDFs.get(key) >= 50.0) {
                fiftyCounter++;
            }
            if (tweetIDFs.get(key) >= 100.0) {
                hundredCounter++;
            }

            if (tweetIDFs.get(key) >= freqThreshold) {
                tweetIDFs.put(key, totalDocs / tweetIDFs.get(key));
            }
            else {
                tweetIDFs.remove(key);
            }


        }

        System.out.println();
    }

    /*
        Saves all idfs to a given path
     *
    private void saveIDFs(String idfPath) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(idfPath)));
        Enumeration<String> keys = tweetIDFs.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            bufferedWriter.write(key+","+tweetIDFs.get(key));
        }

        bufferedWriter.close();
    }
    */

    public Hashtable<String, Double> getTweetIDFs () {
        return tweetIDFs;
    }

    public long getNumIDFs() {
        return (long)tweetIDFs.size();
    }

    /*
        Returns the proper text field from the given TweetVector, given the data field
     */
    public String returnAppropriateTextForm(TweetVector tweetVector) {
        switch (dataType) {
            case textName: return tweetVector.getTweetText();
            case descriptionName: return tweetVector.getDescription();
        }
        return "";
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
            if (tweetIDFs.get(nGram) != null) { //ensure that the ngram appears at least <threshold> times in the dataset
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
    public Hashtable<String, Double> convertTweetByIDFs(Hashtable<String, Double> input) throws IOException {
        Hashtable<String, Double> output = new Hashtable<String, Double>();

        assert (tweetIDFs.size() > 0);
        Enumeration<String> tfKeys = input.keys();
        while (tfKeys.hasMoreElements()) {
            String tfKey = tfKeys.nextElement();

            //get the word's idf. If it is the default value of 0
            // (i.e. it has not appeared in enough documents to be stored), the word will not be in the output
            double idf = 0.0;
            if (tweetIDFs.get(tfKey) != null) { //unnecessary check at this point
                idf = tweetIDFs.get(tfKey);
            }

            //get tf-idf value
            if (idf > 0.0) {
                output.put(tfKey, input.get(tfKey) * idf);
            }
        }

        return output;
    }

    /*
        Gets a dictionary (hash table) of all n-grams in the document (excluding stop words if this model excludes stop words)
        and their tf-idf scores
     */
    public Hashtable<String, Double> getFeaturesForTweetTFIDF(CoreLabel[][] phrases) throws IOException {
        //collect term frequencies
        Hashtable<String, Double> unigramFeatures = getTermFrequenciesForTweet(phrases, stopWords);

        //divide by idfs
        unigramFeatures = convertTweetByIDFs(unigramFeatures);

        return unigramFeatures;
    }

    /*
        Gets a dictionary (hash table) of all n-grams in the document (excluding stop words if this model excludes them)
        and their tf scores
     */
    public Hashtable<String, Double> getFeaturesForTweetTF(CoreLabel[][] phrases) throws IOException {
        return getTermFrequenciesForTweet(phrases, stopWords);
    }

}

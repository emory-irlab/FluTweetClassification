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

/**
 * Created by Alec Wolyniec on 6/14/16.
 */
public class UnigramModel {
    private static ArrayList<String> stopWordList = new ArrayList<String>();
    //private static Hashtable<String, Long> idfStarts = new Hashtable<String, Long>();
    private static Hashtable<String, Double> tweetIDFs = new Hashtable<String, Double>();
    private static String stopWordFilePath = "data/stopwords.txt";
    private static String idfFilePath = "data/term_webcounts.txt";
    private static long totalDocs;
    private static Pattern wordPattern = Pattern.compile("((\\w+'?\\w+)|\\w)([ !\"#$%&'()*+,-./:;<=>?@_`{|}~])");

    /*
    generates a list of stop words from a file
    */
    public static void getStopWordList() throws FileNotFoundException, IOException {
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
         */
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
         */
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

    /*
        Initializes idfs from tweet texts
     */
    public static void updateIDFsFromTweetText(TweetVector[] tweetVectors) {
        totalDocs = tweetVectors.length;
        //go through all tweets
        for (int i = 0; i < tweetVectors.length; i++) {
            String text = tweetVectors[i].getTweetText();

            //annotate to get lemma annotations
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
            Annotation document = new Annotation(tweetVectors[i].getTweetText());
            pipeline.annotate(document);
            List<CoreLabel> tokens = document.get(TokensAnnotation.class);

            //go through all words in the tweet
            /*
            Matcher matchmaker = wordPattern.matcher(text);
            while (matchmaker.find()) {
                String word = matchmaker.group(1).toLowerCase();
            */
            for (CoreLabel token: tokens) {
                String word = util.lowerCaseLemmaUnlessProperNoun(token);
                //System.out.println(word);

                //Increment the count of the number of documents the word appears in
                Enumeration<String> keys = tweetIDFs.keys();
                boolean seenBefore = false;
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    if (key.equals(word)) seenBefore = true;
                    break;
                }
                if (seenBefore) tweetIDFs.put(word, tweetIDFs.get(word) + 1);
                else tweetIDFs.put(word, 1.0);
            }
        }

        //Get idfs from totalDocs and the document count of each word
        Enumeration<String> keys = tweetIDFs.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            tweetIDFs.put(key, totalDocs/tweetIDFs.get(key));
        }
    }

    /*
        Removes stop words from strings using the given stop word guide
     */
    public static String removeStopWords(String input) throws IOException {
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

    public static boolean isStopWord(String input) {
        for (String stopWord: stopWordList) {
            if (input.equals(stopWord)) return true;
        }
        return false;
    }

    public static Hashtable<String, Double> getTermFrequencies(CoreLabel[][] phrases, boolean stopWords) {
        Hashtable<String, Double> output = new Hashtable<String, Double>();
        for (CoreLabel[] phrase: phrases) {
            for (CoreLabel token: phrase) {
                String text = util.lowerCaseLemmaUnlessProperNoun(token);

                //don't use if the token contains no alphabetic characters
                if (util.containsAlphabeticCharacters(text) == 0) continue;

                //only use stopwords if requested in args
                if (!stopWords && isStopWord(text)) continue;

                //create entry or increment
                if (output.get(text) == null) output.put(text, 1.0);
                else output.put(text, output.get(text) + 1.0);
            }
        }
        return output;
    }


    /*
        Takes a dictionary of words and their term frequencies, multiplies each word's term frequency by its
         idf
     */
    public static Hashtable<String, Double> convertByIDFs(Hashtable<String, Double> input) throws IOException {
        assert (tweetIDFs.size() > 0);
        Enumeration<String> tfKeys = input.keys();
        while (tfKeys.hasMoreElements()) {
            String tfKey = tfKeys.nextElement();

            //get the word's idf. If it has none, input a default idf of totalDocs/1.0
            double idf = (double) totalDocs;
            Enumeration<String> idfKeys = tweetIDFs.keys();
            while (idfKeys.hasMoreElements()) {
                String idfKey = idfKeys.nextElement();
                if (idfKey.equals(tfKey)) {
                    idf = tweetIDFs.get(tfKey);
                    break;
                }
            }

            //get tf-idf value
            input.put(tfKey, input.get(tfKey)*idf);
        }

        return input;
    }

    /*
        Gets a dictionary (hash table) of all terms in the document (excluding stop words) and their tf-idf score
     */
    public static Hashtable<String, Double> getFeaturesTFIDFNoStopWords(CoreLabel[][] phrases) throws IOException {
        if (stopWordList.size() == 0) getStopWordList();
        //collect term frequencies
        Hashtable<String, Double> unigramFeatures = getTermFrequencies(phrases, false);

        //divide by idfs
        unigramFeatures = convertByIDFs(unigramFeatures);

        return unigramFeatures;
    }
}

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
    private static int numDocs;
    private static ArrayList<String> stopWordList = new ArrayList<String>();
    //private static Hashtable<String, Long> idfStarts = new Hashtable<String, Long>();
    private static Hashtable<String, Double> tweetIDFs = new Hashtable<String, Double>();
    private static String stopWordFilePath = "data/stopwords.txt";
    //private static String idfFilePath = "data/term_webcounts.txt";
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

    /*
        Gets list of idfs from file
     */
    /*
    public static void initializeIDFs() throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(idfFilePath)));
        String currentLine;
        //the first line is the number of documents
        totalDocs = Long.parseLong(reader.readLine());

        String lastStart = "";
        long lineCounter = 0;
        long lastLineNum = 0;
        long greatestLineDiff = 0;
        long numUnder50Counter = 0;
        long numOver10KCounter = 0;
        while ((currentLine = reader.readLine()) != null) {
            String prefix = currentLine.substring(0, 5);
            if (prefix.indexOf("\t") != -1) prefix = prefix.substring(0, prefix.indexOf("\t"));

            if (!prefix.equals(lastStart)) {
                //System.out.println(lineCounter);
                lastStart = prefix;
                long arrayLength = lineCounter - lastLineNum;
                if (arrayLength < 50) numUnder50Counter++;
                if (arrayLength > 10000) numOver10KCounter++;
                if (arrayLength > greatestLineDiff) greatestLineDiff = arrayLength;
                lastLineNum = lineCounter;
                idfStarts.put(prefix, lineCounter);
            }
            lineCounter++;
        }
        System.out.println(idfStarts.size());
        System.out.println("Longest: "+greatestLineDiff);
        System.out.println("Number under 50: "+numUnder50Counter);
        System.out.println("Number over 10k: "+numOver10KCounter);
    }
    */

    /*
        Initializes idfs from tweet texts
     */
    public static void updateIDFsFromTweetText(TweetVector[] tweetVectors) {
        numDocs = tweetVectors.length;
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

        //Get idfs from numDocs and the document count of each word
        Enumeration<String> keys = tweetIDFs.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            tweetIDFs.put(key, numDocs/tweetIDFs.get(key));
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
                if (TextFeatures.containsAlphabeticCharacters(text) == 0) continue;

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

            //get the word's idf. If it has none, input a default idf of numDocs/1.0
            double idf = numDocs;
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

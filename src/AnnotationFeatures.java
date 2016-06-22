import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.pipeline.*;

/**
 * Created by Alec Wolyniec on 6/8/16.
 */
public class AnnotationFeatures {
    /*
    Pre-defined word classes. Some entries contain special cases, rules specifying that the string to be matched to it
    is not a single word.

    Special cases (checked for in the listed order, cannot be combined):
     1. A single digit between 2-9 before words denotes multi-word features. The number indicates the number of words to search for.
        Note: If there is a feature for an individual word or set of words in a multi-word feature, the detection
        of a multi-word feature will not prevent detection of the sub-feature. Example: The string "the flu" will
        trigger will count as an instance of "the", an instance of "flu", and an instance of "the flu"
     2. "V-" denotes a verb ending. The feature extraction algorithm should match this entry to the ending of a verb
       word being scanned, and not the word itself
    */

    //Note: Multi-word words need to go before single-word words
    private static String[][] wordClasses = {
            {"Infection",
                    "getting", "got", "recovered", "have", "having", "had", "has", "catching", "catch", "cured", "infected"},
            {"Possession",
                    "2the flu", "bird", "flu", "sick", "epidemic"},
            {"Concern",
                    "afraid", "worried", "scared", "fear", "worry", "nervous", "dread", "dreaded", "terrified"},
            {"Vaccination",
                    "2nasal spray", "vaccine", "vaccines", "shot", "shots", "mist", "tamiflu", "jab"},
            {"Past Tense",
                    "was", "did", "had", "got", "were", "V-ed"},
            {"Present Tense",
                    "2it 's", "is", "am", "are", "have", "has", "V-ing"}, //'s as in "is"?
            {"Self",
                    "2I 've", "2I 'd", "2I 'm", "im", "my", "me", "I"},
            {"Others",
                    "2he 's", "2she 's", "2you 're", "2they 're", "2she 'll", "2he 'll", "your",
                    "everyone", "you", "it", "its", "u", "her", "he", "she", "they", "husband", "wife", "brother",
                    "sister", "people", "kid", "kids", "children", "son", "daughter", "his", "hers", "him"},
            {"Plural 1P pronouns",
                    "we", "our", "ourselves", "ours", "us"},
            {"2P pronouns",
                    "you", "your", "yours", "y'all", "yall", "u"},
            {"Follow me",
                    "follow", "tweet", "visit"},
            {"Numerical references",
                    "2a couple", "2a lot", "many", "some", "all", "most", "lots", "none", "much", "few"},
            {"Org. account descriptions",
                    "official", "twitter", "account", "follow", "tweet", "us"},
            {"Person punctuation",
                    ",", "|", "&"},
    };

    public static int phrasesBeginningWithVerb(CoreLabel[][] phrases) {
        int counter = 0;
        for (CoreLabel[] phrase: phrases) {
            if (phrase[0].tag().charAt(0) == 'V') counter++;
        }
        return counter;
    }

    public static int phrasesBeginningWithPastTenseVerb(CoreLabel[][] phrases) {
        int counter = 0;
        for (CoreLabel[] phrase: phrases) {
            String tag = phrase[0].tag();
            if (tag.equals("VBD") || tag.equals("VBN")) counter++;
        }
        return counter;
    }

    public static int numericalReferencesCount(CoreLabel[][] phrases) {
        int counter = 0;
        for (CoreLabel[] phrase : phrases) {
            for (CoreLabel token : phrase) if (token.tag().equals("CD")) counter++;
        }
        counter += getFeatureForWordClass(phrases, "Numerical references");
        return counter;
    }

    public static int verbsCount(CoreLabel[][] phrases) {
        int counter = 0;
        for (CoreLabel[] phrase: phrases) {
            for (CoreLabel token: phrase) if (token.tag().charAt(0) == 'V') counter++;
        }
        return counter;
    }

    /*
    Count the number of words/strings in the given word class
    */
    public static int getFeatureForWordClass(CoreLabel[][] phrases, String relevantClassName) {
        //initialize
        int counter = 0;
        String[] relevantWordClass = new String[1];
        for (String[] aClass: wordClasses) {
            if (aClass[0].equals(relevantClassName)) {
                relevantWordClass = aClass;
                break;
            }
        }
        if (relevantWordClass.length == 1) {
            System.err.println("ERROR: Word class requested, "+relevantClassName+", does not exist.");//change to exception
            System.exit(1);
        }
        //go over each phrase
        for (CoreLabel[] phrase: phrases) {
            //go through each word in the phrase
            for (int i = 0; i < phrase.length; i++) {
                CoreLabel token = phrase[i];
                String stringInPhrase = token.get(CoreAnnotations.TextAnnotation.class);
                String stringInPhrasePOS = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                //System.out.println(stringInPhrase);

                //get words to match
                for (int k = 1; k < relevantWordClass.length; k++) {
                    String stringInPhraseCopy = stringInPhrase; //use this when referring to the input token
                    String stringToMatch = relevantWordClass[k];
                    //Alter the string to match and the copy of the input token if this is a special case

                    //Special case 1: Multiple words are to be scanned
                    int possibleNum = (int)stringToMatch.charAt(0) - '0';
                    if (possibleNum > 1 && possibleNum < 10) {
                        stringToMatch = stringToMatch.substring(1);
                        StringBuilder buildMatch = new StringBuilder(stringInPhraseCopy);
                        int parallelCount = i;
                        //peek at the next words in the phrase, add them to the string to match
                        while (possibleNum > 1) {
                            parallelCount++;
                            if (parallelCount == phrase.length) break;
                            buildMatch.append(" ");
                            buildMatch.append(phrase[parallelCount].get(CoreAnnotations.TextAnnotation.class));
                            possibleNum--;
                        }
                        stringInPhraseCopy = buildMatch.toString();
                        //if the multi-word phrase is found, make sure the words inside it are not scanned
                        if (stringToMatch.equalsIgnoreCase(stringInPhraseCopy)) i = parallelCount;
                    }
                    //Special case 2: The string to be matched is a verb ending, so just compare endings
                    else if (stringToMatch.length() > 1 && stringToMatch.substring(0, 2).equalsIgnoreCase("V-") && stringInPhrasePOS.charAt(0) == 'V') {
                        stringToMatch = stringToMatch.substring(2);
                        int startIndex = stringInPhrase.length() - stringToMatch.length();
                        if (startIndex > 0) {
                            stringInPhraseCopy = stringInPhraseCopy.substring(stringInPhrase.length() - stringToMatch.length());
                        }
                    }
                    //match
                    if (stringToMatch.equalsIgnoreCase(stringInPhraseCopy)) {
                        counter++;
                        //System.out.println("Matched string "+stringInPhraseCopy+" from base string "+stringInPhrase+" to string "+stringToMatch+" in word class "+relevantWordClass[0]);
                        break;
                    }
                }
            }
        }
        return counter;
    }
}

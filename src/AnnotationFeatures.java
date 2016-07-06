import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.semgraph.*;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.trees.*;
import java.util.Iterator;

import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.*;
import java.util.ArrayList;
import java.util.List;


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
    //word class names
    public final static String infectionWordClassName = "Infection";
    public final static String possessionWordClassName = "Possession";
    public final static String concernWordClassName = "Concern";
    public final static String vaccinationWordClassName = "Vaccination";
    public final static String pastTenseWordClassName = "Past Tense";
    public final static String presentTenseWordClassName = "Present Tense";
    public final static String selfWordClassName = "Self";
    public final static String othersWordClassName = "Others";
    public final static String plural1PPronounsWordClassName = "Plural 1P Pronouns";
    public final static String _2PPronounsWordClassName = "2P Pronouns";
    public final static String followMeWordClassName = "Follow Me";
    public final static String numericalReferencesWordClassName = "Numerical References";
    public final static String orgAccountDescriptionsWordClassName = "Org. Account Descriptions";
    public final static String personPunctuationWordClassName = "Person Punctuation";
    public static ArrayList<String> topicWordClassNames = new ArrayList<String>();
    public static String topicWordClassFilePath = "data/topics/tweet_key_500.txt";

    private static Hashtable<String, String[]> wordClasses = new Hashtable<String, String[]>();
    //Note: Multi-word words need to go before single-word words
    private static String[] infectionWordClass = {"getting", "got", "recovered", "have", "having", "had", "has",
            "catching", "catch", "cured", "infected"};
    private static String[] possessionWordClass = {"2the flu", "bird", "flu", "sick", "epidemic"};
    private static String[] concernWordClass = {"afraid", "worried", "scared", "fear", "worry", "nervous", "dread",
            "dreaded", "terrified"};
    private static String[] vaccinationWordClass = {"2nasal spray", "vaccine", "vaccines", "shot", "shots", "mist",
            "tamiflu", "jab"};
    private static String[] pastTenseWordClass = {"was", "did", "had", "got", "were", "V-ed"};
    private static String[] presentTenseWordClass = {"2it 's", "is", "am", "are", "have", "has", "V-ing"}; //'s as in "is"?
    private static String[] selfWordClass = {"2I 've", "2I 'd", "2I 'm", "im", "my", "me", "I"};
    private static String[] othersWordClass = {"2he 's", "2she 's", "2you 're", "2they 're", "2she 'll", "2he 'll", "your",
            "everyone", "you", "it", "its", "u", "her", "he", "she", "they", "husband", "wife", "brother",
            "sister", "people", "kid", "kids", "children", "son", "daughter", "his", "hers", "him"};
    private static String[] plural1PPronounsWordClass = {"we", "our", "ourselves", "ours", "us"};
    private static String[] _2PPronounsWordClass = {"2you 're", "2y 'all", "you", "your", "yours",
            "yall", "u", "ur", "yourself", "yourselves"};
    private static String[] followMeWordClass = {"follow", "tweet", "visit"};
    private static String[] numericalReferencesWordClass = {"2a couple", "2a lot", "many", "some", "all", "most",
            "lots", "none", "much", "few"};
    private static String[] orgAccountDescriptionsWordClass = {"official", "twitter", "account", "follow", "tweet", "us"};
    private static String[] personPuncuationWordClass = {",", "|", "&"};

    /*
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
    */

    public static void initializeWordClasses(String pathToTopicFile) throws IOException {
        wordClasses.put(infectionWordClassName, infectionWordClass);
        wordClasses.put(possessionWordClassName, possessionWordClass);
        wordClasses.put(concernWordClassName, concernWordClass);
        wordClasses.put(vaccinationWordClassName, vaccinationWordClass);
        wordClasses.put(pastTenseWordClassName, pastTenseWordClass);
        wordClasses.put(presentTenseWordClassName, presentTenseWordClass);
        wordClasses.put(selfWordClassName, selfWordClass);
        wordClasses.put(othersWordClassName, othersWordClass);
        wordClasses.put(plural1PPronounsWordClassName, plural1PPronounsWordClass);
        wordClasses.put(_2PPronounsWordClassName, _2PPronounsWordClass);
        wordClasses.put(followMeWordClassName, followMeWordClass);
        wordClasses.put(numericalReferencesWordClassName, numericalReferencesWordClass);
        wordClasses.put(orgAccountDescriptionsWordClassName, orgAccountDescriptionsWordClass);
        wordClasses.put(personPunctuationWordClassName, personPuncuationWordClass);

        //get topic word classes
        Hashtable<String, String[]> topics = getTopics(pathToTopicFile);
        Enumeration<String> topicNames = topics.keys();
        while (topicNames.hasMoreElements()) {
            String currentName = topicNames.nextElement();
            topicWordClassNames.add(currentName);
            wordClasses.put(currentName, topics.get(currentName));
        }
    }

    /*
        *********************
        *  ACTUAL FEATURES  *
        *********************
     */

    /*
        Return phrase templates (in the form of ArrayLists) of words filling certain semantic roles. Returns the
        following templates for each sentence.

        {subject, verb, object}
        {subject, object}
        {subject, verb}
        {verb, object}

        Each role is fulfilled by the following groups of words.
        Subjects: The head noun of the subject group and all nouns depending on it
        Verbs: Single word for the verb
        Objects: The head noun of the object group and all nouns depending on it, and a single preposition (if it is
        right before the object group)

        The subject is the "nsubj" directly depending on the verb,
        and the object is the "nobj" directly depending on the verb. Each word is marked with /S if it is the
        subject, /V if it is the verb, and /O if it is the object
     */
    public static ArrayList<String[]> getPhraseTemplates(List<CoreMap> sentences) {
        System.out.println();
        System.out.println("New tweet: ");

        ArrayList<String[]> phraseTemplates = new ArrayList<String[]>();
        //look through each sentence
        for (CoreMap sentence: sentences) {
            System.out.println();
            System.out.println("New sentence: ");
            /*
            SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);

            //check for all types of subject-verb-object templates
            for (IndexedWord vertex : graph.vertexListSorted()) {

                //for verbs
                if (vertex.tag().charAt(0) == 'V') {
                    //Simple case: verb has an nsubj (direct noun subject) and a dobj (direct object)
                    ArrayList<String[]> templatesNSubjDObj = getTemplatesNSubjDObj(graph, vertex);
                    for (String[] template: templatesNSubjDObj) phraseTemplates.add(template);
                }

                System.out.println(vertex.originalText() + "/" + vertex.tag());
                if (/*vertex.tag().charAt(0) == 'V'true) {
                    for (Pair<GrammaticalRelation, IndexedWord> pair: graph.childPairs(vertex)) {
                        //System.out.println("Child: "+pair.second().originalText()+"/"+pair.second().tag()+", Relation: "+pair.first().getShortName()+" "+pair.first().getLongName());
                    }

                    //get the subject and the object of each verb

                    //create the templates

                    //add the templates to the final list
                }
            }
            */

            //full parse version

            Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
            ArrayList<String[]> templates = traverseForTemplates(tree, tree); //add to phraseTemplates

            tree.pennPrint();

        }

        return phraseTemplates;
    }

    /*
        Traverse the tree, extracting all subject-verb-object phrase templates
    */
    private static ArrayList<String[]> traverseForTemplates(Tree tree, Tree currentNode) {
        ArrayList<String[]> templates = new ArrayList<String[]>();
        String subject = "";
        String verb = "";
        String object = "";

        //System.out.println(currentNode.nodeString()+", "+currentNode.label().toString());
        String nodeValue = currentNode.value();
        //If a verb POS tag is found, use the child as the verb, search for a subject and an object
        if (nodeValue.charAt(0) == 'V' && isPOSTag(currentNode)) {
            //If this verb is part of a chain of verbs directly dominating each other,
            //use the lowest verb in the chain as the verb
            verb = currentNode.firstChild().nodeString()+"/V";

            //look for parent VPs with NP siblings (the siblings are NP specifiers)
            Tree NPSpec = getNPSpecifier(tree, currentNode);

            //if an NP specifier is found, look for a subject group in it
            if (NPSpec != null) {
                String subjectGroup = getSubjectGroup(tree, NPSpec);
                if (subjectGroup.length() > 0) {
                    subject = subjectGroup.substring(0, subjectGroup.length() - 1) + "/S";
                }
            }

            //count the number of siblings eligible to contain objects

            //check the verb's first sibling

            /*
               if no subject has been found yet, and the first sibling is an NP,
               and there are multiple siblings eligible to contain objects,
               search the sibling for a subject group
             */

            //Otherwise, search the first sibling for an object group

            //If the first sibling did not contain an object group, search the following ones

            System.out.println(subject);
            System.out.println(verb);
            System.out.println(object);
            //make the actual templates
            if (subject.length() > 0) {
                String[] subjectVerbTemplate = {subject, verb};
                templates.add(subjectVerbTemplate);

                if (object.length() > 0) {
                    String[] subjectObjectTemplate = {subject, object};
                    templates.add(subjectObjectTemplate);

                    String[] subjectVerbObjectTemplate = {subject, verb, object};
                    templates.add(subjectVerbObjectTemplate);
                }
            }
            if (object.length() > 0) {
                String[] verbObjectTemplate = {verb, object};
                templates.add(verbObjectTemplate);
            }

        }

        //after searching this node, search the rest of the tree
        Tree[] children = currentNode.children();
        for (Tree child: children) {
            ArrayList<String[]> subTemplates = traverseForTemplates(tree, child);
            for (String[] subTemplate: subTemplates) {
                templates.add(subTemplate);
            }
        }
        return templates;
    }

    /*
        From a given point, search the tree for a specifier (ancestor's sibling) that is an NP.
        All ancestors below the NP that mark phrase types (i.e. "VP" for verb phrase, "NP" for noun phrase,
        "ADJP" for adjective phrase) must be VPs.
    */
    public static Tree getNPSpecifier(Tree tree, Tree startingNode) {
        //go up the tree
        Tree currentParent = startingNode.parent(tree);
        while (currentParent != null) {
            //check to see if the current ancestor marks a phrase type
            String parentLabel = currentParent.label().toString();
            //stop if it's not a VP phrase marker
            if (!isPhraseTypeMarker(currentParent) || !parentLabel.equals("VP")) {
                break;
            }

            //Check for the first NP sibling occurring before the highlighted VP
            Tree parentOfPossibleNP = currentParent.parent(tree);
            for (Tree sibling: parentOfPossibleNP.children()) {
                if (isPhraseTypeMarker(sibling) && sibling.label().toString().equals("NP")) {
                    return sibling;
                }
                else if (sibling.equals(currentParent)) {
                    break;
                }
            }
            currentParent = currentParent.parent(tree);
        }
        return null;
    }

    /*
        Obtains the subject group (the first continuous sequence of noun/pronoun entities) of a given node.
        Search the node's children (in reverse order) for the first group of nouns and pronouns.
        This is the subject.
     */
    public static String getSubjectGroup(Tree tree, Tree startingNode) {
        String subjectGroup = "";
        ArrayList<String> subjectPieces = new ArrayList<String>();

        System.out.println("Starting node: "+startingNode.label().toString());
        Tree[] children = startingNode.children();
        boolean collect = false;
        //check the node's children for a noun group
        for (int i = children.length - 1; i > -1; i--) { //scan in reverse order, but add in forward order
            Tree child = children[i];
            System.out.println("Child: "+child.value());
            String childValue = child.label().toString();

            //the subject may be in a nested child NP, in which case it is the first noun group in that NP
            if (isPhraseTypeMarker(child) && childValue.equals("NP")) {
                String childSubjectGroup = getSubjectGroup(tree, child);
                if (childSubjectGroup.length() > 0) {
                    return childSubjectGroup;
                }
            }

            if (isPOSTag(child) && (childValue.charAt(0) == 'N' || childValue.contains("PRP") || childValue.equals("EX"))) {
                collect = true;
                try {
                    subjectGroup += child.firstChild().value() + " ";
                }
                finally {
                }
            }
            //If nouns have been collected before and the current entry is not a noun, the noun group is over
            else if (collect) {
                break;
            }
        }

        return subjectGroup;
    }

    /*
        A node in a Tree object is a phrase type marker if both of the following conditions apply:
         a) its label doesn't end in a hyphen and a number
         b) it doesn't have a child whose label ends in a hyphen and a sequence of digits,

         OR:
         a) It has multiple children
    */
    public static boolean isPhraseTypeMarker(Tree node) {
        //check for multple children
        Tree[] children = node.children();
        if (children.length > 1) {
            return true;
        }

        //check to see if its label ends in a hyphen and a sequence of digits
        String nodeLabel = node.label().toString();
        if (nodeLabel.contains("-") && util.isAllNumeric(nodeLabel.substring(nodeLabel.indexOf("-")))) {
            return false;
        }

        //check to see if it has a child whose label ends in a hyphen and a sequence of digits
        for (Tree child: children) {
            String childNodeLabel = child.label().toString();
            if (childNodeLabel.contains("-") && util.isAllNumeric(childNodeLabel.substring(childNodeLabel.indexOf("-")))) {
                return false;
            }
        }

        return true;
    }

    /*
        A node in a Tree object is a POS tag if both of the following conditions apply:
        a) its label doesn't end in a hyphen and a number
        b) It has one child, and condition a) applies to it
     */
    public static boolean isPOSTag(Tree node) {
        Tree[] children = node.children();
        //if it has multiple children, it's not a POS tag
        if (children.length > 1) {
            return false;
        }

        //check to see if its label ends in a hyphen and a sequence of digits
        String nodeLabel = node.label().toString();
        if (nodeLabel.contains("-") && util.isAllNumeric(nodeLabel.substring(nodeLabel.indexOf("-")))) {
            return false;
        }

        //check to see if it has a child whose label ends in a hyphen and a sequence of digits
        for (Tree child: children) {
            String childNodeLabel = child.label().toString();
            if (childNodeLabel.contains("-") && util.isAllNumeric(childNodeLabel.substring(childNodeLabel.indexOf("-")+1))) {
                return true;
            }
        }

        return false;
    }

    /*
        From a root verb, get all possible phrase templates in which the verb has a direct noun or pronoun subject
        and a direct object (noun or pronoun).
     */
    public static ArrayList<String[]> getTemplatesNSubjDObj(SemanticGraph graph, IndexedWord vertex) {
        ArrayList<String[]> templates = new ArrayList<String[]>();
        String subject = "";
        String verb = vertex.originalText()+"/V";
        String object = "";
        for (Pair<GrammaticalRelation, IndexedWord> pair: graph.childPairs(vertex)) {
            //find subject
            if (pair.first().getShortName().equals("nsubj")) {
                subject = pair.second().originalText()+"/S";
            }
            //find object
            else if (pair.first().getShortName().equals("dobj")) {
                object = pair.second().originalText()+"/O";
            }

            //make all possible pairs from the given data
            if (subject.length() > 0) {
                String[] subjectVerbTemplate = {subject, verb};
                templates.add(subjectVerbTemplate);
                if (object.length() > 0) {
                    String[] subjectVerbObjectTemplate = {subject, verb, object};
                    templates.add(subjectVerbObjectTemplate);
                }
            }
            if (object.length() > 0) {
                String[] verbObjectTemplate = {verb, object};
                templates.add(verbObjectTemplate);
            }
        }

        return templates;
    }

    public static Hashtable<String, String[]> getTopics(String pathToTopicFile) throws IOException, FileNotFoundException{
        Hashtable<String, String[]> topics = new Hashtable<String, String[]>();
        String name;
        String[] data;

        File file = new File(pathToTopicFile);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        String currentLine = "";
        while ((currentLine = bufferedReader.readLine()) != null) {
            String[] lineSep = currentLine.split("\\t");
            name = lineSep[0];
            data = lineSep[2].split(" ");
            topics.put(name, data);
        }
        return topics;
    }

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

    public static int numericalReferencesCount(CoreLabel[][] phrases) throws IOException {
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
    public static int getFeatureForWordClass(CoreLabel[][] phrases, String relevantClassName) throws IOException {
        //initialize word classes
        if (wordClasses.size() == 0) initializeWordClasses(topicWordClassFilePath);

        //initialize
        int counter = 0;
        String[] relevantWordClass = new String[1];
        Enumeration<String> wordClassNames = wordClasses.keys();
        while (wordClassNames.hasMoreElements()) {
            String currentClassName = wordClassNames.nextElement();
            if (currentClassName.equals(relevantClassName)) {
                relevantWordClass = wordClasses.get(currentClassName);
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

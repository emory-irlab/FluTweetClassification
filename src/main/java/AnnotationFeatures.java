import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.semgraph.*;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.Pair;

import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

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
    private static String[] pastTenseWordClass = {"was", "did", "had", "got", "were", "V-ed"};
    private static String[] presentTenseWordClass = {"2it 's", "is", "am", "are", "have", "has", "V-ing"}; //'s as in "is"?
    private static String[] selfWordClass = {"2I 've", "2I 'd", "2I 'm", "im", "my", "me", "I"};
    private static String[] othersWordClass = {"2he 's", "2she 's", "2you 're", "2they 're", "2she 'll", "2he 'll", "your",
            "everyone", "you", "it", "its", "u", "her", "he", "she", "they", "husband", "wife", "brother",
            "sister", "people", "kid", "kids", "children", "son", "daughter", "his", "hers", "him", "he", "she", "they're", "she'll", 
            "he'll", "niece", "nephew"};
    private static String[] plural1PPronounsWordClass = {"we", "our", "ourselves", "ours", "us"};
    private static String[] _2PPronounsWordClass = {"2you 're", "2y 'all", "you", "your", "yours",
            "yall", "u", "ur", "yourself", "yourselves"};
    private static String[] followMeWordClass = {"follow", "tweet", "visit"};
    private static String[] numericalReferencesWordClass = {"2a couple", "2a lot", "many", "some", "all", "most",
            "lots", "none", "much", "few"};
    private static String[] orgAccountDescriptionsWordClass = {"official", "twitter", "account", "follow", "tweet", "us"};
    private static String[] personPuncuationWordClass = {",", "|", "&"};

    public static void initializeWordClasses(String pathToTopicFile) throws IOException {
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

//        //get topic word classes
//        Hashtable<String, String[]> topics = getTopics(pathToTopicFile);
//        Enumeration<String> topicNames = topics.keys();
//        while (topicNames.hasMoreElements()) {
//            String currentName = topicNames.nextElement();
//            topicWordClassNames.add(currentName);
//            wordClasses.put(currentName, topics.get(currentName));
//        }
    }
    
    
    public static String checkForNegs(SemanticGraph graph, IndexedWord vertex) {
        for (Pair<GrammaticalRelation, IndexedWord> pair : graph.childPairs(vertex)) {
            if (pair.first().getShortName().equals("neg")) {
                return "not ";
            }
        }
        return "";
    }

    public static int countAdjectives(CoreLabel[][] phrases) {
        int count = 0;

        for (CoreLabel[] phrase: phrases) {
            if (phrase[0].tag().equals("JJ")) count++;
        }

        return count;
    }

    public static int countAdverbs(CoreLabel[][] phrases) {
        int count = 0;

        for (CoreLabel[] phrase: phrases) {
            if (phrase[0].tag().equals("RB")) count++;
        }
        return count;
    }

    public static int countPersonalPronouns(CoreLabel[][] phrases) {
        int count = 0;

        for (CoreLabel[] phrase: phrases) {
            if (phrase[0].tag().equals("PRP")) count++;
        }

        return count;
    }

    public static int countProperNouns(CoreLabel[][] phrases) {
        int count = 0;

        for (CoreLabel[] phrase: phrases) {
            if (phrase[0].tag().equals("NNP") || phrase[0].tag().equals("NNPS")) count++;
        }

        return count;
    }

    public static int countPluralProperNouns(CoreLabel[][] phrases) {
        int count = 0;

        for (CoreLabel[] phrase: phrases) {
            if (phrase[0].tag().equals("NNPS")) count++;
        }

        return count;
    }

    public static int countSingularProperNouns(CoreLabel[][] phrases) {
        int count = 0;

        for (CoreLabel[] phrase: phrases) {
            if (phrase[0].tag().equals("NNP")) count++;
        }

        return count;
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
	        System.err.println("ERROR: Word class requested, " + relevantClassName + ", does not exist.");//change to exception
	        System.exit(1);
	    }
	    //go over each phrase
	    for (CoreLabel[] phrase : phrases) {
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
	                int possibleNum = (int) stringToMatch.charAt(0) - '0';
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
    
    public static ArrayList<String> getPhraseTemplates(List<CoreMap> sentences) {
        //System.out.println();
        //System.out.println("New tweet: ");

        ArrayList<String[]> phraseTemplates = new ArrayList<String[]>();
        //look through each sentence
        for (CoreMap sentence : sentences) {
            //System.out.println();
            //System.out.println("New sentence: ");
            SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);

            //check for all types of subject-verb-object templates
            for (IndexedWord vertex : graph.vertexListSorted()) {

                //for verbs
                if (vertex.tag().charAt(0) == 'V') {
                    //Simple case: verb has an nsubj (direct noun subject) and a dobj (direct object)
                    ArrayList<String[]> templatesVerbs = getTemplatesForVerbs(graph, vertex);
                    for (String[] template : templatesVerbs) phraseTemplates.add(template);
                }

                //for nouns
                if (vertex.tag().charAt(0) == 'N') {
                    ArrayList<String[]> templatesNouns = getTemplatesForNouns(graph, vertex);
                    for (String[] template : templatesNouns) phraseTemplates.add(template);
                }

                //System.out.println(vertex.originalText() + "/" + vertex.tag());
                if (vertex.tag().charAt(0) == 'V' || vertex.tag().charAt(0) == 'N') {
                    for (Pair<GrammaticalRelation, IndexedWord> pair : graph.childPairs(vertex)) {
                        //System.out.println("Child: "+pair.second().originalText()+"/"+pair.second().tag()+", Relation: "+pair.first().getShortName()+" "+pair.first().getLongName());
                    }

                    for (Pair<GrammaticalRelation, IndexedWord> pair : graph.parentPairs(vertex)) {
                        //System.out.println("Parent: "+pair.second().originalText()+"/"+pair.second().tag()+", Relation: "+pair.first().getShortName()+" "+pair.first().getLongName());
                    }
                }
            }

            //full parse version
            //Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
            //ArrayList<String[]> templates = traverseForTemplates(tree, tree); //add to phraseTemplates

            //tree.pennPrint();
        }

        ArrayList<String> compressedPhraseTemplates = new ArrayList<String>();
        //each phrase template is stored as an array. Instead, store it as a String
        for (String[] template : phraseTemplates) {
            String compressedTemplate = "(";
            for (int i = 0; i < template.length; i++) {
                String entry = template[i];
                compressedTemplate += entry;
                if (i < template.length - 1) {
                    compressedTemplate += ", ";
                }
            }
            compressedTemplate += ")";
            compressedPhraseTemplates.add(compressedTemplate);
        }

        return compressedPhraseTemplates;
    }
    
    public static ArrayList<String[]> getTemplatesForNouns(SemanticGraph graph, IndexedWord vertex) {
        ArrayList<String> subjects = new ArrayList<String>();

        //String subject = "";
        String verb = "";
        String object = util.lowerCaseTextUnlessProperNoun(vertex) + "/O";
        ArrayList<String[]> phraseTemplates = new ArrayList<String[]>();

        for (Pair<GrammaticalRelation, IndexedWord> pair : graph.childPairs(vertex)) {
            //get the copula if it's there
            if (pair.first().getShortName().equals("cop")) {
                verb = checkForNegs(graph, vertex) + util.lowerCaseTextUnlessProperNoun(pair.second()) + "/V";
            }

            //get the nsubj if it's there
            if (pair.first().getShortName().equals("nsubj")) {
                subjects.add(util.lowerCaseTextUnlessProperNoun(pair.second()) + "/S");
                //subject = util.lowerCaseTextUnlessProperNoun(pair.second())+"/S";
            }

        }

        //construct the templates from the data
        if (verb.length() > 0) {
            String[] verbObjectTemplate = {verb, object};
            phraseTemplates.add(verbObjectTemplate);

            //System.out.println(verb+", "+object);

            //if (subject.length() > 0) {
            for (String subject : subjects) {
                String[] subjectObjectTemplate = {subject, object};
                phraseTemplates.add(subjectObjectTemplate);

                String[] subjectVerbTemplate = {subject, verb};
                phraseTemplates.add(subjectVerbTemplate);

                String[] subjectVerbObjectTemplate = {subject, verb, object};
                phraseTemplates.add(subjectVerbObjectTemplate);

                //test
                //System.out.println(subject+", "+verb+", "+object);
            }
        }

        return phraseTemplates;
    }
    
    public static ArrayList<String[]> getTemplatesForVerbs(SemanticGraph graph, IndexedWord vertex) {
        ArrayList<String[]> templates = new ArrayList<String[]>();
        ArrayList<String> subjects = new ArrayList<String>();
        ArrayList<String> objects = new ArrayList<String>();

        //String subject = "";
        String verb = checkForNegs(graph, vertex) + util.lowerCaseTextUnlessProperNoun(vertex) + "/V";
        //String object = "";
        for (Pair<GrammaticalRelation, IndexedWord> pair : graph.childPairs(vertex)) {
            //find subject
            if (pair.first().getShortName().contains("nsubj")) { //use .contains() so it collects nsubj and nsubjpass
                //subject = util.lowerCaseTextUnlessProperNoun(pair.second()) + "/S";
                subjects.add(util.lowerCaseTextUnlessProperNoun(pair.second()) + "/S");
            }
            //find direct object
            else if (pair.first().getShortName().equals("dobj")) {
                //object = util.lowerCaseTextUnlessProperNoun(pair.second()) + "/O";
                objects.add(util.lowerCaseTextUnlessProperNoun(pair.second()) + "/O");
            }
            //find complementing verb/adjective object if no direct object has been found
            else if (objects.size() == 0 && pair.first().getShortName().equals("xcomp") &&
                    (pair.second().tag().charAt(0) == 'V' || pair.second().tag().charAt(0) == 'J')) {
                //object = util.lowerCaseTextUnlessProperNoun(pair.second()) + "/O";
                objects.add(util.lowerCaseTextUnlessProperNoun(pair.second()) + "/O");
            }
        }

        //if the verb has no nsubj and it has an xclausal complement verb as its parent,
        //use the parent verb's subject as the subject
        //if (subject.length() == 0) {
        if (subjects.size() == 0) {
            for (Pair<GrammaticalRelation, IndexedWord> pair : graph.parentPairs(vertex)) {
                if (pair.first().getShortName().equals("xcomp") && pair.second().tag().charAt(0) == 'V') {
                    //if a suitable parent is found, get all of its phrase templates and check for the subject
                    ArrayList<String[]> parentVerbTemplates = getTemplatesForVerbs(graph, pair.second());
                    for (String[] template : parentVerbTemplates) {
                        //if the first entry ends in "/S", that entry is the subject
                        if (template[0].substring(template[0].length() - 2).equals("/S")) {
                            subjects.add(template[0]);
                            break;
                        }
                    }
                }
            }
        }

        //make all possible templates from the given data
        //if (subject.length() > 0) {
        for (String subject : subjects) {
            String[] subjectVerbTemplate = {subject, verb};
            templates.add(subjectVerbTemplate);

            //test
            //System.out.println(subject+", "+verb);

            //if (object.length() > 0) {
            for (String object : objects) {
                String[] subjectVerbObjectTemplate = {subject, verb, object};
                templates.add(subjectVerbObjectTemplate);

                String[] subjectObjectTemplate = {subject, object};
                templates.add(subjectObjectTemplate);

                //test
                //System.out.println(subject+", "+verb+", "+object);
            }
        }
        //if (object.length() > 0) {
        for (String object : objects) {
            String[] verbObjectTemplate = {verb, object};
            templates.add(verbObjectTemplate);

            //test
            //System.out.println(verb+", "+object);
        }

        return templates;
    }
    
    public static Hashtable<String, String[]> getTopics(String pathToTopicFile) throws IOException, FileNotFoundException {
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
        bufferedReader.close();
        return topics;
    }
    
    public static int countWordsInClassOther(String[] wordsToCheck) {
    	int count = 0;

        for (String s : wordsToCheck) {
            for (String wordInOthersClass: othersWordClass) {
                if (s.equals(wordInOthersClass)) {
                    count++;
                }
            }
        }
        return count;
    }
    
    public static boolean isInWordClassOther(String wordToCheck) {

        for (String wordInOthersClass: othersWordClass) {
        	if (wordToCheck.equals(wordInOthersClass)) {
        		return true;
            }
        }
        return false;
    }
    
    
    
    public static int countWordsInClassSelf(String[] wordsToCheck) {
    	int count = 0;

        for (String s : wordsToCheck) {
            for (String wordInSelfClass: selfWordClass) {
                if (s.equals(wordInSelfClass)) {
                    count++;
                }
            }
        }
        return count;
    }
    
    public static boolean isInWordClassSelf(String wordToCheck) {
    	
        for (String wordInSelfClass: selfWordClass) {
        	if (wordToCheck.equals(wordInSelfClass)) {
        		return true;
            }
        }
        return false;
    }
    
    public static int numericalReferencesCount(CoreLabel[][] phrases) throws IOException {
        int counter = 0;
        for (CoreLabel[] phrase : phrases) {
            for (CoreLabel token : phrase) if (token.tag().equals("CD")) counter++;
        }
        counter += getFeatureForWordClass(phrases, numericalReferencesWordClassName);
        return counter;
    }
    
    public static String[] pairFirstPronounLastNoun(CoreLabel[][] phrases) {
    	
    	String[] pronounNounPair = new String[2];
    	boolean firstPronoun = false;
    	
    	for (CoreLabel[] phrase: phrases) {
    		
    		String tag = phrase[0].tag();
            
    		if ((tag.equals("PRP") || tag.equals("PRP$")) && !firstPronoun) {
            	pronounNounPair[0] = phrase[0].word();
            	firstPronoun = true;
            }
            
            if (tag.equals("NN") || tag.equals("NNS") ) {
            	pronounNounPair[1] = phrase[0].word();
            }
        }
    	
    	return pronounNounPair;
    }
    
    public static String[] pairFirstPronounOrNounLastVerb(CoreLabel[][] phrases) {
    	
    	String[] pronounNounVerbPair = new String[2];
    	boolean firstNounPronoun = false;
    	
    	for (CoreLabel[] phrase: phrases) {
    		
    		String tag = phrase[0].tag();
            
    		if ((tag.equals("PRP") || tag.equals("PRP$")) || tag.equals("NN") || tag.equals("NNS") && !firstNounPronoun) {
            	pronounNounVerbPair[0] = phrase[0].word();
            	firstNounPronoun = true;
            }
            
            if (tag.equals("VB") || tag.equals("VBP") || tag.equals("VBG") || tag.equals("VBN")) {
            	pronounNounVerbPair[1] = phrase[0].word();
            }
        }
    	
    	return pronounNounVerbPair;
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
    
    public static int properNounsFollowedByVerb(CoreLabel[][] phrases) {
    	int count = 0;
    	
    	for (int i = 0; i < phrases.length - 1; i++) {
    		
    		CoreLabel[] phrase = phrases[i];
    		
    		String currentTag = phrase[0].tag();
    		if (currentTag.equals("NNP") || currentTag.equals("NNPS")) {
    			
    			CoreLabel[] phrase2 = phrases[i+1];
    			String nextTag = phrase2[0].tag();
    			if (nextTag.equals("VB") || nextTag.equals("VBP") || nextTag.equals("VBG") || nextTag.equals("VBN")) {
    				count++;
    			}
    		}
    	}
    	
    	return count;
    }

    public static int verbsCount(CoreLabel[][] phrases) {
        int counter = 0;
        for (CoreLabel[] phrase : phrases) {
            for (CoreLabel token : phrase) if (token.tag().charAt(0) == 'V') counter++;
        }
        return counter;
	}
    
    public static String[] createSubjectVerbObjectTuple(CoreLabel[][] phrases) {
    	
    	String[] subjectVerbObject = new String[3];
    	boolean subject = false;
    	boolean verb = false;
    	
    	for (int i = 0; i < phrases.length; i++) {
    		
    		CoreLabel[] phrase = phrases[i];
    		String tag = phrase[0].tag();
    		
    		if ((tag.equals("NN") || tag.equals("NNP") || tag.equals("NNS") || tag.equals("NNPS")) && !subject) {
    			subjectVerbObject[0] = phrase[0].word();
            	subject = true;
        	}
    		
    		//Finds first noun (proper included) in tweet
    		//resetForVerb is due to the circumstance that the object may come before a verb
    		else if ((tag.equals("VB") || tag.equals("VBP") || tag.equals("VBG") || tag.equals("VBN")) && !verb) {
        		subjectVerbObject[1] = phrase[0].word();
        		verb = true;
        	}
    		
    		//Finds following first noun or pronoun (plural included) object after first pronoun/noun has been found
    		//Accounts for if object comes before verb by resetting i to index+1 at which the first
    		//noun/pronoun was located
    		else if ((tag.equals("NN") || tag.equals("NNP") || tag.equals("NNS") || tag.equals("NNPS")) && subject) {
    			
    			String prevTag = phrases[i-1][0].tag();
    			if (prevTag.equals("VB") || prevTag.equals("VBP") || prevTag.equals("VBG") || prevTag.equals("VBN") || i == phrases.length-1) {
        			subjectVerbObject[2] = phrase[0].word();
        		}
    		}
    	}
    	return subjectVerbObject;
    }
}
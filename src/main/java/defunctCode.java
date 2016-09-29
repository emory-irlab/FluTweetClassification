/**
 * @deprecated
 * Created by Alec Wolyniec on 8/3/16.
 */
@Deprecated
public class defunctCode {
        /*
        Traverse the tree, extracting all subject-verb-object phrase templates
    *
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
            verb = currentNode.firstChild().nodeString() + "/V";

            //look for parent VPs with NP siblings (the siblings are NP specifiers)
            Tree NPSpec = getNPSpecifier(tree, currentNode);

            //if an NP specifier is found, look for a subject group in it
            if (NPSpec != null) {
                String subjectGroup = getSubjectGroup(tree, NPSpec);
                if (subjectGroup.length() > 0) {
                    subject = subjectGroup + "/S";
                }
            }

            //count the number of siblings eligible to contain objects

            //check the verb's first sibling

            /*
               if no subject has been found yet, and the first sibling is an NP,
               and there are multiple siblings eligible to contain objects,
               search the sibling for a subject group
             *

            //Otherwise, search the first sibling for an object group

            //If the first sibling did not contain an object group, search the following ones

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
        for (Tree child : children) {
            ArrayList<String[]> subTemplates = traverseForTemplates(tree, child);
            for (String[] subTemplate : subTemplates) {
                templates.add(subTemplate);
            }
        }
        return templates;
    }

    /*
        From a given point, search the tree for a specifier (ancestor's sibling) that is an NP.
        All ancestors below the NP that mark phrase types (i.e. "VP" for verb phrase, "NP" for noun phrase,
        "ADJP" for adjective phrase) must be VPs.
    *
    public static Tree getNPSpecifier(Tree tree, Tree startingNode) {
        //go up the tree
        Tree currentParent = startingNode.parent(tree);
        while (currentParent != null) {
            //check to see if the current ancestor marks a phrase
            String parentLabel = currentParent.label().toString();
            //stop if it's not a VP phrase marker
            if (!isPhraseTypeMarker(currentParent) || !parentLabel.equals("VP")) {
                break;
            }

            //Check for an NP sibling occurring right before the highlighted VP
            Tree parentOfPossibleNP = currentParent.parent(tree);
            Tree possibleNP = null;
            //look through all the VP's siblings
            for (Tree sibling : parentOfPossibleNP.children()) {
                //if an NP is found, mark it
                if (isPhraseTypeMarker(sibling) && sibling.label().toString().equals("NP")) {
                    possibleNP = sibling;
                    continue;
                }
                //if the previously followed NP is followed by the highlighted VP,
                //it is the specifier. If it is not, keep looking
                if (possibleNP != null) {
                    if (sibling.equals(currentParent)) {
                        return possibleNP;
                    } else {
                        possibleNP = null;
                    }
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
     *
    public static String getSubjectGroup(Tree tree, Tree startingNode) {
        String subjectGroup = "";
        ArrayList<String> subjectPieces = new ArrayList<String>();

        System.out.println("Starting node: " + startingNode.label().toString());
        Tree[] children = startingNode.children();
        boolean collect = false;
        //check the node's children for a noun group
        for (int i = children.length - 1; i > -1; i--) {
            Tree child = children[i];
            System.out.println("Child: " + child.value());
            String childValue = child.label().toString();

            //the subject may be in a nested child NP, in which case it is the first noun group in that NP
            if (isPhraseTypeMarker(child) && childValue.equals("NP")) {
                String childSubjectGroup = getSubjectGroup(tree, child);
                if (childSubjectGroup.length() > 0) {
                    return childSubjectGroup;
                }
            }

            //if one of the node's children is a noun, preposition, or existential "there", begin collecting the noun group
            if (isPOSTag(child) && (childValue.charAt(0) == 'N' || childValue.contains("PRP") || childValue.equals("EX"))) {
                collect = true;
                try {
                    subjectPieces.add(child.firstChild().value());
                } finally {
                }
            }
            //If nouns have been collected before and the current entry is not a noun, the noun group is over
            else if (collect) {
                break;
            }
        }

        //add the subject pieces in the reverse of the order they were collected in (since they were collected in
        //reverse order)
        for (int i = subjectPieces.size() - 1; i > -1; i--) {
            subjectGroup += subjectPieces.get(i);
            if (i != 0) {
                subjectGroup += " ";
            }
        }

        return subjectGroup;
    }
    */

    /*
        A node in a Tree object is a phrase type marker if both of the following conditions apply:
         a) its label doesn't end in a hyphen and a number
         b) it doesn't have a child whose label ends in a hyphen and a sequence of digits,
         OR:
         a) It has multiple children
    *
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
        for (Tree child : children) {
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
     *
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
        for (Tree child : children) {
            String childNodeLabel = child.label().toString();
            if (childNodeLabel.contains("-") && util.isAllNumeric(childNodeLabel.substring(childNodeLabel.indexOf("-") + 1))) {
                return true;
            }
        }

        return false;
    }
    */

        /*
        From 5 training tweets, collect each word's idf (number of documents / word's number of occurrences within the
        corpus) and check if the idf values are correct
            Expected idf values:
            -this - 5
            -be - 2.5
            -a - 5/3
            -test - 2.5
            -for - 5
            -Speakonia - 5
            -Mr. - 5
            -Bingo - 5
            -you - 2.5
            -will - 2.5
            -fail - 5/3
            -the - 5
            -i - 5
            -can - 2.5
            -Disneyland - 5
            -steal - 5
            -that - 5
            -my - 5
            -dear - 5
            -not - 2.5
            -ideal - 5
            -or - 5
            -it - 5.0
            - -lrb- - 5
            - -rrb- - 5
            -Can - 5
    */
    /*
    public static void testUnigramFeaturesIDFThresholdOf1WithStopWords() throws IOException {
        //---------------
        //TRAINING
        //---------------
        //set up vars
        ArrayList<String[]> trainingTweets = TweetParser.getTweets("data/testData/5ExampleTweetTexts.txt");
        ArrayList<String> trainingLabelSet = new ArrayList<String>();
        TweetVector[] trainingTweetVectors = new TweetVector[trainingTweets.size()];

        //train the model on the training data
        for (int i = 0; i < trainingTweets.size(); i++) {
            String[] tweet = trainingTweets.get(i);
            trainingTweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5], trainingLabelSet);
        }
        NGramModel unigramModel = new NGramModel(1, trainingTweetVectors, NGramModel.textName, "", 1, 1);

        //--------------
        //TESTING
        //--------------

        System.out.println("Testing IDF values with a frequency threshold of 1 and stop words...");
        try {
            Hashtable<String, Double> idfs = unigramModel.getTweetIDFs();

            //check the idfs
            if (idfs.get("this") != 5.0 ||
                    idfs.get("be") != 2.5 ||
                    idfs.get("a") != ((double) 5) / 3 ||
                    idfs.get("test") != 2.5 ||
                    idfs.get("for") != 5.0 ||
                    idfs.get("Speakonia") != 5.0 ||
                    idfs.get("Mr.") != 5.0 ||
                    idfs.get("Bingo") != 5.0 ||
                    idfs.get("you") != 2.5 ||
                    idfs.get("will") != 2.5 ||
                    idfs.get("fail") != ((double) 5) / 3 ||
                    idfs.get("the") != 5.0 ||
                    idfs.get("i") != 5.0 ||
                    idfs.get("can") != 2.5 ||
                    idfs.get("Disneyland") != 5.0 ||
                    idfs.get("steal") != 5.0 ||
                    idfs.get("that") != 5.0 ||
                    idfs.get("my") != 5.0 ||
                    idfs.get("dear") != 5.0 ||
                    idfs.get("not") != 2.5 ||
                    idfs.get("ideal") != 5.0 ||
                    idfs.get("or") != 5.0 ||
                    idfs.get("it") != 5.0 ||
                    idfs.get("-lrb-") != 5 ||
                    idfs.get("-rrb-") != 5 ||
                    idfs.get("Can") != 5) {
                System.out.println("FAILED due to improper idf value(s) for word(s)");
            } else if (unigramModel.getNumIDFs() != 26) {
                System.out.println("FAILED due to superfluous idfs");
            }
            else {
                System.out.println("PASSED.");
            }
        }
        catch (Exception e) {
            System.out.println("FAILED due to exception");
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }
    */

    /*
    Assuming a working TweetVector model and a working Stanford CoreNLP model, tests to see if the unigram model
    present here will yield the correct tf-idf values for words in 10 example tweets (5 training and 5 testing),
    including stop words. The unigram model must also represent all words in lowercase unless they are proper
    nouns, in which case the first letter (and only the first letter) is capitalized.

    Expected tf-idf values:
    First tweet:
        -test - 2.5
        -be - 2.5

    Second tweet:
        -a - 10/3
        -test - 5
        -be - 2.5

    Third:
        -Can - 5
        -you - 5
        -can - 2.5
        -i - 5

    Fourth:
        -be - 2.5
        -you - 2.5
        -Disneyland - 5

    Fifth:

 */
    /*
    public static void testUnigramFeaturesTFIDFThresholdOf1WithStopWords() throws IOException {
        //---------------
        //TRAINING
        //---------------
        //set up vars
        ArrayList<String[]> trainingTweets = TweetParser.getTweets("data/testData/5ExampleTweetTexts.txt");
        ArrayList<String> trainingLabelSet = new ArrayList<String>();
        TweetVector[] trainingTweetVectors = new TweetVector[trainingTweets.size()];

        //train the model on the training data
        for (int i = 0; i < trainingTweets.size(); i++) {
            String[] tweet = trainingTweets.get(i);
            trainingTweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5], trainingLabelSet);
        }
        NGramModel unigramModel = new NGramModel(1, trainingTweetVectors, NGramModel.textName, "", 1, 1);


        //--------------
        //TESTING
        //--------------

        //feature var
        ArrayList<Hashtable<String, Double>> tfIdfFeatures = new ArrayList<Hashtable<String, Double>>();
        //annotator
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //get the test data into phrase form so tf features can be extracted
        ArrayList<String[]> testingTweets = TweetParser.getTweets("data/testData/5MoreExampleTweetTexts.txt");

        //get the tfs
        for (String[] tweet: testingTweets) {
            Annotation tweetDocument = new Annotation(TweetFeatureExtractor.process(tweet[4]));
            pipeline.annotate(tweetDocument);
            CoreLabel[][] tweetPhrases = TweetFeatureExtractor.getPhrases(tweetDocument);

            Hashtable<String, Double> tfIdfForTweet = unigramModel.getFeaturesForTweetTFIDF(tweetPhrases);
            tfIdfFeatures.add(tfIdfForTweet);
        }

        System.out.println("Testing TF-IDF unigram features with a document frequency threshold of 1 and stop words...");
        try {
            //generate vars
            Hashtable<String, Double> firstTweet = tfIdfFeatures.get(0);
            Hashtable<String, Double> secondTweet = tfIdfFeatures.get(1);
            Hashtable<String, Double> thirdTweet = tfIdfFeatures.get(2);
            Hashtable<String, Double> fourthTweet = tfIdfFeatures.get(3);
            Hashtable<String, Double> fifthTweet = tfIdfFeatures.get(4);

            //check the tf-idf values
            //tweet 1
            if (firstTweet.get("test-TFIDF") != 2.5 ||
                    firstTweet.get("be-TFIDF") != 2.5) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.size() != 2) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 2
            else if (secondTweet.get("a-TFIDF") != ((double)10)/3 ||
                    secondTweet.get("test-TFIDF") != 5.0 ||
                    secondTweet.get("be-TFIDF") != 2.5) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.size() != 3) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 3
            else if (thirdTweet.get("can-TFIDF") != 2.5 ||
                    thirdTweet.get("you-TFIDF") != 5.0 ||
                    thirdTweet.get("Can-TFIDF") != 5.0 ||
                    thirdTweet.get("i-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (thirdTweet.size() != 4) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 4
            else if (fourthTweet.get("be-TFIDF") != 2.5 ||
                    fourthTweet.get("you-TFIDF") != 2.5 ||
                    fourthTweet.get("Disneyland-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (fourthTweet.size() != 3) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 5
            else if (fifthTweet.size() != 0) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            else {
                System.out.println("PASSED.");
            }
        }
        catch (Exception e) {
            System.out.println("FAILED due to exception.");
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }
    */

    /*
        From 5 training tweets, test to see if tf-idf values are correct when only words that appear at least 2 times
        in the training tweets are considered.

        Expected tf-idf values:
        First tweet:
            -test - 2.5
            -be - 2.5

        Second tweet:
            -a - 10/3
            -test - 5
            -be - 2.5

        Third:
            -you - 5
            -can - 2.5

        Fourth:
            -be - 2.5
            -you - 2.5

        Fifth:

     */
    /*
    public static void testUnigramFeaturesTFIDFThresholdOf2WithStopWords() throws IOException {
        //---------------
        //TRAINING
        //---------------
        //set up vars
        ArrayList<String[]> trainingTweets = TweetParser.getTweets("data/testData/5ExampleTweetTexts.txt");
        ArrayList<String> trainingLabelSet = new ArrayList<String>();
        TweetVector[] trainingTweetVectors = new TweetVector[trainingTweets.size()];

        //train the model on the training data
        for (int i = 0; i < trainingTweets.size(); i++) {
            String[] tweet = trainingTweets.get(i);
            trainingTweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5], trainingLabelSet);
        }
        NGramModel unigramModel = new NGramModel(1, trainingTweetVectors, NGramModel.textName, "", 2, 1);


        //--------------
        //TESTING
        //--------------

        //feature var
        ArrayList<Hashtable<String, Double>> tfIdfFeatures = new ArrayList<Hashtable<String, Double>>();
        //annotator
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //get the test data into phrase form so tf features can be extracted
        ArrayList<String[]> testingTweets = TweetParser.getTweets("data/testData/5MoreExampleTweetTexts.txt");

        //get the tfs
        for (String[] tweet: testingTweets) {
            Annotation tweetDocument = new Annotation(TweetFeatureExtractor.process(tweet[4]));
            pipeline.annotate(tweetDocument);
            CoreLabel[][] tweetPhrases = TweetFeatureExtractor.getPhrases(tweetDocument);

            Hashtable<String, Double> tfIdfForTweet = unigramModel.getFeaturesForTweetTFIDF(tweetPhrases);
            tfIdfFeatures.add(tfIdfForTweet);
        }

        System.out.println("Testing TF-IDF unigram features with a document frequency threshold of 2 and stop words...");
        try {
            //generate vars
            Hashtable<String, Double> firstTweet = tfIdfFeatures.get(0);
            Hashtable<String, Double> secondTweet = tfIdfFeatures.get(1);
            Hashtable<String, Double> thirdTweet = tfIdfFeatures.get(2);
            Hashtable<String, Double> fourthTweet = tfIdfFeatures.get(3);
            Hashtable<String, Double> fifthTweet = tfIdfFeatures.get(4);

            //check the tf-idf values
            //tweet 1
            if (firstTweet.get("test-TFIDF") != 2.5 ||
                    firstTweet.get("be-TFIDF") != 2.5) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.size() != 2) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 2
            else if (secondTweet.get("a-TFIDF") != ((double)10)/3 ||
                    secondTweet.get("test-TFIDF") != 5.0 ||
                    secondTweet.get("be-TFIDF") != 2.5) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.size() != 3) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 3
            else if (thirdTweet.get("can-TFIDF") != 2.5 ||
                    thirdTweet.get("you-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (thirdTweet.size() != 2) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 4
            else if (fourthTweet.get("be-TFIDF") != 2.5 ||
                    fourthTweet.get("you-TFIDF") != 2.5) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (fourthTweet.size() != 2) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 5
            else if (fifthTweet.size() != 0) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            else {
                System.out.println("PASSED.");
            }
        }
        catch (Exception e) {
            System.out.println("FAILED due to exception.");
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }
    */

    /*
        Test to see if tf-idf values remain correct even if stopwords are removed

        Expected tf-idf values:
        First tweet:
            -test - 2.5

        Second tweet:
            -test - 5

        Third:
            -Can - 5.0

        Fourth:
            -Disneyland - 5.0

        Fifth:

     */
    /*
    public static void testUnigramFeaturesTFIDFThresholdOf1NoStopWords() throws IOException {
        //---------------
        //TRAINING
        //---------------
        //set up vars
        ArrayList<String[]> trainingTweets = TweetParser.getTweets("data/testData/5ExampleTweetTexts.txt");
        ArrayList<String> trainingLabelSet = new ArrayList<String>();
        TweetVector[] trainingTweetVectors = new TweetVector[trainingTweets.size()];

        //train the model on the training data
        for (int i = 0; i < trainingTweets.size(); i++) {
            String[] tweet = trainingTweets.get(i);
            trainingTweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5], trainingLabelSet);
        }
        NGramModel unigramModel = new NGramModel(1, trainingTweetVectors, NGramModel.textName, "data/stopwords.txt", 1, 1);


        //--------------
        //TESTING
        //--------------
        //feature var
        ArrayList<Hashtable<String, Double>> tfIdfFeatures = new ArrayList<Hashtable<String, Double>>();
        //annotator
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //get the test data into phrase form so tf features can be extracted
        ArrayList<String[]> testingTweets = TweetParser.getTweets("data/testData/5MoreExampleTweetTexts.txt");

        //get the tfs
        for (String[] tweet: testingTweets) {
            Annotation tweetDocument = new Annotation(TweetFeatureExtractor.process(tweet[4]));
            pipeline.annotate(tweetDocument);
            CoreLabel[][] tweetPhrases = TweetFeatureExtractor.getPhrases(tweetDocument);

            Hashtable<String, Double> tfIdfForTweet = unigramModel.getFeaturesForTweetTFIDF(tweetPhrases);
            tfIdfFeatures.add(tfIdfForTweet);
        }

        System.out.println("Testing TF-IDF unigram features with a document frequency threshold of 1 and no stop words...");
        try {
            //generate vars
            Hashtable<String, Double> firstTweet = tfIdfFeatures.get(0);
            Hashtable<String, Double> secondTweet = tfIdfFeatures.get(1);
            Hashtable<String, Double> thirdTweet = tfIdfFeatures.get(2);
            Hashtable<String, Double> fourthTweet = tfIdfFeatures.get(3);
            Hashtable<String, Double> fifthTweet = tfIdfFeatures.get(4);

            //check the tf-idf values
            //tweet 1
            if (firstTweet.get("test-TFIDF") != 2.5) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.size() != 1) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 2
            else if (secondTweet.get("test-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.size() != 1) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 3
            else if (thirdTweet.get("Can-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (thirdTweet.size() != 1) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 4
            else if (fourthTweet.get("Disneyland-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (fourthTweet.size() != 1) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 5
            else if (fifthTweet.size() != 0) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            else {
                System.out.println("PASSED.");
            }
        }
        catch (Exception e) {
            System.out.println("FAILED due to exception.");
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }
    */

    /*
        From 5 training and 5 test tweets, extract tf-idf features on bigrams and check to make sure they're correct.

        Expected values:
        First tweet:
        -observe chicken - 2.5
        -table -post- - 5.0
        -chicken soup - 5.0
        -soup table - 5.0
        - -pre- observe - 5.0

        Second tweet:
        -Mr. Bingley - 5.0

        Third tweet:

        Fourth tweet:
        -table -post- - 5.0

        Fifth tweet:
        - -pre- marshmallow - 5.0

     */
    /*
    public static void testBigramFeaturesTFIDFThresholdOf1NoStopWords() throws IOException {
        //---------------
        //TRAINING
        //---------------
        //set up vars
        ArrayList<String[]> trainingTweets = TweetParser.getTweets("data/testData/5BigramExampleTweets.txt");
        ArrayList<String> trainingLabelSet = new ArrayList<String>();
        TweetVector[] trainingTweetVectors = new TweetVector[trainingTweets.size()];

        //train the model on the training data
        for (int i = 0; i < trainingTweets.size(); i++) {
            String[] tweet = trainingTweets.get(i);
            trainingTweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5], trainingLabelSet);
        }
        NGramModel bigramModel = new NGramModel(2, trainingTweetVectors, NGramModel.textName, "data/stopwords.txt", 1, 1);


        //--------------
        //TESTING
        //--------------

        //feature var
        ArrayList<Hashtable<String, Double>> tfIdfFeatures = new ArrayList<Hashtable<String, Double>>();
        //annotator
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //get the test data into phrase form so tf features can be extracted
        ArrayList<String[]> testingTweets = TweetParser.getTweets("data/testData/5MoreBigramExampleTweets.txt");

        //get the tfs
        for (String[] tweet: testingTweets) {
            Annotation tweetDocument = new Annotation(TweetFeatureExtractor.process(tweet[4]));
            pipeline.annotate(tweetDocument);
            CoreLabel[][] tweetPhrases = TweetFeatureExtractor.getPhrases(tweetDocument);

            Hashtable<String, Double> tfIdfForTweet = bigramModel.getFeaturesForTweetTFIDF(tweetPhrases);
            tfIdfFeatures.add(tfIdfForTweet);
        }

        System.out.println("Testing TF-IDF bigram features with a document frequency threshold of 1 and no stop words...");
        try {
            //generate vars
            Hashtable<String, Double> firstTweet = tfIdfFeatures.get(0);
            Hashtable<String, Double> secondTweet = tfIdfFeatures.get(1);
            Hashtable<String, Double> thirdTweet = tfIdfFeatures.get(2);
            Hashtable<String, Double> fourthTweet = tfIdfFeatures.get(3);
            Hashtable<String, Double> fifthTweet = tfIdfFeatures.get(4);

            //check the tf-idf values
            //tweet 1
            if (firstTweet.get("observe chicken-TFIDF") != 2.5) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("table -post--TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("chicken soup-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("soup table-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("-pre- observe-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.size() != 5) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 2
            else if (secondTweet.get("Mr. Bingley-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.size() != 1) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 3
            else if (thirdTweet.size() != 0) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 4
            else if (fourthTweet.get("table -post--TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (fourthTweet.size() != 1) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 5
            else if (fifthTweet.get("-pre- marshmallow-TFIDF") != 5.0) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }
            else if (fifthTweet.size() != 1) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            else {
                System.out.println("PASSED.");
            }
        }
        catch (Exception e) {
            System.out.println("FAILED due to exception.");
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }
    */

    /*
    public static void test6gramFeaturesTFIDFThresholdOf1NoStopWords() throws IOException {
        //---------------
        //TRAINING
        //---------------
        //set up vars
        ArrayList<String[]> trainingTweets = TweetParser.getTweets("data/testData/5_6gramExampleTweets.txt");
        ArrayList<String> trainingLabelSet = new ArrayList<String>();
        TweetVector[] trainingTweetVectors = new TweetVector[trainingTweets.size()];

        //train the model on the training data
        for (int i = 0; i < trainingTweets.size(); i++) {
            String[] tweet = trainingTweets.get(i);
            trainingTweetVectors[i] = new TweetVector(tweet[0], tweet[1], tweet[2], tweet[3], tweet[4], tweet[5], trainingLabelSet);
        }
        NGramModel bigramModel = new NGramModel(6, trainingTweetVectors, NGramModel.textName, "data/stopwords.txt", 1, 1);


        //--------------
        //TESTING
        //--------------

        //feature var
        ArrayList<Hashtable<String, Double>> tfIdfFeatures = new ArrayList<Hashtable<String, Double>>();
        //annotator
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        //get the test data into phrase form so tf features can be extracted
        ArrayList<String[]> testingTweets = TweetParser.getTweets("data/testData/5More6gramExampleTweets.txt");

        //get the tfs
        for (String[] tweet: testingTweets) {
            Annotation tweetDocument = new Annotation(TweetFeatureExtractor.process(tweet[4]));
            pipeline.annotate(tweetDocument);
            CoreLabel[][] tweetPhrases = TweetFeatureExtractor.getPhrases(tweetDocument);

            Hashtable<String, Double> tfIdfForTweet = bigramModel.getFeaturesForTweetTFIDF(tweetPhrases);
            tfIdfFeatures.add(tfIdfForTweet);
        }

        System.out.println("Testing TF-IDF 6-gram features with a document frequency threshold of 1 and no stop words...");
        try {
            //generate vars
            Hashtable<String, Double> firstTweet = tfIdfFeatures.get(0);
            Hashtable<String, Double> secondTweet = tfIdfFeatures.get(1);
            Hashtable<String, Double> thirdTweet = tfIdfFeatures.get(2);
            Hashtable<String, Double> fourthTweet = tfIdfFeatures.get(3);
            Hashtable<String, Double> fifthTweet = tfIdfFeatures.get(4);

            //check the tf-idf values
            //tweet 1
            if (firstTweet.get("-pre- -pre- -pre- -pre- -pre- potato-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("-pre- -pre- -pre- -pre- potato -post--TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("-pre- -pre- -pre- potato -post- -post--TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("-pre- -pre- potato -post- -post- -post--TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("-pre- potato -post- -post- -post- -post--TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.get("potato -post- -post- -post- -post- -post--TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (firstTweet.size() != 6) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 2
            else if (secondTweet.get("-pre- -pre- -pre- -pre- -pre- moose-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("-pre- -pre- -pre- -pre- moose sheep-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("-pre- -pre- -pre- moose sheep cat-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("-pre- -pre- moose sheep cat dog-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("-pre- moose sheep cat dog goat-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("moose sheep cat dog goat pig-TFIDF") != 2.5) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("sheep cat dog goat pig -post--TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("cat dog goat pig -post- -post--TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("dog goat pig -post- -post- -post--TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("goat pig -post- -post- -post- -post--TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.get("pig -post- -post- -post- -post- -post--TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (secondTweet.size() != 11) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 3
            else if (thirdTweet.get("-pre- -pre- -pre- -pre- -pre- dance-TFIDF") != 5.0) {
                System.out.println("FAILED due to incorrect tf-idf values");
            }
            else if (thirdTweet.size() != 1) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 4
            else if (fourthTweet.size() != 0) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            //tweet 5
            else if (fifthTweet.size() != 0) {
                System.out.println("FAILED due to superfluous tf-idf values");
            }

            else {
                System.out.println("PASSED.");
            }
        }
        catch (Exception e) {
            System.out.println("FAILED due to exception.");
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }
    */

}

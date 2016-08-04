/**
 * Created by Alec Wolyniec on 8/3/16.
 */
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

}

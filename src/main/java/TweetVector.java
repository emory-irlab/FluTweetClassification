/**
 * Created by Alec Wolyniec on 4/14/16.
 */
import java.util.*;

//only one feature of each type!!
/*
    A representation of a tweet as a vector of feature-value pairs and a unique identifier
*/
public class TweetVector {
    private String profilePicLink;
    private String username;
    private String name;
    private String description;
    private String tweetText;
    private String label;
    private Hashtable<String, Double> features;
    private ArrayList<String> labelSet;

    public TweetVector(String pro, String us, String na, String des, String text, String lab, ArrayList<String> labs) {
        profilePicLink = pro;
        username = us;
        name = na;
        description = des;
        tweetText = text;
        label = lab;
        features = new Hashtable<String, Double>();
        labelSet = labs;

        //if this label is not in the data set yet, include it
        boolean found = false;
        for (String str: labs) {
            if (str.equals(label)) {
                found = true;
                break;
            }
        }
        if (!found) labelSet.add(label);
    }
    /*
    public TweetVector(String pro, String us, String na, String des, String text, String lab, Hashtable feats) {
        profilePicLink = pro;
        username = us;
        name = na;
        description = des;
        tweetText = text;
        label = lab;
        features = feats;
    }
    public TweetVector(String pro, String us, String na, String des, String text, String lab, int featNum) {
        profilePicLink = pro;
        username = us;
        name = na;
        description = des;
        tweetText = text;
        label = lab;
        features = new Hashtable<String, Integer>(featNum);
    }
    */

    public String getProfilePicLink() { return profilePicLink; }

    public String getUsername() { return username; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public String getTweetText() { return tweetText; }

    public String getLabel() { return label; }

    public String[] getLabelSet() { return labelSet.toArray(new String[labelSet.size()]); }

    public Hashtable<String, Double> getFeatures() { return features; } //enables modification

    public void addFeature(String feature, double value) { //need to purge null entries
        features.put(feature, value);
    }

    public void addFeatures(Hashtable<String, Double> input) {
        Enumeration<String> en = input.keys();
        while (en.hasMoreElements()) {
            String key = en.nextElement();
            features.put(key, input.get(key));
        }

    }

}
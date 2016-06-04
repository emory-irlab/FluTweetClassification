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
    private Hashtable<String, Integer> features;

    public TweetVector(String pro, String us, String na, String des, String text, String lab) {
        profilePicLink = pro;
        username = us;
        name = na;
        description = des;
        tweetText = text;
        label = lab;
        features = new Hashtable<String, Integer>();
    }
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

    public String getProfilePicLink() { return profilePicLink; }

    public String getUsername() { return username; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public String getTweetText() { return tweetText; }

    public String getLabel() { return label; }

    public Hashtable<String, Integer> getFeatures() { return features; } //enables modification

    public void addFeature(String feature, int value) { //need to purge null entries
        //double if necessary
        features.put(feature, value);
    }

}

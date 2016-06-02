/**
 * Created by Alec Wolyniec on 4/14/16.
 */
import java.util.*;

//only one feature of each type!!
/*
    A representation of a tweet as a vector of feature-value pairs and a unique identifier
*/
public class TweetVector {
    private String label;
    private String name;
    private Hashtable<String, Integer> features;

    public TweetVector(String na, String lab) {
        label = lab;
        name = na;
        features = new Hashtable<String, Integer>();
    }
    public TweetVector(String na, String lab, Hashtable feats) {
        label = lab;
        name = na;
        features = feats;
    }
    public TweetVector(String na, String lab, int featNum) {
        label = lab;
        name = na;
        features = new Hashtable<String, Integer>(featNum);
    }

    public String getLabel() { return label; }

    public String getName() { return name; }

    public Hashtable<String, Integer> getFeatures() { return features; } //enables modification

    public void addFeature(String feature, int value) { //need to purge null entries
        //double if necessary
        features.put(feature, value);
    }

}

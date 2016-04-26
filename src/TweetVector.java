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

    public TweetVector(String lab, String na) {
        label = lab;
        name = na;
        features = new Hashtable<String, Integer>();
    }
    public TweetVector(String lab, String na, Hashtable feats) {
        label = lab;
        name = na;
        features = feats;
    }
    public TweetVector(String lab, String na, int featNum) {
        label = lab;
        name = na;
        features = new Hashtable<String, Integer>(featNum);
    }

    public String getLabel() { return label; }

    public String getName() { return name; }

    public Hashtable<String, Integer> getFeatures() { return features; } //enables modification

    public void addFeature(StringFeatureValuePair feat) { //need to purge null entries
        //double if necessary
        features.put(feat.getFeature(), feat.getValue());
    }

}

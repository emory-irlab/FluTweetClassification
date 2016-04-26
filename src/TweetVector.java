/**
 * Created by Alec Wolyniec on 4/14/16.
 */

//only one feature of each type!!
/*
    A representation of a tweet as a vector of feature-value pairs and a unique identifier
*/
public class TweetVector {
    private String label;
    private String name;
    private StringFeatureValuePair[] features;
    private int featureCounter;

    public TweetVector(String lab, String na) {
        label = lab;
        name = na;
        features = new StringFeatureValuePair[1];
        featureCounter = 0;
    }
    public TweetVector(String lab, String na, StringFeatureValuePair[] feats) {
        label = lab;
        name = na;
        features = feats;
        featureCounter = feats.length;
    }
    public TweetVector(String lab, String na, int featNum) {
        label = lab;
        name = na;
        features = new StringFeatureValuePair[featNum];
        featureCounter = 0;
    }

    public String getLabel() { return label; }

    public String getName() { return name; }

    public StringFeatureValuePair[] getFeatures() { return features; } //enables modification

    public void addFeature(StringFeatureValuePair feat) {
        //double if necessary
        features[featureCounter++] = feat;
        if (featureCounter == features.length) {
            StringFeatureValuePair[] newFeatures = new StringFeatureValuePair[featureCounter * 2];
            for (int i = 0; i < featureCounter; i++) {
                newFeatures[i] = features[i];
            }
            features = newFeatures;
        }
    }

}

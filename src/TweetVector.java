/**
 * Created by Alec Wolyniec on 4/14/16.
 */

//only one feature of each type!!
/*
    A representation of a tweet as a vector of feature-value pairs and a unique identifier
*/
public class TweetVector {
    private String tweetID;
    private FeatureValuePair[] features;
    private int featureCounter;

    public TweetVector(String id) {
        tweetID = id;
        features = new FeatureValuePair[0];
        featureCounter = 0;
    }
    public TweetVector(String id, FeatureValuePair[] feats) {
        tweetID = id;
        features = feats;
        featureCounter = feats.length;
    }
    public TweetVector(String id, int featNum) {
        tweetID = id;
        features = new FeatureValuePair[featNum];
        featureCounter = 0;
    }

    public String getTweetID() { return tweetID; }

    public FeatureValuePair[] getFeatures() { return features; } //enables modification

    public void addFeature(FeatureValuePair feat) {
        //double if necessary
        if (featureCounter == features.length) {
            FeatureValuePair[] newFeatures = new FeatureValuePair[featureCounter * 2];
            for (int i = 0; i < featureCounter; i++) {
                newFeatures[i] = features[i];
            }
            features = newFeatures;
        }
        features[featureCounter++] = feat;
    }

}

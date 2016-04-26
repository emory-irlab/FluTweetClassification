/**
 * Created by Alec Wolyniec on 4/19/16.
 */
public class StringFeatureValuePair {
    private String feature;
    private int value;

    public StringFeatureValuePair(String feat) {
        value = 0;
        feature = feat;
    }

    public StringFeatureValuePair(String feat, int val) {
        value = val;
        feature = feat;
    }

    public String getFeature() { return feature; }

    public int getValue() { return value; }

    public void setValue(int v) { value = v; }

    public void incrementValue(int v) { value += v; }
}

/**
 * Created by Alec Wolyniec on 4/14/16.
 */

/*
    Implementation of a feature-value pair, including a feature, an integer value, and an optional
    type string that can describe the pair.
 */
public class FeatureValuePair {
    private int value;
    private String type;

    public FeatureValuePair() {
        value = 0;
        type = "";
    }

    public FeatureValuePair(int val) {
        value = val;
    }
    public FeatureValuePair(int val, String t) {
        value = val;
        type = t;
    }

    public int getValue() { return value; }

    public void setValue(int v) { value = v; }

    public void incrementValue(int v) { value += v; }

    public String getType() { return type; }

    public void setType(String t) { type =t; }
}

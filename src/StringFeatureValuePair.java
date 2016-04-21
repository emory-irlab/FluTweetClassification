/**
 * Created by Alec Wolyniec on 4/19/16.
 */
public class StringFeatureValuePair extends FeatureValuePair {
    private String feature;

    public StringFeatureValuePair(String feat) {
        super();
        feature = feat;
    }

    public StringFeatureValuePair(String feat, int val) {
        super(val);
        feature = feat;
    }

    public StringFeatureValuePair(String feat, int val, String type) {
        super(val, type);
        feature = feat;
    }

    public String getFeature() { return feature; }
}

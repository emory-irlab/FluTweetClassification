/**
 * Created by tehredwun on 4/17/16.
 */
public class test {
    public static void main (String[] args) {
        FeatureValuePair[] feats = new FeatureValuePair[3];
        StringFeatureValuePair fea = new StringFeatureValuePair("duck", 3);
        feats[0] = fea;
        StringFeatureValuePair st = (StringFeatureValuePair)feats[0];
        System.out.println(st.getFeature());
    }
}

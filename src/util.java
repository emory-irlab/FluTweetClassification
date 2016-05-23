import java.util.*;

/**
 * Created by Alec Wolyniec on 4/20/16.
 */
public class util {
    public static void printStringFeaturesIntValuesFromHashtable(Hashtable<String, Integer> table) {
        System.out.println("FEATURES FOR ONE ITEM:");
        Enumeration<String> features = table.keys();
        while (features.hasMoreElements()) {
            String feature = features.nextElement();
            int value = table.get(feature);
            System.out.println(feature+": "+value);
        }

    }
}

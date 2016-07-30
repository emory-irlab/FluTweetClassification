import edu.stanford.nlp.util.Pair;
import java.util.Comparator;
import java.util.ArrayList;

/**
 * Created by Alec Wolyniec on 7/26/16.
 */
public class MostCommonWordsComparator implements Comparator<Pair<Pair<String, Integer>, ArrayList<String>>>{
    public int compare(Pair<Pair<String, Integer>, ArrayList<String>> o1, Pair<Pair<String, Integer>, ArrayList<String>> o2) {
        int firstInt = o1.first().second();
        int secondInt = o2.first().second();
        if (firstInt < secondInt) {
            return -1;
        }
        else if (firstInt == secondInt) {
            return 0;
        }
        else {
            return 1;
        }
    }
}

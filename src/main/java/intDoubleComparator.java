import edu.stanford.nlp.util.Pair;

import java.util.Comparator;

/**
 * Created by Alec Wolyniec on 7/23/16.
 */
public class intDoubleComparator implements Comparator<Pair<Integer, Double>> {
        public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
            double do1 = o1.second();
            double do2 = o2.second();
            if (do1 < do2) {
                return -1;
            }
            else if (do1 == do2) {
                return 0;
            }
            else {
                return 1;
            }
        }

    }

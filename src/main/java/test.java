import edu.stanford.nlp.util.Pair;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.*;

/**
 * Created by Alec Wolyniec on 4/17/16.
 */
public class test {

    // [ { Accuracy: {Accuracy: 0}, Eggman: {Precision: 0, Recall: 0}, Sonic: {Precision: 0, Recall: 0} }, { Accuracy: {Accuracy: 1}, Eggman: {Precision: 1, Recall: 1}, Sonic: {Precision: 1, Recall: 1} }, {Accuracy: {Accuracy: 0.5}, Eggman: {Precision: 3, Recall: 3}, Sonic: {Precision: 3, Recall: 3} } ]

    // { Accuracy: {Accuracy: 0.5}, Eggman: {Precision: 4/3, Recall: 4/3}, Sonic: {Precision: 4/3, Recall: 4/3} }

    public static void main (String[] args) throws IOException, InterruptedException {
        //System.out.println(readTweetsGetFeatures.process("\"\"@gomezpickering: Just Back: The vibrant waterways of #Mexico's Xochimilco https://t.co/oa0XEiBLT5\"\"@TaiteilijanKuu mira :3"));
        /*
        System.out.println(readTweetsGetFeatures.process("@Riteishd &amp; Wishing u a beautiful day Hopes and dreams I`m sending your way May all be good On this very special day for new born baby"));
        System.out.println(readTweetsGetFeatures.process("@Andre_Allegiant @CarlaCantare @1happynerd @ActorJoshPrice @lauralong08 Well that's the thing, he has not \"moved on\" at all, actually..."));
        System.out.println(readTweetsGetFeatures.process("I found a dolphin :( but it had died. #dolphin #prestatyn #onthebeach #thebeach #beach #sea https://t.co/4B5WRzIFoY"));
        System.out.println(readTweetsGetFeatures.process("Death to false stoners @ Sleepy Hollow Cemetery https://t.co/mDrlAsKCfg"));
        */

        //add "duck" at position 6
        //ad "poomie" at position 0
        //add "wel" at position 1
        //add "fork" at position 4
        ArrayList<String> the = new ArrayList<String>(7);
        for (int i = 0; i < 7; i++) {
            the.add("");
        }
        the.set(6, "duck");
        the.set(0, "poomie");
        the.set(1, "wel");
        the.set(4, "fork");
        the.add(12, "mad");

        /*
        TopicFeatureModel topicFeatureModel = new TopicFeatureModel("data/topics/countFileMinusOnes.txt", "data/topics/tweet_composition.txt", "", 1);
        ArrayList<String[]> tweets = TweetParser.getTweets("data/tweets/tweet_event.csv");
        double average = 0.0;
        int tweetNum = 0;
        for (String[] tweet: tweets) {
            tweetNum++;
            long startTime = System.currentTimeMillis();
            int[] most = topicFeatureModel.getNMostLikelyTopics(3, tweet[4]);
            double time = ((double)System.currentTimeMillis() - startTime)/1000;
            System.out.println(time+" seconds to get");
            average = (((tweetNum - 1) * average) + time)/tweetNum;
            if (tweetNum % 25 == 0) {
                System.out.println("   Average time so far: "+average+ " seconds. Time for 30k tweets is "+average*30000+" seconds "+average*500+" minutes "+average*500/60+" hours");
            }
        }

        long startTime = System.currentTimeMillis();
        int[] most = topicFeatureModel.getNMostLikelyTopics(3, "stage race monday jack louis wins flag stream sunday ichoosenicki crowd ji football victory game weekend watching jerry pita");
        System.out.println(((double)System.currentTimeMillis() - startTime)/1000+" seconds to get");
        startTime = System.currentTimeMillis();
        most = topicFeatureModel.getNMostLikelyTopics(3, "a new baby is like the beginning of all things: hope, faith, ");
        System.out.println(((double)System.currentTimeMillis() - startTime)/1000+" seconds to get");
        startTime = System.currentTimeMillis();
        most = topicFeatureModel.getNMostLikelyTopics(3, "attention college students! business economic b4 graduating. n't pertain major.");
        System.out.println(((double)System.currentTimeMillis() - startTime)/1000+" seconds to get");
        startTime = System.currentTimeMillis();
        most = topicFeatureModel.getNMostLikelyTopics(3, "Leaving jetblue plane  @ ronald reagan washington national airport");
        System.out.println(((double)System.currentTimeMillis() - startTime)/1000+" seconds to get");
        startTime = System.currentTimeMillis();
        most = topicFeatureModel.getNMostLikelyTopics(3, "WALK FREE CHARITY - END SLAVERY just supported Share the Facts. Fight Slavery on ThunderclapIt // walkfree");
        System.out.println(((double)System.currentTimeMillis() - startTime)/1000+" seconds to get");
        startTime = System.currentTimeMillis();
        most = topicFeatureModel.getNMostLikelyTopics(3, "SteveBrooksTA thearchers Not so sure. She might take the line that Rob is welcome to him. And then regret it.");
        System.out.println(((double)System.currentTimeMillis() - startTime)/1000+" seconds to get");
        startTime = System.currentTimeMillis();
        most = topicFeatureModel.getNMostLikelyTopics(3, "Puji Tuhan officially Tulang, welcome baby (at RSIA MINA) [pic]");
        System.out.println(((double)System.currentTimeMillis() - startTime)/1000+" seconds to get");
        startTime = System.currentTimeMillis();
        most = topicFeatureModel.getNMostLikelyTopics(3, "OK here is one for you I been dealing with a rare disease for 8 years now I have cancer and if I do chemo my chances of");
        System.out.println(((double)System.currentTimeMillis() - startTime)/1000+" seconds to get");
        startTime = System.currentTimeMillis();
        most = topicFeatureModel.getNMostLikelyTopics(3, "Love your work SaraJapanwalla: Lovely sketch of my fav actor mahirahkhan in person! She looks ju");
        System.out.println(((double)System.currentTimeMillis() - startTime)/1000+" seconds to get");
        startTime = System.currentTimeMillis();
        most = topicFeatureModel.getNMostLikelyTopics(3, "beautiful travel wanderlust borabora aintgotnothingonthis animal lovely vsco vscocam vscophoto");
        System.out.println(((double)System.currentTimeMillis() - startTime)/1000+" seconds to get");
        */
    }
}

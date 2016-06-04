import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Alec Wolyniec on 4/17/16.
 */
public class test {
    public static int containsURL(String tweet) {
		/*
		 * NOTICE: More sophisticated pattern, however was unable to pass simple test.
		 * Could be on my part.
		 * */
        //Regex pattern found at "http://stackoverflow.com/questions/163360/regular-expression-to-match-urls-in-java"
		/*Pattern locateURL = Pattern.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
		Matcher URLFinder = locateURL.matcher(tweet);*/

        Pattern detectURL = Pattern.compile("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
        //Pattern detectURL = Pattern.compile("(http)|(https)|(.com)|(.gov)|(.org)");
        Matcher URLdetector = detectURL.matcher(tweet);

        if (URLdetector.find()) {
            return 1;
        }
        return 0;
    }

    public static void main (String[] args) {
        System.out.println(containsURL("I am a little teapot short and stout, this is my http://www.dankmemes.org"));
        System.out.println(containsURL("httpr://mybluestud.com"));
        System.out.println(containsURL("Visit us at http://instagr.am/dougiefresh"));
        System.out.println(containsURL("@ http://t.co/cTvyaQFaj5"));
    }
}

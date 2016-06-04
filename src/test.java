import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Alec Wolyniec on 4/17/16.
 */
public class test {
    public static int containsMention(String tweet) {
        int in = tweet.indexOf('@');
        while (in != -1) {
            String after = tweet.substring(Math.min(in+1, tweet.length()), Math.min(in+10, tweet.length()));
            Pattern pattern = Pattern.compile(" ?https?://");
            Matcher matcher = pattern.matcher(after);
            //if there is no http(s):// right after the "@", count as user mention
            if (!matcher.find() || matcher.start() > 0) return 1;

            in = tweet.indexOf('@', in+1);
        }
        return 0;
    }

    public static void main (String[] args) {
        System.out.println(containsMention(" baba booey @ducknado777"));
        System.out.println(containsMention(" eyy ey oo @  dsji @https://co.mpa.com"));
        System.out.println(containsMention("@a http://"));
        System.out.println(containsMention("@ManUtd Supporters Club | http://"));
        System.out.println(containsMention(""));
    }
}

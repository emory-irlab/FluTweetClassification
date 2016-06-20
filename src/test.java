import javax.xml.soap.Text;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Alec Wolyniec on 4/17/16.
 */
public class test {

    public static void main (String[] args) {
        String orig = "I #am a https://ducks.com person, are you https://ducks.com listening @ donkeypoo @ https://a.com spark?";
        String noURL = TextFeatures.removeURL(orig);
        System.out.println(noURL);
        String noHashtags = TextFeatures.removeHashtags(noURL);
        System.out.println(noHashtags);
        System.out.println(TextFeatures.removePattern(noHashtags, TextFeatures.atPattern));
    }
}


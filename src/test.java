import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Alec Wolyniec on 4/17/16.
 */
public class test {

    public static void main (String[] args) {
        System.out.println(TextFeatures.isAllUpperCase("JOYW AVE "));
        System.out.println(TextFeatures.isAllUpperCase("Shamalamadingdong"));
        System.out.println(TextFeatures.isAllUpperCase("39plebian"));
    }
}


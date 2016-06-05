import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Alec Wolyniec on 4/17/16.
 */
public class test {

    public static void main (String[] args) {
        System.out.println(TextFeatures.containsCommonFirstName("Jerry Rosen"));
        System.out.println(TextFeatures.containsCommonFirstName("Yobany Alonso Casas"));
        System.out.println(TextFeatures.containsCommonFirstName("John Cena"));
        System.out.println(TextFeatures.containsCommonFirstName("Frank Kudasik III"));
    }
}

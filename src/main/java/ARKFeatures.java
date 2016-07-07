import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cmu.arktweetnlp.Twokenize;
import cmu.arktweetnlp.impl.Model;
import cmu.arktweetnlp.impl.ModelSentence;
import cmu.arktweetnlp.impl.Sentence;
import cmu.arktweetnlp.impl.features.FeatureExtractor;

/**
 * Tagger object -- wraps up the entire tagger for easy usage from Java.
 *
 * To use:
 *
 * (1) call loadModel().
 *
 * (2) call tokenizeAndTag() for every tweet.
 *
 * See main() for example code.
 *
 * (Note RunTagger.java has a more sophisticated runner.
 * This class is intended to be easiest to use in other applications.)
 */

public class ARKFeatures {

    public static String MODEL_FILENAME = "/cmu/arktweetnlp/model.20120919";
    public static ARKFeatures tagger = new ARKFeatures();
    public Model model;
    public FeatureExtractor featureExtractor;
    public static String[] posEmoticons = {":)", ":D", "(:", ":-)", ":]", ":3", ":>", "=]", "8)", "=)", ":}", ":^)", ":‑D", "8‑D",
            "8D", "x‑D", "xD", "X‑D", "XD", "=‑D", "=D", "=‑3", "=3", "B^D",
            ":-))", ";‑)", ";)", "*-)", "*)", ";‑]", ";]", ";D", ";^)", ":‑,", ":*,", ":-*", ":^*",
            ":‑P", ":P", "X‑P", "x‑p", "xp", "XP", ":‑p", ":p", "=p", ":‑Þ", ":Þ", ":þ", ":‑þ", ":‑b",
            ":b", "d:", "O:‑)", "0:‑3", "0:3", "0:‑)", "0:)", "0;^)", ">:)", ">;)", ">:‑)"};

    public static String[] negEmoticons = {">:[", ":‑(", ":(", ":‑c", ":c", ":‑<", ":<", ":‑[",
            ":[", ":{", ";(", ":-||", ":@", ">:(", ":'‑(", ":'(",
            "D:<", "D:", "D8", "D;", "D=", "DX", "v.v", "D‑':",
            ">:O", ":‑O", ":O", ":‑o", ":o", "8‑0", "O_O", "o‑o",
            "O_o", "o_O", "o_o", "O-O", ">:\\", ">:/", ":‑/", ":‑.",
            ":/", ":\\", "=/", "=\\", ":L", "=L", ":S", ">.<"};

    public static String[] personalPronouns = {"I", "i", "my", "myself", "me"};

    /**
     * One token and its tag.
     **/
    public static class TaggedToken {
        public String token;
        public String tag;
    }

    /**
     * @param modelFilename
     * @throws IOException
     */
    public void loadModel(String modelFilename) throws IOException {
        model = Model.loadModelFromText(modelFilename);
        featureExtractor = new FeatureExtractor(model, false);
    }

    public static void loadModelStatically() throws IOException {
        tagger.loadModel(MODEL_FILENAME);
    }

    public static int positiveEmoticons(List<TaggedToken> tokens) {
        int count = 0;

        for(TaggedToken t : tokens) {
            if(t.tag.equals("E")) {
                for (String emoticon : posEmoticons) {
                    if(emoticon.contains(t.tag)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static int negativeEmoticons(List<TaggedToken> tokens) {
        int count = 0;

        for(TaggedToken t : tokens) {
            if(t.tag.equals("E")) {
                for (String emoticon : negEmoticons) {
                    if(emoticon.contains(t.tag)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static int numberOfPersonalPronouns(List<TaggedToken> tokens) {
        int count = 0;

        for (TaggedToken t : tokens) {
            if (t.tag.equals("O")) {
                for (String s : personalPronouns){
                    if (s.contains(t.token)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Run the tokenizer and tagger on one tweet's text.
     **/
    public List<TaggedToken> tokenizeAndTag(String text) {
        if (model == null) throw new RuntimeException("Must loadModel() first before tagging anything");
        List<String> tokens = Twokenize.tokenizeRawTweetText(text);

        Sentence sentence = new Sentence();
        sentence.tokens = tokens;
        ModelSentence ms = new ModelSentence(sentence.T());
        featureExtractor.computeFeatures(sentence, ms);
        model.greedyDecode(ms, false);

        ArrayList<TaggedToken> taggedTokens = new ArrayList<TaggedToken>();

        for (int t=0; t < sentence.T(); t++) {
            TaggedToken tt = new TaggedToken();
            tt.token = tokens.get(t);
            tt.tag = model.labelVocab.name( ms.labels[t] );
            taggedTokens.add(tt);
        }
        return taggedTokens;
    }

    /**
     * Illustrate how to load and call the POS tagger.
     * This main() is not intended for serious use; see RunTagger.java for that.
     **/
	/*public static void main(String[] args) throws IOException {
		
		ARKTagger tagger = new ARKTagger();
		tagger.loadModel(model_Filename);
		String text;
		List<TaggedToken> tokens = tagger.tokenizeAndTag(text);
		
		for (TaggedToken token : taggedTokens) {
			System.out.printf("%s\t%s\n", token.tag, token.token);
		}
	}*/
}

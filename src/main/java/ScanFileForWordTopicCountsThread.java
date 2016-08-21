import cc.mallet.types.Instance;

import java.util.ArrayList;
import java.io.*;

/**
 * Created by Alec Wolyniec on 8/4/16.
 */
public class ScanFileForWordTopicCountsThread implements Runnable {
    public Thread thread;
    private String threadName;
    private String filePath;
    private String word;

    private ArrayList<String> results;

    ScanFileForWordTopicCountsThread(String word, String pathToFile, String name) {
        this.filePath = pathToFile;
        this.word = word;
        threadName = name;
        results = new ArrayList<String>();
    }

    public ArrayList<String> getResults() {
        return results;
    }

    public void run() {
        //System.out.println("Running thread "+threadName);
        try {
            //go through the file and get the topic counts for the word, if they're there
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(filePath)));
            String currentLine;

            while ((currentLine = bufferedReader.readLine()) != null) {
                String[] split = currentLine.split(" ");
                if (split.length < 3) {
                    continue;
                }

                //check to see if this line contains the count for the provided word
                String wordInLine = split[1];
                if (wordInLine.equals(word)) {
                    //if it does, append all of the counts to the ArrayList
                    for (int i = 2; i < split.length; i++) {
                        results.add(split[i]);
                    }
                    break;
                }
            }

            bufferedReader.close();
            //obligatory
            Thread.sleep(1);

        } catch(InterruptedException e) {
            System.out.println("Thread interrupted");
        } catch(IOException e) {
            System.out.println("IOException");
            e.printStackTrace();
        }
        //System.out.println("Thread exiting");
    }

    public void start() {
        //System.out.println("Starting thread "+threadName);
        if (thread == null) {
            thread = new Thread(this, threadName);
            thread.start();
        }
    }
}
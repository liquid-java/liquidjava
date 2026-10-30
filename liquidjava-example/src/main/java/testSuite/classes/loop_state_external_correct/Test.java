package testSuite.classes.loop_state_external_correct;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

// Issue #427: the states of an object whose class has an external spec, computed over the iterations of a loop
public class Test {
    // the body restores the state it starts from
    static void markAndResetInLoop(Reader in, int n) throws IOException {
        BufferedReader br = new BufferedReader(in);
        int k = 0;
        while (k < n) {
            br.mark(100);
            br.readLine();
            br.reset();
            k++;
        }
        br.mark(100);
        br.reset();
        br.close();
    }

    // br is open or marked at the start of every iteration and after the loop
    static void openOrMarked(Reader in, int n) throws IOException {
        BufferedReader br = new BufferedReader(in);
        for (int i = 0; i < n; i++) {
            br.readLine();
            br.mark(100);
        }
        br.readLine();
        br.close();
    }

    static void doWhile(Reader in, int n) throws IOException {
        BufferedReader br = new BufferedReader(in);
        int k = 0;
        do {
            br.mark(100);
            br.reset();
            k++;
        } while (k < n);
        br.close();
    }

    static void forEach(Reader in, int[] values) throws IOException {
        BufferedReader br = new BufferedReader(in);
        for (int value : values) {
            br.mark(value);
            br.readLine();
        }
        br.readLine();
        br.close();
    }

    // marked on every path to the next iteration and to the exit
    static void markBeforeContinueAndBreak(Reader in, int n) throws IOException {
        BufferedReader br = new BufferedReader(in);
        br.mark(100);
        for (int i = 0; i < n; i++) {
            br.reset();
            br.mark(100);
            if (i > 2)
                continue;
            if (i > 5)
                break;
        }
        br.reset();
        br.close();
    }

    // the inner loop leaves br open or marked, which the outer loop allows
    static void nestedLoops(Reader in, int n) throws IOException {
        BufferedReader br = new BufferedReader(in);
        for (int i = 0; i < n; i++) {
            br.readLine();
            for (int j = 0; j < i; j++)
                br.mark(100);
        }
        br.close();
    }

    // the fix of the tabu-timetabling bug: mark before the inner loop
    static void markBeforeInnerLoop(Reader in) throws IOException {
        BufferedReader br = new BufferedReader(in);
        String currentLine;
        while ((currentLine = br.readLine()) != null) {
            br.mark(10000);
            String nextLine;
            while ((nextLine = br.readLine()) != null && currentLine.equals(nextLine)) {
                br.mark(10000);
            }
            br.reset();
        }
        br.close();
    }

    // closed only when the loop ends
    static void closeAfterLoop(Reader in) throws IOException {
        BufferedReader br = new BufferedReader(in);
        while (br.readLine() != null) {
            br.readLine();
        }
        br.close();
    }
}

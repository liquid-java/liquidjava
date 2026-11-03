package testSuite.classes.loop_state_external_error;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

// Issue #427: the state after a loop, and at the start of every iteration, of an object whose class has an external
// spec
public class Test {
    // the loop may run zero times, leaving br open
    static void zeroIterations(Reader in, int n) throws IOException {
        BufferedReader br = new BufferedReader(in);
        int k = 0;
        while (k < n) {
            br.mark(100);
            k++;
        }
        br.reset(); // Expect: State Refinement Error
    }

    // from the second iteration on, br is closed
    static void laterIteration(Reader in, int n) throws IOException {
        BufferedReader br = new BufferedReader(in);
        for (int i = 0; i < n; i++) {
            br.readLine(); // Expect: State Refinement Error
            br.close();
        }
    }

    static void doWhileLaterIteration(Reader in, int n) throws IOException {
        BufferedReader br = new BufferedReader(in);
        int k = 0;
        do {
            br.readLine(); // Expect: State Refinement Error
            br.close();
            k++;
        } while (k < n);
    }

    static void forEachMayBeEmpty(Reader in, int[] values) throws IOException {
        BufferedReader br = new BufferedReader(in);
        for (int value : values) {
            br.mark(value);
        }
        br.reset(); // Expect: State Refinement Error
    }

    // a continue goes on to the next iteration with br closed
    static void closedBeforeContinue(Reader in, int n) throws IOException {
        BufferedReader br = new BufferedReader(in);
        for (int i = 0; i < n; i++) {
            br.readLine(); // Expect: State Refinement Error
            if (i > 2) {
                br.close();
                continue;
            }
        }
    }

    // the loop exits through its condition with br open, or through the break with br marked
    static void breakOrCondition(Reader in, int n) throws IOException {
        BufferedReader br = new BufferedReader(in);
        int k = 0;
        while (k < n) {
            br.mark(100);
            if (k > 2)
                break;
            br.reset();
            k++;
        }
        br.reset(); // Expect: State Refinement Error
    }

    // marked by an inner loop, which may run zero times, in the second iteration of the outer one
    static void nestedLoops(Reader in, int n) throws IOException {
        BufferedReader br = new BufferedReader(in);
        for (int i = 0; i < n; i++) {
            br.reset(); // Expect: State Refinement Error
            for (int j = 0; j < i; j++)
                br.mark(100);
        }
    }

    // tabu-timetabling InputReader: the inner loop may stop at once, when the next line belongs to another student,
    // and then reset runs without a mark ("Stream not marked")
    static void markOnlyInInnerLoop(Reader in) throws IOException {
        BufferedReader br = new BufferedReader(in);
        String currentLine;
        while ((currentLine = br.readLine()) != null) {
            String nextLine;
            while ((nextLine = br.readLine()) != null && currentLine.equals(nextLine)) {
                br.mark(10000);
            }
            br.reset(); // Expect: State Refinement Error
        }
        br.close();
    }
}

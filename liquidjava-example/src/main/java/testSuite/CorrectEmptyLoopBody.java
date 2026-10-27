package testSuite;

import java.util.Iterator;

import liquidjava.specification.Refinement;

// Loops whose body is an empty statement (#395)
@SuppressWarnings("unused")
public class CorrectEmptyLoopBody {
    static int total(int[] counts) {
        int total = 0;
        for (int i = 0; i < counts.length; total += counts[i], i++)
            ;
        return total;
    }

    static void countDown() {
        @Refinement("_ >= 0")
        int n = 10;
        for (; n > 0; n--)
            ;
    }

    static void skip(Iterator<String> it, int[] xs) {
        while (it.hasNext() && it.next() != null)
            ;
        do
            ;
        while (it.hasNext() && it.next() != null);
        for (int x : xs)
            ;
    }
}

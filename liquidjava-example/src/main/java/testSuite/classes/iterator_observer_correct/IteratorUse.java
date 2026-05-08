package testSuite.classes.iterator_observer_correct;

import java.util.Scanner;

public class IteratorUse {

    public static void readOne(Scanner it) {
        if (it.hasNext()) {
            it.next();
        }
    }

    public static void readTwo(Scanner it) {
        if (it.hasNext()) {
            it.next();
            if (it.hasNext()) {
                it.next();
            }
        }
    }

    public static void readInElse(Scanner it) {
        if (!it.hasNext()) {
        } else {
            it.next();
        }
    }

    public static void recursive(Scanner it) {
        if (it.hasNext()) {
            String s = it.next(); 
            System.out.println(s);
            recursive(it);
        } 
    }
}

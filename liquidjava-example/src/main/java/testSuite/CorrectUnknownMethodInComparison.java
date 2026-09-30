package testSuite;

import java.util.List;

// Results of methods without refinements can be used in operations (issue #300)
@SuppressWarnings("unused")
public class CorrectUnknownMethodInComparison {
    public void lengthInComparison(String s) {
        if (s.length() < 3) {
            System.out.println("short");
        }
    }

    public void equalsInDisjunction(String s) {
        boolean known = s.equals("a") || s.equals("b");
    }

    public void sizeInLoopBound(List<String> list) {
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i));
        }
    }
}

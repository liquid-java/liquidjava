package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class CorrectCharLiteralComparisons {

    static int skipTo(String s) {
        int i = 0;
        while (s.charAt(i) != '[')
            i++;
        return i;
    }

    static boolean isSeparator(char c) {
        return c == '{' || c == '}' || c == ',' || c == ' ' || c == 'Z' || c == '"' || c == '\\';
    }

    @Refinement("_ == 48")
    static int zero(char c) {
        if (c == '0')
            return c;
        return 48;
    }

    @Refinement("_ == 97")
    static int letterA(char c) {
        int a = 5;
        if (c == 'a')
            return c;
        return 97;
    }

    @Refinement("_ == 91")
    static int bracket(char c) {
        char d = c;
        d += '[' - c;
        return d;
    }
}

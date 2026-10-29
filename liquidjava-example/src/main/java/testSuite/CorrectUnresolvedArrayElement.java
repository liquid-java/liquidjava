package testSuite;

import org.example.missing.Cookie;

import liquidjava.specification.Refinement;

// Array elements whose type cannot be resolved (#425)
@SuppressWarnings("unused")
public class CorrectUnresolvedArrayElement {
    static void any(@Refinement("_ == _") Object o) {
    }

    void createAll(Cookie[] cookies) {
        for (int i = 0; i < cookies.length; i++) {
            cookies[i].create();
        }
    }

    Cookie first(Cookie[] cookies) {
        return cookies[0];
    }

    void passed(Cookie[] cookies) {
        any(cookies[0]); // the element is part of the SMT query
    }
}

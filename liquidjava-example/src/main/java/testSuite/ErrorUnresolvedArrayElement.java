package testSuite;

import org.example.missing.Cookie;

import liquidjava.specification.Refinement;

// A violation after reading an array element whose type cannot be resolved is still reported (#425)
@SuppressWarnings("unused")
public class ErrorUnresolvedArrayElement {
    static void positive(@Refinement("_ > 0") int x) {
    }

    static void first(Cookie[] cookies) {
        Cookie cookie = cookies[0];
        cookie.create();
        positive(-1); // Expect: Refinement Error
    }
}

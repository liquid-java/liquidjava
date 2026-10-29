package testSuite;

import org.example.missing.Request;

import liquidjava.specification.Refinement;

// A violation after a lambda whose parameter type cannot be resolved is still reported (#396)
@SuppressWarnings("unused")
public class ErrorUnresolvedLambdaParameter {
    static void positive(@Refinement("_ > 0") int x) {
    }

    static void print(Request request) {
        request.names().forEach(name -> System.out.println(name));
        positive(-1); // Expect: Refinement Error
    }
}

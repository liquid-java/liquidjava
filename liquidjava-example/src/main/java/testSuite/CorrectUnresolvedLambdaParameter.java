package testSuite;

import org.example.missing.Request;

// Lambda parameters whose type cannot be resolved (#396)
@SuppressWarnings("unused")
public class CorrectUnresolvedLambdaParameter {
    static void print(Request request) {
        request.names().forEach(name -> System.out.println(name));
    }

    static void headers(Request request) {
        request.headerNames().forEach(header -> request.copy(header, request.header(header)));
    }
}

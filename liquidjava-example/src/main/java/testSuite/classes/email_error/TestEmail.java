package testSuite.classes.email_error;

public class TestEmail {

    public static void main(String[] args) {

        Email e = new Email();
        e.from("me");
        // missing to
        e.subject("not important"); // State Refinement Error
        e.body("body");
        e.build();
    }
}

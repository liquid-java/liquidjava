package testSuite.classes.email_correct;

import java.util.ArrayList;
import java.util.List;
import liquidjava.specification.RefinementAlias;
import liquidjava.specification.RefinementPredicate;
import liquidjava.specification.StateRefinement;

// https://blog.sigplan.org/2021/03/02/fluent-api-practice-and-theory/
// Suppose there is only one acceptable order to construct the email
// add sender -> add multiple receivers -> add subject <optional> -> add body -> build()
@SuppressWarnings("unused")
@RefinementAlias("EmptyEmail (Email e) { state(e) == 1}")
@RefinementAlias("SenderSet  (Email e) { state(e) == 2}")
@RefinementAlias("ReceiverSet(Email e) { state(e) == 3}")
@RefinementAlias("BodySet    (Email e) { state(e) == 4}")
@RefinementPredicate("int state(Email e)")
public class Email {
    private String sender;
    private List<String> receiver;
    private String subject;
    private String body;

    @StateRefinement(to = "EmptyEmail(this)")
    public Email() {
        receiver = new ArrayList<>();
    }

    @StateRefinement(from = "EmptyEmail(this)", to = "SenderSet(this)")
    public void from(String s) {
        sender = s;
    }

    @StateRefinement(from = "(SenderSet(this)) || (ReceiverSet(this))", to = "ReceiverSet(this)")
    public void to(String s) {
        receiver.add(s);
    }

    @StateRefinement(from = "ReceiverSet(this)", to = "ReceiverSet(this)")
    public void subject(String s) { // optional
        subject = s;
    }

    @StateRefinement(from = "ReceiverSet(this)", to = "BodySet(this)")
    public void body(String s) {
        body = s;
    }

    @StateRefinement(from = "BodySet(this)", to = "BodySet(this)")
    public String build() {
        return "email..."; // string with all the email
    }
}

package liquidjava.processor.context;

import liquidjava.rj_language.Predicate;

public class ObjectState {

    Predicate from;
    Predicate to;
    String message;

    public ObjectState() {
    }

    public ObjectState(Predicate from, Predicate to) {
        this.from = from;
        this.to = to;
    }

    public ObjectState(Predicate from, Predicate to, String message) {
        this.from = from;
        this.to = to;
        this.message = message;
    }

    public void setFrom(Predicate from) {
        this.from = from;
    }

    public void setTo(Predicate to) {
        this.to = to;
    }

    public boolean hasFrom() {
        return from != null;
    }

    public boolean hasTo() {
        return to != null;
    }

    public Predicate getFrom() {
        return from != null ? from : new Predicate();
    }

    public Predicate getTo() {
        return to != null ? to : new Predicate();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ObjectState clone() {
        return new ObjectState(from.clone(), to.clone(), message);
    }

    @Override
    public String toString() {
        return "ObjectState [from=" + from + ", to=" + to + "]";
    }
}

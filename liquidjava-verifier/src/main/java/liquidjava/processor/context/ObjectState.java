package liquidjava.processor.context;

import liquidjava.rj_language.Predicate;
import spoon.reflect.cu.SourcePosition;

public class ObjectState {

    Predicate from;
    Predicate to;
    SourcePosition fromPosition;
    SourcePosition toPosition;
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

    public SourcePosition getFromPosition() {
        return fromPosition;
    }

    public void setFromPosition(SourcePosition fromPosition) {
        this.fromPosition = fromPosition;
    }

    public SourcePosition getToPosition() {
        return toPosition;
    }

    public void setToPosition(SourcePosition toPosition) {
        this.toPosition = toPosition;
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
        Predicate clonedFrom = from == null ? null : from.clone();
        Predicate clonedTo = to == null ? null : to.clone();
        ObjectState clone = new ObjectState(clonedFrom, clonedTo, message);
        clone.setFromPosition(fromPosition);
        clone.setToPosition(toPosition);
        return clone;
    }

    @Override
    public String toString() {
        return "ObjectState [from=" + from + ", to=" + to + "]";
    }
}

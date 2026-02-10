package liquidjava.processor.context;

import liquidjava.rj_language.Predicate;
import spoon.reflect.reference.CtTypeReference;

public abstract class Refined {

    private String name; // y
    private CtTypeReference<?> type; // int
    private Predicate refinement; // 9 <= y && y <= 100
    private String message;

    public Refined() {
    }

    public Refined(String name, CtTypeReference<?> type, Predicate refinement) {
        this.name = name;
        this.type = type;
        this.refinement = refinement;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CtTypeReference<?> getType() {
        return type;
    }

    public void setType(CtTypeReference<?> type) {
        this.type = type;
    }

    public void setRefinement(Predicate c) {
        this.refinement = c;
    }

    public Predicate getRefinement() {
        if (refinement != null)
            return refinement;
        return new Predicate();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Predicate getRenamedRefinements(String toReplace) {
        return refinement.substituteVariable(name, toReplace);
    }

    @Override
    public String toString() {
        return "Refined [name=" + name + ", type=" + type + ", refinement=" + refinement + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((refinement == null) ? 0 : refinement.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Refined other = (Refined) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (type == null) {
            return other.type == null;
        } else {
            return type.equals(other.type);
        }
    }
}

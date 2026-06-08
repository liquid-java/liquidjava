package liquidjava.rj_language;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import spoon.reflect.reference.CtTypeReference;

public class SimplifiedPredicate extends Predicate {

    private final Predicate simplified;
    private final Predicate origin;
    private final List<Binder> binders;

    public SimplifiedPredicate(Predicate simplified, Predicate origin) {
        this(simplified, origin, List.of());
    }

    public SimplifiedPredicate(Predicate simplified, Predicate origin, List<Binder> binders) {
        super(simplified.getExpression());
        this.simplified = simplified;
        this.origin = origin;
        this.binders = new ArrayList<>(binders);
    }

    public Predicate getSimplifiedPredicate() {
        return simplified;
    }

    public Predicate getOrigin() {
        return origin;
    }

    public List<Binder> getBinders() {
        return binders;
    }

    @Override
    public boolean isBooleanTrue() {
        return getSimplifiedPredicate().isBooleanTrue();
    }

    @Override
    public SimplifiedPredicate clone() {
        return new SimplifiedPredicate(getSimplifiedPredicate().clone(), origin.clone(), binders);
    }

    @Override
    public String toString() {
        return getSimplifiedPredicate().toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSimplifiedPredicate(), origin, binders);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SimplifiedPredicate other = (SimplifiedPredicate) obj;
        return getSimplifiedPredicate().equals(other.getSimplifiedPredicate()) && origin.equals(other.origin)
                && binders.equals(other.binders);
    }

    public static class Binder {
        private final String name;
        private final String type;

        public Binder(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public Binder(String name, CtTypeReference<?> type) {
            this(name, type.getQualifiedName());
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Binder other = (Binder) obj;
            return name.equals(other.name) && type.equals(other.type);
        }
    }
}

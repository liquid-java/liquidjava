package liquidjava.rj_language;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import spoon.reflect.reference.CtTypeReference;

/**
 * Represents a predicate simplified from another predicate. Stores the original predicate and any variables that must
 * be reintroduced as binders when relating the simplified predicate back to its origin.
 * <p>
 * For example, simplifying {@code x == 1 && y > x} with binders {@code x: int, y: int} may produce
 * {@code y > 1}. The origin {@code x == 1 && y > x} and binder {@code x: int} are kept so we can relate the simplified predicate back to the original.
 */
public class SimplifiedPredicate extends Predicate {

    private final Predicate origin;
    private final List<Binder> binders;

    public SimplifiedPredicate(Predicate simplified, Predicate origin) {
        this(simplified, origin, List.of());
    }

    public SimplifiedPredicate(Predicate simplified, Predicate origin, List<Binder> binders) {
        super(simplified.getExpression());
        this.origin = origin;
        this.binders = new ArrayList<>(binders);
    }

    public Predicate getSimplifiedPredicate() {
        return new Predicate(getExpression());
    }

    public Predicate getOrigin() {
        return origin;
    }

    public List<Binder> getBinders() {
        return binders;
    }

    @Override
    public SimplifiedPredicate clone() {
        return new SimplifiedPredicate(new Predicate(getExpression().clone()), origin.clone(), binders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getExpression(), origin, binders);
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
        return getExpression().equals(other.getExpression()) && origin.equals(other.origin)
                && binders.equals(other.binders);
    }

    /**
     * Represents a variable that must be bound when relating the simplified predicate to its origin
     */
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

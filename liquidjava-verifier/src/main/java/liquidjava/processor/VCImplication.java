package liquidjava.processor;

import java.util.Objects;

import liquidjava.rj_language.Predicate;
import liquidjava.utils.Utils;
import spoon.reflect.reference.CtTypeReference;

/**
 * @author cgamboa
 */
public class VCImplication {
    String name;
    CtTypeReference<?> type;
    Predicate refinement;
    VCImplication next;

    public VCImplication(String name, CtTypeReference<?> type, Predicate ref) {
        this.name = name;
        this.type = type;
        this.refinement = ref;
    }

    public VCImplication(Predicate ref) {
        this.refinement = ref;
    }

    public VCImplication(VCImplication implication, Predicate ref) {
        this.name = implication.name;
        this.type = implication.type;
        this.refinement = ref;
    }

    public Predicate getOriginRefinement() {
        return refinement.clone();
    }

    public VCImplication copyWithRefinement(Predicate refinement) {
        return new VCImplication(this, refinement);
    }

    public void setNext(VCImplication c) {
        next = c;
    }

    public String getName() {
        return name;
    }

    public CtTypeReference<?> getType() {
        return type;
    }

    public Predicate getRefinement() {
        return refinement;
    }

    public void setRefinement(Predicate refinement) {
        this.refinement = refinement;
    }

    public VCImplication getNext() {
        return next;
    }

    public boolean hasNext() {
        return next != null;
    }

    public boolean hasBinder() {
        return name != null && type != null;
    }

    public String toString() {
        if (name != null && type != null) {
            String qualType = type.getQualifiedName();
            String simpleType = qualType.contains(".") ? Utils.getSimpleName(qualType) : qualType;
            return String.format("%-20s %s %s", "∀" + name + ":" + simpleType + ",", refinement.toString(),
                    next != null ? " => \n" + next : "");
        } else
            return String.format("%-20s %s", "", refinement.toString());
    }

    public Predicate toConjunctions() {
        Predicate c = new Predicate();
        if (name == null && type == null && next == null)
            return c;
        c = auxConjunction(c);
        return c;
    }

    private Predicate auxConjunction(Predicate c) {
        Predicate t = Predicate.createConjunction(c, refinement);
        if (next == null)
            return t;
        t = next.auxConjunction(t);
        return t;
    }

    public VCImplication clone() {
        VCImplication vc = new VCImplication(this.name, this.type, this.refinement.clone());
        if (this.next != null)
            vc.next = this.next.clone();
        return vc;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, typeName(), refinement, next);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VCImplication other = (VCImplication) obj;
        return Objects.equals(name, other.name) && Objects.equals(typeName(), other.typeName())
                && Objects.equals(refinement, other.refinement) && Objects.equals(next, other.next);
    }

    private String typeName() {
        return type == null ? null : type.getQualifiedName();
    }
}

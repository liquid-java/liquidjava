package liquidjava.processor.context;

import java.util.List;
import liquidjava.rj_language.Predicate;
import spoon.reflect.reference.CtTypeReference;

public class GhostState extends GhostFunction {

    private GhostFunction parent;
    private Predicate refinement;
    private final String file;

    public GhostState(String name, List<CtTypeReference<?>> list, CtTypeReference<?> returnType, String prefix,
            String file) {
        super(name, list, returnType, prefix);
        this.file = file;
    }

    public void setGhostParent(GhostFunction parent) {
        this.parent = parent;
    }

    public void setRefinement(Predicate c) {
        refinement = c;
    }

    public GhostFunction getParent() {
        return parent;
    }

    public Predicate getRefinement() {
        return refinement;
    }

    public String getFile() {
        return file;
    }
}

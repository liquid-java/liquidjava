package liquidjava.processor.context;

import java.util.ArrayList;
import java.util.List;
import liquidjava.processor.facade.AliasDTO;
import liquidjava.rj_language.Predicate;
import liquidjava.utils.Utils;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

public class AliasWrapper {
    private final String name;
    private final List<CtTypeReference<?>> varTypes;
    private final List<String> varNames;
    private final Predicate expression;
    private final String definition;
    private final SourcePosition position;

    public AliasWrapper(AliasDTO a, Factory factory, String klass, String path, SourcePosition position) {
        name = a.getName();
        expression = new Predicate(a.getExpression());
        definition = a.getRef() == null ? expression.getExpression().toDisplayString()
                : a.getRef().strip().replaceAll("\\s+", " ");
        this.position = position;
        varTypes = new ArrayList<>();
        varNames = a.getVarNames();
        for (String s : a.getVarTypes()) {
            CtTypeReference<?> r = Utils.getType(s.equals(klass) ? path : s, factory);
            varTypes.add(r);
        }
    }

    public String getName() {
        return name;
    }

    public List<CtTypeReference<?>> getTypes() {
        return varTypes;
    }

    public List<String> getVarNames() {
        return varNames;
    }

    /** The definition as written in the declaration, on one line */
    public String getDefinition() {
        return definition;
    }

    /** Position of the alias declaration */
    public SourcePosition getPosition() {
        return position;
    }

    public Predicate getClonedPredicate() {
        return expression.clone();
    }

    public AliasDTO createAliasDTO() {
        return new AliasDTO(name, varTypes, varNames, expression.getExpression());
    }
}

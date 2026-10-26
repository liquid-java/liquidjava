package liquidjava.processor.facade;

import java.util.List;
import java.util.stream.Collectors;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.parsing.RefinementsParser;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtTypeInformation;
import spoon.reflect.reference.CtTypeReference;

public class AliasDTO {
    private final String name;
    private final List<String> varTypes;
    private final List<String> varNames;
    private Expression expression;
    private String ref;

    public AliasDTO(String name, List<CtTypeReference<?>> varTypes, List<String> varNames, Expression expression) {
        super();
        this.name = name;
        this.varTypes = varTypes.stream().map(CtTypeInformation::getQualifiedName).collect(Collectors.toList());
        this.varNames = varNames;
        this.expression = expression;
    }

    public AliasDTO(String name2, List<String> varTypes2, List<String> varNames2, String ref) {
        super();
        this.name = name2;
        this.varTypes = varTypes2;
        this.varNames = varNames2;
        this.ref = ref;
    }

    // Parse the alias expression using the given the prefix to ensure ghost names are qualified consistently with
    // where the alias is declared or used
    public void parse(String prefix) throws LJError {
        if (ref != null) {
            this.expression = RefinementsParser.createAST(ref, prefix);
        }
    }

    // Resolve Type.CONST references to static final constants, using the imports of the class declaring the alias
    public void resolveStaticFinalConstants(CtElement context, SourcePosition position) throws LJError {
        if (expression != null) {
            this.expression = Predicate.resolveStaticFinalConstants(expression, context, position);
        }
    }

    public String getName() {
        return name;
    }

    public List<String> getVarTypes() {
        return varTypes;
    }

    public List<String> getVarNames() {
        return varNames;
    }

    public Expression getExpression() {
        return expression;
    }
}

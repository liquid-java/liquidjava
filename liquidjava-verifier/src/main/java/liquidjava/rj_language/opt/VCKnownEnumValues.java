package liquidjava.rj_language.opt;

import static liquidjava.rj_language.opt.VCSimplificationUtils.copyWithRefinement;
import static liquidjava.rj_language.opt.VCSimplificationUtils.factsOf;

import java.util.HashMap;
import java.util.Map;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Enum;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.Var;
import liquidjava.utils.constants.Formats;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtEnumValue;

/**
 * Decides the comparisons of a variable with an enum constant that a fact about the variable already decides: under
 * {@code x == A}, {@code x == A} is true and {@code x == B} false, and under {@code x != A}, {@code x == A} is false,
 * so the ternaries they test can fold. Only the constants of the enum types of the chain's binders are used, which the
 * solver translates as distinct values, unlike {@code static final} constants that may share a value
 */
public class VCKnownEnumValues implements VCSimplificationPass {

    /**
     * A fact {@code variable == constant}, or {@code variable != constant} when not {@code equal}
     */
    private record KnownValue(Expression fact, String variable, String constant, boolean equal) {
    }

    /** The enum type of each enum constant, by the name the solver translates the constant from */
    private final Map<String, String> enumTypes = new HashMap<>();

    public VCKnownEnumValues(VCImplication implication) {
        for (VCImplication row = implication; row != null; row = row.getNext())
            if (row.getType() != null && row.getType().getDeclaration()instanceof CtEnum<?> enumDecl)
                for (CtEnumValue<?> value : enumDecl.getEnumValues())
                    enumTypes.put(String.format(Formats.ENUM, enumDecl.getSimpleName(), value.getSimpleName()),
                            enumDecl.getQualifiedName());
    }

    /**
     * Decides the comparisons that the first fact deciding any decides
     */
    @Override
    public VCImplication apply(VCImplication implication) {
        for (VCImplication row = implication; row != null; row = row.getNext())
            for (Expression fact : factsOf(row.getRefinement().getExpression())) {
                KnownValue known = knownValue(fact);
                if (known == null)
                    continue;
                // the last row may be the conclusion, which the other rows cannot assume
                boolean onlyLastRow = row.getNext() == null && row != implication;
                VCImplication decided = decide(implication, known, onlyLastRow);
                if (!decided.equals(implication))
                    return decided;
            }
        return implication;
    }

    private VCImplication decide(VCImplication implication, KnownValue known, boolean onlyLastRow) {
        if (implication == null)
            return null;
        Expression expression = implication.getRefinement().getExpression();
        boolean decides = !onlyLastRow || implication.getNext() == null;
        VCImplication result = copyWithRefinement(implication,
                new Predicate(decides ? decide(expression, known) : expression.clone()));
        result.setNext(decide(implication.getNext(), known, onlyLastRow));
        return result;
    }

    // The expression with the comparisons known decides replaced by their value, except for its own fact
    private Expression decide(Expression expression, KnownValue known) {
        if (expression == known.fact())
            return expression.clone();
        KnownValue compared = knownValue(expression);
        if (compared != null && compared.variable().equals(known.variable())
                && enumTypes.get(compared.constant()).equals(enumTypes.get(known.constant()))) {
            boolean sameConstant = compared.constant().equals(known.constant());
            if (known.equal())
                return new LiteralBoolean(sameConstant == compared.equal());
            if (sameConstant)
                return new LiteralBoolean(!compared.equal());
        }
        Expression result = expression.clone();
        for (int i = 0; i < expression.getChildren().size(); i++)
            result.setChild(i, decide(expression.getChildren().get(i), known));
        return result;
    }

    // A comparison of a variable with an enum constant, either way round, or null
    private KnownValue knownValue(Expression expression) {
        if (!(expression instanceof BinaryExpression binary))
            return null;
        boolean equal = "==".equals(binary.getOperator());
        if (!equal && !"!=".equals(binary.getOperator()))
            return null;
        Expression left = binary.getFirstOperand(), right = binary.getSecondOperand();
        String leftConstant = enumConstant(left), rightConstant = enumConstant(right);
        if (left instanceof Var variable && leftConstant == null && rightConstant != null)
            return new KnownValue(expression, variable.getName(), rightConstant, equal);
        if (right instanceof Var variable && rightConstant == null && leftConstant != null)
            return new KnownValue(expression, variable.getName(), leftConstant, equal);
        return null;
    }

    // The name of the enum constant an expression is, written in code or in a refinement, or null
    private String enumConstant(Expression expression) {
        String name = null;
        if (expression instanceof Var var)
            name = var.getName();
        else if (expression instanceof Enum en && en.getResolvedLiteral() == null)
            name = en.toString();
        return enumTypes.containsKey(name) ? name : null;
    }
}

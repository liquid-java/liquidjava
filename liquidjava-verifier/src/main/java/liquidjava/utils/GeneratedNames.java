package liquidjava.utils;

import java.util.regex.Pattern;

import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtUnaryOperator;

/**
 * Names of the values the verifier introduces, such as path conditions and call results. Each is written
 * {@code #<what the value is>_<counter>}, so that error messages say what the value stands for; the counter keeps it
 * unique and {@code #} keeps it apart from the variables of the program.
 */
public final class GeneratedNames {
    public static final String IF_CONDITION = "if_condition";
    public static final String LOOP_CONDITION = "loop_condition";
    public static final String TERNARY_CONDITION = "ternary_condition";
    public static final String TRY_BRANCH = "try_branch";
    public static final String CATCH_BRANCH = "catch_branch";
    public static final String ARRAY_LENGTH = "array_length";

    private static final String RESULT = "_result";
    private static final String VALUE = "_value";
    private static final String NEW = "new_";
    private static final String OPERATION_RESULT = "operation" + RESULT;
    private static final String EXPRESSION_VALUE = "expression" + VALUE;

    private static final Pattern GENERATED = Pattern
            .compile("^#(" + NEW + "\\w+|\\w+_(condition|branch|length|result|value))_[0-9]+$");
    private static final Pattern NOT_IN_IDENTIFIER = Pattern.compile("[^A-Za-z0-9_]");

    private GeneratedNames() {
    }

    public static String of(String description, int counter) {
        return String.format("#%s_%d", NOT_IN_IDENTIFIER.matcher(description).replaceAll("_"), counter);
    }

    /**
     * Names the value of an expression after what it is: {@code #size_result_3} for a call to {@code size},
     * {@code #count_value_3} for a read of field {@code count}, {@code #new_File_3} for a new {@code File}
     */
    public static String valueOf(CtExpression<?> expression, int counter) {
        return of(describe(expression), counter);
    }

    public static boolean isGenerated(String name) {
        return GENERATED.matcher(name).matches();
    }

    private static String describe(CtExpression<?> expression) {
        if (expression instanceof CtInvocation<?> invocation)
            return invocation.getExecutable().getSimpleName() + RESULT;
        if (expression instanceof CtConstructorCall<?> constructorCall)
            return NEW + constructorCall.getType().getSimpleName();
        if (expression instanceof CtFieldRead<?> fieldRead)
            return fieldRead.getVariable().getSimpleName() + VALUE;
        if (expression instanceof CtBinaryOperator<?> || expression instanceof CtUnaryOperator<?>)
            return OPERATION_RESULT;
        return EXPRESSION_VALUE;
    }
}

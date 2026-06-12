package liquidjava.rj_language.opt;

import static liquidjava.utils.VCTestUtils.vc;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import liquidjava.processor.VCImplication;

public class VCImplicationGenerator extends Generator<VCImplication> {

    static final String[] BINDERS = { "x", "y", "z" };
    static final String[] FREE_VARS = { "a", "b", "c" };
    private static final String[] COMPARISON_OPS = { "==", "!=", ">=", ">", "<=", "<" };

    public VCImplicationGenerator() {
        super(VCImplication.class);
    }

    @Override
    public VCImplication generate(SourceOfRandomness random, GenerationStatus status) {
        return switch (random.nextInt(0, 5)) {
        case 0 -> vc(substitution(random, "x"), comparison(random, "x"));
        case 1 -> vc(reverseSubstitution(random, "x"), comparison(random, "x"));
        case 2 -> vc(nonSubstitution(random, "x"), substitution(random, "y"), comparison(random, "y"));
        case 3 -> vc(substitution(random, "x"), dependentSubstitution(random), comparison(random, "y"));
        case 4 -> vc("∀y:int. true", "∀x:int. x == y + 1", comparison(random, "x"));
        default -> vc(substitution(random, "x"), substitution(random, "y"), comparison(random, "z"));
        };
    }

    private static String substitution(SourceOfRandomness random, String binder) {
        String value = value(random);
        if (random.nextBoolean())
            return "∀" + binder + ":int. " + binder + " == " + value;
        return "∀" + binder + ":int. " + value + " == " + binder;
    }

    private static String reverseSubstitution(SourceOfRandomness random, String binder) {
        return "∀" + binder + ":int. " + value(random) + " == " + binder;
    }

    private static String dependentSubstitution(SourceOfRandomness random) {
        int offset = random.nextInt(-3, 3);
        return "∀y:int. y == x " + signed(offset);
    }

    private static String nonSubstitution(SourceOfRandomness random, String binder) {
        if (random.nextBoolean())
            return "∀" + binder + ":int. " + binder + " > " + intLiteral(random);
        return "∀" + binder + ":int. " + binder + " == " + binder + " " + signed(random.nextInt(1, 5));
    }

    private static String comparison(SourceOfRandomness random, String preferredVar) {
        String left = random.nextBoolean() ? preferredVar : arithmetic(random, preferredVar);
        String right = random.nextBoolean() ? intLiteral(random)
                : arithmetic(random, FREE_VARS[random.nextInt(0, FREE_VARS.length - 1)]);
        return left + " " + COMPARISON_OPS[random.nextInt(0, COMPARISON_OPS.length - 1)] + " " + right;
    }

    private static String value(SourceOfRandomness random) {
        String var = FREE_VARS[random.nextInt(0, FREE_VARS.length - 1)];
        return switch (random.nextInt(0, 3)) {
        case 0 -> intLiteral(random);
        case 1 -> var;
        case 2 -> var + " " + signed(random.nextInt(-4, 4));
        default -> arithmetic(random, var);
        };
    }

    private static String arithmetic(SourceOfRandomness random, String var) {
        int constant = random.nextInt(-3, 3);
        return switch (random.nextInt(0, 2)) {
        case 0 -> var + " " + signed(constant);
        case 1 -> var + " * " + random.nextInt(1, 5);
        default -> "(" + var + " " + signed(constant) + ")";
        };
    }

    private static String signed(int value) {
        if (value < 0)
            return "- " + Math.abs(value);
        return "+ " + value;
    }

    private static String intLiteral(SourceOfRandomness random) {
        return Integer.toString(random.nextInt(-7, 7));
    }
}

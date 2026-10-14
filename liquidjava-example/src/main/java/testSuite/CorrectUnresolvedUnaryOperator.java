package testSuite;

import org.example.missing.Item;
import org.example.missing.Settings;

// A negation of a call whose type cannot be resolved is an unknown boolean (#389)
@SuppressWarnings("unused")
public class CorrectUnresolvedUnaryOperator {
    static void start(Settings settings) {
        if (!settings.enabled()) {
            return;
        }
    }

    static boolean negatedComparison(Item x) {
        return !(x.a() == x.b());
    }

    static int negated(Item x) {
        return -x.a();
    }
}

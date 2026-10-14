package testSuite;

import org.example.missing.Item;
import org.example.missing.Interrupts;

import liquidjava.specification.Refinement;

// Operations whose operand types cannot be resolved (classes not on the classpath) are unrefined (#388, #344)
@SuppressWarnings("unused")
public class CorrectUnresolvedBinaryOperator {
    static boolean same(Item a, Item b) {
        if (a.id() == b.id()) {
            return true;
        }
        return false;
    }

    static boolean comparedToConstant(Item p, int id) {
        return p.getValue() == id;
    }

    static void inLoop(Item embedding) {
        for (int i = 0; i < embedding.length(); i++) {
        }
    }

    static void withKnownOperand(boolean someHandlerDefined) {
        if (!someHandlerDefined || Interrupts.isInterruptsDisabled()) {
            return;
        }
    }

    static boolean nested(Item x) {
        return x.a() + 1 > 0;
    }

    static String concatenation(Item i) {
        return "Configuration is not an SQL config:" + i.name();
    }

    static void unknownVarNullCheck(Item x) {
        var v = x.a();
        if (v == null) {
            return;
        }
    }

    static void operatorAssignment(Item x) {
        int c = 0;
        c += x.a();
    }

    static void knownValuesStillUsed(Item x, int k) {
        if (k > 0 && x.ok()) {
            @Refinement("_ > 0")
            int y = 1;
        }
    }
}

package testSuite;

import liquidjava.specification.Refinement;

public class CorrectOperatorPrecedence {

    @Refinement("_ == 1 + 2 * 0")
    int multiplicationBeforeAddition() {
        return 1;
    }

    @Refinement("_ == (1 + 2) * 0")
    int groupedAdditionBeforeMultiplication() {
        return 0;
    }

    @Refinement("_ == 10 - 3 - 1")
    int subtractionAssociatesLeft() {
        return 6;
    }

    @Refinement("_ == 10 - (3 - 1)")
    int groupedSubtractionAssociatesRight() {
        return 8;
    }

    @Refinement("_ == -2 + 3")
    int unaryBeforeAddition() {
        return 1;
    }

    @Refinement("_ == (true || false && false)")
    boolean andBeforeOr() {
        return true;
    }

    @Refinement("_ == (!false && false)")
    boolean notBeforeAnd() {
        return false;
    }

    @Refinement("_ == (false --> true && false)")
    boolean andBeforeImplication() {
        return true;
    }

    @Refinement("_ == (true || true --> false)")
    boolean orBeforeImplication() {
        return false;
    }

    @Refinement("_ == (false --> false && false)")
    boolean anotherAndBeforeImplication() {
        return true;
    }

    @Refinement("_ == (false --> true --> false)")
    boolean implicationAssociatesRight() {
        return true;
    }

    @Refinement("_ == (true ? false : true ? false : true)")
    boolean ternaryAssociatesRight() {
        return false;
    }
}

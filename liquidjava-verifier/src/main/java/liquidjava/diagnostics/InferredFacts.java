package liquidjava.diagnostics;

import java.util.List;

import liquidjava.rj_language.ast.Expression;
import spoon.reflect.cu.SourcePosition;

/**
 * Facts that loop invariant inference gave the value of a variable, which an error shows among the others it found, for
 * clients that tell where they come from
 *
 * @param variable
 *            the internal name of the value, an instance of an {@code int} local variable the loop changes
 * @param facts
 *            the facts, each a conjunct of the refinement of {@code variable}
 * @param loop
 *            the position of the loop
 * @param point
 *            where in the loop the value is taken
 */
public record InferredFacts(String variable, List<Expression> facts, SourcePosition loop, Point point) {

    public enum Point {
        /** At the start of an iteration: the facts hold on every iteration */
        ITERATION_START,
        /** At the end of the body, where its continues join: the facts hold on every path that gets there */
        ITERATION_END,
        /** After a loop that exits only through its condition: the facts hold, and the condition is false */
        AFTER_LOOP
    }
}

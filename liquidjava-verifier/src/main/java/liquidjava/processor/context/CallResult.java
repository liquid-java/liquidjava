package liquidjava.processor.context;

import liquidjava.rj_language.Predicate;
import spoon.reflect.cu.SourcePosition;

/**
 * The value a call to a refined method returned
 *
 * @param function
 *            the method called
 * @param returned
 *            the method's return refinement at this call, with its arguments and receiver substituted and {@code _} for
 *            the value
 * @param position
 *            where the call is
 */
public record CallResult(RefinedFunction function, Predicate returned, SourcePosition position) {
}

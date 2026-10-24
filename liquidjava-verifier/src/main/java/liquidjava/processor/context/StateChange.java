package liquidjava.processor.context;

import java.util.Optional;

import spoon.reflect.cu.SourcePosition;

/**
 * The call that put an object in its current state
 *
 * @param function
 *            the method or constructor called
 * @param change
 *            the state refinement of {@code function} that applied
 * @param before
 *            the object before the call, or {@code null} for a constructor
 * @param position
 *            where the call is
 */
public record StateChange(RefinedFunction function, ObjectState change, VariableInstance before,
        SourcePosition position) {

    public Optional<VariableInstance> previous() {
        return Optional.ofNullable(before);
    }
}

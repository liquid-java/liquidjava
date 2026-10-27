package liquidjava.processor.refinement_checker.object_checkers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import liquidjava.processor.refinement_checker.object_checkers.ConstructorDelegation.Constant;
import liquidjava.processor.refinement_checker.object_checkers.ConstructorDelegation.Parameter;
import org.junit.jupiter.api.Test;

class ConstructorDelegationTest {

    private static final Constant UNNAMED = new Constant(Optional.empty());

    @Test
    void passesItsParametersOnUnchanged() {
        assertEquals(Optional.of(new ConstructorDelegation("java.lang.Throwable",
                List.of("java.lang.String", "java.lang.Throwable"), List.of(new Parameter(0), new Parameter(1)))),
                ConstructorDelegation.of("java.lang.Exception", List.of("java.lang.String", "java.lang.Throwable")));
    }

    @Test
    void passesNullCastToPickAnOverload() {
        // aload_0; aconst_null; checkcast Throwable; invokespecial ReflectiveOperationException(Throwable)
        assertEquals(
                Optional.of(new ConstructorDelegation("java.lang.ReflectiveOperationException",
                        List.of("java.lang.Throwable"), List.of(UNNAMED))),
                ConstructorDelegation.of("java.lang.ClassNotFoundException", List.of()));
    }

    @Test
    void passesAParameterAndAConstant() {
        assertEquals(
                Optional.of(new ConstructorDelegation("java.lang.ReflectiveOperationException",
                        List.of("java.lang.String", "java.lang.Throwable"), List.of(new Parameter(0), UNNAMED))),
                ConstructorDelegation.of("java.lang.ClassNotFoundException", List.of("java.lang.String")));
    }

    @Test
    void namesBooleanAndIntConstantsBySuperParameterType() {
        // ReferenceHandler(ThreadGroup g, String name) { super(g, null, name, 0, false); }
        Optional<ConstructorDelegation> d = ConstructorDelegation.of("java.lang.ref.Reference$ReferenceHandler",
                List.of("java.lang.ThreadGroup", "java.lang.String"));
        assertTrue(d.isPresent());
        assertEquals(List.of(new Parameter(0), UNNAMED, new Parameter(1), UNNAMED, new Constant(Optional.of("false"))),
                d.get().arguments());
    }

    @Test
    void rejectsAConstructorThatDoesMoreThanDelegate() {
        // super(s); this.className = className; this.key = key;
        assertEquals(Optional.empty(), ConstructorDelegation.of("java.util.MissingResourceException",
                List.of("java.lang.String", "java.lang.String", "java.lang.String")));
    }

    @Test
    void rejectsObjectAndUnknownConstructorsAndClasses() {
        assertEquals(Optional.empty(), ConstructorDelegation.of("java.lang.Object", List.of()));
        assertEquals(Optional.empty(), ConstructorDelegation.of("java.lang.Exception", List.of("int")));
        assertEquals(Optional.empty(), ConstructorDelegation.of("does.not.Exist", List.of()));
    }

    @Test
    void readsOnlyJdkClasses() {
        // on the test classpath, but not part of the JDK
        assertEquals(Optional.empty(), ConstructorDelegation.of(getClass().getName(), List.of()));
    }
}

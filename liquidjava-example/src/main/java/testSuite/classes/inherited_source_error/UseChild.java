package testSuite.classes.inherited_source_error;

public class UseChild {
    static void check() {
        Child child = new Child();
        child.change(1);
        child.use(); // Expect: State Refinement Error
    }

    static void superclassFallback() {
        new Child().inheritedPositive(-1); // Expect: Refinement Error
    }

    static void subclassOverride() {
        new Child().overridePositive(0);
    }
}

package testSuite.classes.inherited_source_correct;

public class UseChild {
    static void check() {
        Child child = new Child();
        child.change("unchanged");
        child.use();
    }

    static void superclassFallback() {
        new Child().inheritedPositive(1);
    }

    static void subclassOverride() {
        new Child().overridePositive(0);
    }
}

import liquidjava.specification.ExternalRefinementsFor;

class DefaultParent {
    void packageMethod() {}
}

class DefaultChild extends DefaultParent {}

@ExternalRefinementsFor("DefaultChild")
public interface DefaultChildRefinements {
    void packageMethod();
}

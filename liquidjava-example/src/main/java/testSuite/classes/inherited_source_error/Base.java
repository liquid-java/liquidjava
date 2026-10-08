package testSuite.classes.inherited_source_error;

public class Base {
    public void change(int value) {}
    public void change(String value) {}
    public void use() {}
    public void inheritedPositive(@liquidjava.specification.Refinement("_ > 0") int value) {}
    public void overridePositive(@liquidjava.specification.Refinement("_ > 0") int value) {}
}

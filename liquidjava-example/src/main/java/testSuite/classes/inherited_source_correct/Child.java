package testSuite.classes.inherited_source_correct;

public class Child extends Base {
    @Override
    public void overridePositive(@liquidjava.specification.Refinement("_ >= 0") int value) {}
}

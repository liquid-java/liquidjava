package testSuite.classes.state_field_initializer_correct;

import liquidjava.specification.Ghost;
import liquidjava.specification.StateRefinement;

public class StateFieldInitializer {

    private final Obj obj = new Obj();

    public void test() {
        obj.foo();
    }
}

@Ghost("boolean ready")
class Obj {

    @StateRefinement(to="ready(this)")
    Obj() {}

    @StateRefinement(from="ready(this)")
    void foo() {}
}

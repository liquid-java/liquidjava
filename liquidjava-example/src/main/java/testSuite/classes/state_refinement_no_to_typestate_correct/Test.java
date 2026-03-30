package testSuite.classes.state_refinement_no_to_typestate_correct;

public class Test {

    public void test() {
        Counter counter = new Counter();
        counter.addFirst();
        counter.addSecond();
        counter.inspect();
        counter.consumeOne();
    }
}

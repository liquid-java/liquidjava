package testSuite.classes.iterator_error;

public class Test {

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        Iterator i = new Iterator();
        int x = i.next(true); // State Refinement Error
    }
}

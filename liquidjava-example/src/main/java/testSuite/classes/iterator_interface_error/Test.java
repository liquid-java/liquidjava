package testSuite.classes.iterator_interface_error;

import java.util.ArrayList;
import java.util.Iterator;

public class Test {

    public static void main(String[] args) {
        ArrayList<Object> list = new ArrayList<>();
        list.add(new Object());
        Iterator<Object> it = list.iterator();
        it.remove(); // State Refinement Error
    }
}

package testSuite.classes.loop_list_index_correct;

import java.util.ArrayList;

// A loop over the indices of a list knows the index is non-negative (#479)
public class Client {
    public static void main(String[] args) {
        ArrayList<String> l = new ArrayList<>();
        l.add("a");
        l.add("b");
        for (int i = 0; i < l.size(); i++) {
            String s = l.get(i);
        }
    }
}

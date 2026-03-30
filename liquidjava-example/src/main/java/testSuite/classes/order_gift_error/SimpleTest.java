package testSuite.classes.order_gift_error;

import java.io.IOException;

public class SimpleTest {

    public static void main(String[] args) throws IOException {
        Order o = new Order();
        Order f = o.addItem("shirt", 60).getNewOrderPayThis().addItem("t", 6).addItem("t", 1);
        o.addGift();
        f.addItem("l", 1).addGift(); // State Refinement Error
    }
}

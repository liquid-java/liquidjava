package testingInProgress;

import liquidjava.specification.RefinementPredicate;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"empty", "addingItems", "checkout", "closed"})
@RefinementPredicate("int countItems(OrderSimple o)")
public class OrderSimple {

    @StateRefinement(to = "(countItems(this) == 0) && empty(this)")
    public OrderSimple() {}

    @StateRefinement(
            from = "(empty(this) || addingItems(this))",
            to = "((countItems(this) == (countItems(old(this)) + 1)) && addingItems(this))")
    //	@Refinement("_ == this")
    public OrderSimple addItem(String itemName, int price) {
        return this;
    }

    @StateRefinement(from = "((addingItems(this)) && (countItems(this) > 20))")
    public boolean hasGift() {
        return true;
    }

    //	@StateRefinement(from="addingItems(this)", to = "checkout(this)")
    //	public Order pay(int cardNumber) {
    //		return this;
    //	}

    //	@StateRefinement(from="checkout(this) && priceNow(this) > 20", to = "checkout(this)")
    //	public Order addGift() {
    //		return this;
    //	}
    //
    //	@StateRefinement(from="checkout(this)", to = "closed(this)")
    //	public Order sendToAddress(String a) {
    //		return this;
    //	}

}

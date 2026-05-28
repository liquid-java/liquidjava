package testSuite;

import liquidjava.specification.*;

@StateSet({"maybeEmpty", "hasNextElem", "empty"})
public class ErrorIteratorReturnHasNext {
    int index;
    int size;

    @StateRefinement(to = "maybeEmpty(this) && index() == 0 && size() == size")
    ErrorIteratorReturnHasNext(int size) {
        this.size = size;
        this.index = 0;
    }

    @StateRefinement(to = "return ? hasNextElem(this) : empty(this)")
    boolean hasNext() {
        return index < size;
    }

    @StateRefinement(from = "hasNextElem(this)", to = "maybeEmpty(this) && index() == index(old(this)) + 1 && size() == size(old(this))")
    int next() {
        index += 1;
        return index; // return index++; does not work
    }


    void main1() {
        ErrorIteratorReturnHasNext it = new ErrorIteratorReturnHasNext(5);
        if(it.hasNext()){
            it.next();
        } else {
            it.next(); // State Refinement Error
        }
    }

    void main2() {
        ErrorIteratorReturnHasNext it = new ErrorIteratorReturnHasNext(5);
        it.next(); // State Refinement Error
    }

    int main3() {
        ErrorIteratorReturnHasNext it = new ErrorIteratorReturnHasNext(5);
        int sum = 0;
        while (true){
            if(!it.hasNext()){
                sum += it.next(); // State Refinement Error
            } else {
                break;
            }
        }
        return sum;
    }
}

package testSuite;

import liquidjava.specification.*;

@StateSet({"maybeEmpty", "hasNextElem", "empty"})
public class CorrectIteratorReturnHasNext {

    int index;
    int size;

    @StateRefinement(to = "maybeEmpty(this) && index() == 0 && size() == size")
    CorrectIteratorReturnHasNext(int size) {
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

    int main1() {
        CorrectIteratorReturnHasNext it = new CorrectIteratorReturnHasNext(5);
        int sum = 0;
        while (true){
            if(it.hasNext()){
                sum += it.next();
            } else {
                break;
            }
        }
        return sum;
    }
}

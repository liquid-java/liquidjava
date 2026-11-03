package testSuite;

import liquidjava.specification.Refinement;

// writes to another object's field are checked as on main; only their display names the object (#457)
public class ErrorOtherObjectFieldWrite {
    @Refinement("_ >= 0")
    int level;

    void setOther(ErrorOtherObjectFieldWrite other) {
        other.level = -1; // Expect: Refinement Error
    }

    void incrementOther(ErrorOtherObjectFieldWrite other) {
        other.level++;
    }

    void addToOther(ErrorOtherObjectFieldWrite other) {
        other.level += 1;
    }

    void decrementOther(ErrorOtherObjectFieldWrite other) {
        other.level--; // Expect: Refinement Error
    }

    void writeThroughAlias() {
        level = 7;
        ErrorOtherObjectFieldWrite other = this;
        other.level = 3;
        @Refinement("_ == 7")
        int k = level; // Expect: Refinement Error
    }

    void setOwn() {
        this.level = -1; // Expect: Refinement Error
    }
}

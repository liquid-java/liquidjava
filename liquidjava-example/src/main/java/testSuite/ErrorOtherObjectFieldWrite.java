package testSuite;

import liquidjava.specification.Refinement;

// a write to another object's field is checked against the field's refinement under that object's name (#457)
public class ErrorOtherObjectFieldWrite {
    @Refinement("_ >= 0")
    int level;

    void setOther(ErrorOtherObjectFieldWrite other) {
        other.level = -1; // Expect: Refinement Error
    }

    void decrementOther(ErrorOtherObjectFieldWrite other) {
        other.level--; // Expect: Refinement Error
    }

    void setOwn() {
        this.level = -1; // Expect: Refinement Error
    }
}

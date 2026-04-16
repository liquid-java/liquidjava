package testSuite;

import liquidjava.specification.Refinement;

class CorrectEnumRefinement {
    enum Lever {
        Up, Down, Middle
    }

    public static void main(String[] args) {
        @Refinement("_==Lever.Up || _==Lever.Down")
        Lever lever = Lever.Up;
        System.out.println(lever);
    }
}

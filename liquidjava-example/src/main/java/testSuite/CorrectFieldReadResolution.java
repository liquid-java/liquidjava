package testSuite;

import liquidjava.specification.Refinement;

// #321, #322: field reads resolve to the field of the class and object they name
public class CorrectFieldReadResolution {
    enum Codec {
        JPG, PNG
    }

    static final int LIMIT = 10;

    @Refinement("val > 0")
    int val;

    int max = 5;

    public CorrectFieldReadResolution(@Refinement("_ > 0") int v) {
        val = v;
    }

    static void onlyPng(@Refinement("_ == Codec.PNG") Codec f) {}

    void otherClass() {
        @Refinement("_ == 10")
        int k = Limits.max;
        @Refinement("_ == 5")
        int m = max;
        @Refinement("_ == 5")
        int t = this.max;
    }

    void otherObject(CorrectFieldReadResolution other) {
        val = 7;
        @Refinement("_ > 0")
        int k = other.val;
        @Refinement("_ > 7")
        int s = val + other.val;
        if (other.val == 3) {
            @Refinement("_ == 7")
            int t = val;
        }
        @Refinement("_ == 7")
        int u = this.val;
    }

    static void sameNamedLocals() {
        Codec JPG = Codec.JPG;
        Codec PNG = Codec.PNG;
        onlyPng(PNG);
        onlyPng(Codec.PNG);
        Codec g = Codec.PNG;
        onlyPng(g);

        int LIMIT = 3;
        @Refinement("_ == 10")
        int k = CorrectFieldReadResolution.LIMIT;
        @Refinement("_ == 3")
        int l = LIMIT;
    }

    static class Limits {
        static final int max = 10;
    }
}

package testSuite;

import java.util.zip.ZipFile;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class CorrectBitwiseConstantArgument {
    static final int READ = 1;
    static final int DELETE = 4;

    static void open(@Refinement("_ == 1 || _ == 5") int mode) {
    }

    static void flags(@Refinement("_ >= 0") int f) {
    }

    public static void main(String[] args) {
        open(1 | 4); // literals fold to 5
        open(READ | DELETE); // static final constants
        open(ZipFile.OPEN_READ | ZipFile.OPEN_DELETE); // JDK constants, by reflection
        open((READ | DELETE) & 5); // nested
        open(20 >> 2 ^ 1 << 2); // shifts and xor: 5 ^ 4 = 1
        int mask = 3 << 1;
        mask |= 1; // operator assignment: the new value is unknown, nothing is claimed about it
        boolean a = true, b = false;
        boolean both = a & b; // non-short-circuit boolean operators are and/or/not-equal
        boolean either = a | b;
        boolean differ = a ^ b;
    }
}

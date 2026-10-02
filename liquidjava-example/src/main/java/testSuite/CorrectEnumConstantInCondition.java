package testSuite;

import java.time.DayOfWeek;

import javax.imageio.ImageWriteParam;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
class CorrectEnumConstantInCondition {
    enum Format {
        JPG, PNG, GIF
    }

    static class Limits {
        static final int max = 10; // lowercase: int fields get a generated @Ghost, which must be lowercase
        static int counter = 0; // not final: no known value
    }

    static void onlyJpg(@Refinement("f == Format.JPG") Format f) {}

    static void notJpg(@Refinement("f != Format.JPG") Format f) {}

    static void notGif(@Refinement("f != Format.GIF") Format f) {}

    static String extension(Format format) {
        if (format == Format.JPG) {
            return "jpg";
        }
        return "png";
    }

    static void thenBranch(Format format) {
        if (format == Format.JPG) {
            onlyJpg(format);
        }
    }

    static void elseBranch(Format format) {
        if (format == Format.JPG) {
            onlyJpg(format);
        } else {
            notJpg(format);
        }
    }

    static void notEquals(Format format) {
        if (format != Format.JPG) {
            notJpg(format);
        } else {
            onlyJpg(format);
        }
    }

    static void constantOnTheLeft(Format format) {
        if (Format.JPG == format) {
            onlyJpg(format);
        }
    }

    static void and(Format format, int n) {
        if (format == Format.JPG && n > 0) {
            onlyJpg(format);
            @Refinement("_ > 0")
            int m = n;
        }
    }

    static void or(Format format) {
        if (format == Format.JPG || format == Format.PNG) {
            notGif(format);
        }
    }

    static void staticFinalOfUserClass(int n) {
        if (n == Limits.max) {
            @Refinement("_ == 10")
            int m = n;
        }
    }

    static void jdkStaticFinal(int mode) {
        if (mode != ImageWriteParam.MODE_COPY_FROM_METADATA) {
            mode = ImageWriteParam.MODE_EXPLICIT;
        }
    }

    // a constant of an enum outside the analyzed sources carries no information, but does not crash
    static int jdkEnum(DayOfWeek day) {
        if (day == DayOfWeek.SUNDAY) {
            return 0;
        } else {
            return 1;
        }
    }

    // a constant without a known value carries no information, but does not crash
    static void unknownStaticField(int n) {
        if (n == Limits.counter) {
            n = 1;
        } else {
            n = 2;
        }
    }

    public static void main(String[] args) {
        extension(Format.PNG);
    }
}

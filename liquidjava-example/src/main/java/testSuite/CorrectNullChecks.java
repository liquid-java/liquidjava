package testSuite;

import java.util.ArrayList;
import java.util.Date;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class CorrectNullChecks {

    Integer x;

    void test() {
        if (x == null) {
            mustBeNull(x);
        } else {
            mustBeNotNull(x);
        }
    }

    void mustBeNull(@Refinement("_ == null") Integer i) {}
    void mustBeNotNull(@Refinement("_ != null") Integer i) {}

    void testNullInteger() {
        Integer i = null;

        @Refinement("_ == null")
        Integer i1 = i;

        i = 123;

        @Refinement("_ != null")
        Integer i2 = i;
    }

    void testNullString() {
        String s = null;

        @Refinement("_ == null")
        String s1 = s;

        s = "hello";

        @Refinement("_ != null")
        String s2 = s;
    }

    void testNulls() {
        @Refinement("_ == null")
        String s = null;

        @Refinement("_ == null")
        Integer i = null;

        @Refinement("_ == null")
        Boolean b = null;

        @Refinement("_ == null")
        Double d = null;

        @Refinement("_ == null")
        Long l = null;

        @Refinement("_ == null")
        Float f = null;

        @Refinement("_ == null")
        Date dt = null;

        @Refinement("_ == null")
        ArrayList<String> lst = null;        
    }

    void testNonNulls() {
        @Refinement("_ != null")
        String s = "hello";

        @Refinement("_ != null")
        Integer i = 123;

        @Refinement("_ != null")
        Boolean b = true;

        @Refinement("_ != null")
        Double d = 1.0;

        @Refinement("_ != null")
        Long l = 2L;

        @Refinement("_ != null")
        Float f = 1.0f;

        @Refinement("_ != null")
        Date dt = new Date();

        @Refinement("_ != null")
        ArrayList<String> lst = new ArrayList<>();

        @Refinement("_ != null")
        CorrectNullChecks r = this;
    }

    void testNullChecksInMethods() {
        @Refinement("_ != null")
        String x = returnNotNullIf(null);

        @Refinement("_ != null")
        String y = returnNotNullTernary(null);

        @Refinement("_ != null")
        String z = returnNotNullParam("not null");

        @Refinement("_ == null")
        String w = returnNull();
    }

    @Refinement("_ != null")
    String returnNotNullIf(String s) {
        if (s == null)
            s = "default";
    
        return s;
    }

    @Refinement("_ != null")
    String returnNotNullTernary(String s) {
        return s != null ? s : "default";
    }

    @Refinement("_ != null")
    String returnNotNullParam(@Refinement("_ != null") String s) {
        return s;
    }

    @Refinement("_ == null")
    String returnNull() {
        return null;
    }
}

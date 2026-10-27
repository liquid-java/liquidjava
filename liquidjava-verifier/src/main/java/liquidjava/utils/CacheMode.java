package liquidjava.utils;

import java.util.Locale;

/**
 * How the caches that reuse solver results across verification runs behave, set with the system property
 * {@value #PROPERTY}: {@code on} (the default), {@code off}, or {@code check}, which recomputes every result found in a
 * cache and fails if it differs, to test that the caches never change what the verifier reports
 */
public enum CacheMode {
    ON, OFF, CHECK;

    public static final String PROPERTY = "liquidjava.caches";

    public static CacheMode current() {
        return valueOf(System.getProperty(PROPERTY, "on").toUpperCase(Locale.ROOT));
    }

    /**
     * Fails when a cached result differs from the recomputed one. An {@link AssertionError}, so that code that recovers
     * from exceptions cannot hide it
     */
    public static void checkSame(String cache, Object cached, Object recomputed) {
        if (!cached.equals(recomputed))
            throw new AssertionError(
                    String.format("The %s cache returned %s but recomputing gives %s", cache, cached, recomputed));
    }
}

package liquidjava.utils;

import java.util.Locale;

/**
 * Whether the verifier reuses solver results from earlier verification runs, set with the system property
 * {@value #PROPERTY}: {@code on} (the default), {@code off}, or {@code check}, which recomputes every result found in
 * the cache and fails if it differs, to test that the cache never changes what the verifier reports
 */
public enum CacheMode {
    ON, OFF, CHECK;

    public static final String PROPERTY = "liquidjava.cache";

    public static CacheMode current() {
        return valueOf(System.getProperty(PROPERTY, "on").toUpperCase(Locale.ROOT));
    }
}

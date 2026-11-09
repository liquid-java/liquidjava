package liquidjava.utils.constants;

public final class Formats {
    public static final String INSTANCE = "#%s_%d";
    public static final String THIS = "this#%s";
    public static final String RET = "#ret_%d";
    public static final String ENUM = "%s.%s";
    /** The name of a state set declared without one, numbered from 1 in the order of the declarations */
    public static final String STATE_SET = "state%d";
    /** The names {@link #STATE_SET} gives, which a state set cannot be given explicitly */
    public static final String STATE_SET_NAMES = "state[1-9][0-9]*";
}
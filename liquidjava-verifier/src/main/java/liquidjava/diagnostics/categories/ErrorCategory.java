package liquidjava.diagnostics.categories;

/**
 * The kind of mistake an error shows, named after the categories of the LiquidJava bug codebook. A category describes
 * the program according to its specification, so it cannot tell whether the specification itself is wrong.
 */
public enum ErrorCategory {

    OUT_OF_ORDER("A1", "Out of order: the operation is legal for this object, but not right now"),
    OUT_OF_DOMAIN("A2", "Out of domain: the value is outside what is accepted"),
    ARGUMENT_PICKS_THE_STATE("C3.1", "Argument picks the state: the value passed decides where the object lands"),
    PATH_DEPENDENT_STATE("C4.1", "Path-dependent state: the state differs depending on which branch ran"),
    UNPROVABLE("C4.2", "Unprovable: nothing is known that could prove the requirement");

    private final String id;
    private final String title;

    ErrorCategory(String id, String title) {
        this.id = id;
        this.title = title;
    }

    /** The id of the category in the codebook */
    public String id() {
        return id;
    }

    public String title() {
        return title;
    }
}

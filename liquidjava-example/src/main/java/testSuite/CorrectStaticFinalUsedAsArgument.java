import liquidjava.specification.Refinement;

public class CorrectStaticFinalUsedAsArgument {
    private static final int BUFFER = 64;

    static void use(@Refinement("_ > 0") int n) {}

    public static void main(String[] args) {
        use(BUFFER);
    }
}

package liquidjava.smt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.z3.Expr;
import com.microsoft.z3.Status;
import liquidjava.processor.context.Context;
import liquidjava.rj_language.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import spoon.Launcher;

class ReferenceEqualityTest {
    private final Context context = Context.getInstance();

    @AfterEach
    void resetContext() {
        context.reinitializeAllContext();
    }

    @Test
    void referenceIdentityAgreesAcrossNativeAndAncestorViews() throws Exception {
        var factory = new Launcher().getFactory();
        context.addVarToContext("child", factory.Type().createReference(java.io.IOException.class), new Predicate(),
                factory.Code().createCodeSnippetStatement(""));
        context.addVarToContext("parent", factory.Type().createReference(Exception.class), new Predicate(),
                factory.Code().createCodeSnippetStatement(""));
        context.addVarToContext("ancestor", factory.Type().createReference(Throwable.class), new Predicate(),
                factory.Code().createCodeSnippetStatement(""));
        try (TranslatorToZ3 translator = new TranslatorToZ3(context)) {
            Expr<?> child = translator.makeVariable("child");
            Expr<?> parent = translator.makeVariable("parent");
            Expr<?> forward = translator.makeEquals(child, parent);
            Expr<?> reverse = translator.makeEquals(parent, child);
            assertEquals(Status.UNSATISFIABLE, translator
                    .makeSolverForExpression(translator.mkNot(translator.makeEquals(forward, reverse))).check());
            Expr<?> ancestor = translator.makeVariable("ancestor");
            Expr<?> childAncestor = translator.makeEquals(child, ancestor);
            Expr<?> parentAncestor = translator.makeEquals(parent, ancestor);
            for (Expr<?> equality : new Expr<?>[] { forward, reverse }) {
                Expr<?> sameAncestor = translator.makeAnd(childAncestor, parentAncestor);
                assertEquals(Status.UNSATISFIABLE, translator
                        .makeSolverForExpression(translator.makeAnd(sameAncestor, translator.mkNot(equality))).check());
                assertEquals(Status.UNSATISFIABLE,
                        translator.makeSolverForExpression(translator
                                .makeAnd(translator.makeAnd(equality, childAncestor), translator.mkNot(parentAncestor)))
                                .check());
                assertEquals(Status.UNSATISFIABLE,
                        translator
                                .makeSolverForExpression(translator.makeAnd(
                                        translator.makeAnd(equality, parentAncestor), translator.mkNot(childAncestor)))
                                .check());
            }
            assertEquals(Status.SATISFIABLE, translator.makeSolverForExpression(forward).check());
            assertEquals(Status.SATISFIABLE, translator.makeSolverForExpression(translator.mkNot(reverse)).check());
        }
    }
}

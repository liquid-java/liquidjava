package liquidjava.smt;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import liquidjava.processor.context.Context;
import liquidjava.processor.context.Variable;
import liquidjava.rj_language.Predicate;
import spoon.Launcher;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

class QueryCacheTest {

    private final Context context = Context.getInstance();
    private final Factory factory = new Launcher().getFactory();

    @AfterEach
    void resetContext() {
        context.reinitializeAllContext();
    }

    @Test
    void variablesAreRenamedInOrderOfAppearance() {
        addVariable("a", int.class);
        addVariable("b", int.class);
        addVariable("c", int.class);
        addVariable("d", int.class);
        QueryCache.Key ab = key("a > b");
        QueryCache.Key cd = key("c > d");
        assertEquals(ab.text(), cd.text());
        assertEquals(List.of("a", "b"), ab.names());
        assertEquals(List.of("c", "d"), cd.names());
        QueryCache.Key ba = key("b > a");
        assertEquals(ab.text(), ba.text());
        assertEquals(List.of("b", "a"), ba.names());
        assertNotEquals(ab.text(), key("a > a").text());
    }

    @Test
    void typesOfTheVariablesAreInTheKey() {
        addVariable("a", int.class);
        addVariable("b", long.class);
        assertNotEquals(key("a > 0").text(), key("b > 0").text());
    }

    @Test
    void supertypesOfTheVariablesAreInTheKey() {
        CtTypeReference<?> type = factory.Type().createReference(java.io.IOException.class);
        Variable plain = new Variable("plain", type, new Predicate());
        Variable withSupertypes = new Variable("withSupertypes", type, new Predicate());
        withSupertypes.addSuperTypes(factory.Type().createReference(Runnable.class), Set.of());
        context.addVarToContext(plain);
        context.addVarToContext(withSupertypes);
        assertNotEquals(key("plain == plain").text(), key("withSupertypes == withSupertypes").text());
    }

    private void addVariable(String name, Class<?> type) {
        context.addVarToContext(name, factory.Type().createReference(type), new Predicate(),
                factory.Code().createCodeSnippetStatement(""));
    }

    private QueryCache.Key key(String predicate) {
        return QueryCache.key(
                new Predicate(predicate, factory.Code().createCodeSnippetStatement(""), "").getExpression(), context);
    }
}

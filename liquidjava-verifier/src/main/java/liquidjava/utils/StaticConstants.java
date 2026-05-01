package liquidjava.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.LiteralString;
import liquidjava.utils.constants.Types;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.reference.CtFieldReference;

/** Resolution of {@code static final} primitive/String constants used in refinement predicates and field reads. */
public final class StaticConstants {

    private StaticConstants() {
    }

    /** Resolve a Spoon field reference to its compile-time constant value, source AST first then reflection. */
    public static Object resolve(CtFieldReference<?> ref) {
        if (!ref.isStatic() || !ref.isFinal())
            return null;
        CtField<?> decl = ref.getFieldDeclaration();
        if (decl != null && decl.getDefaultExpression()instanceof CtLiteral<?> lit && lit.getValue() != null)
            return lit.getValue();
        try {
            return ref.getActualField()instanceof Field jf ? readStaticFinal(jf) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Resolve a {@code TypeName.CONST_NAME} reference (as it appears inside a refinement predicate string) via
     * reflection. Tries {@code typeName} as-given, then {@code java.lang.}, then class names imported by
     * {@code context}'s compilation unit (single-type and on-demand imports). {@code context} may be {@code null}.
     */
    public static Object resolve(String typeName, String constName, CtElement context) {
        Object v = lookup(typeName, constName);
        if (v == null)
            v = lookup("java.lang." + typeName, constName);
        if (v != null || context == null || context.getPosition() == null || !context.getPosition().isValidPosition())
            return v;
        CtCompilationUnit cu = context.getPosition().getCompilationUnit();
        if (cu == null)
            return null;
        for (CtImport imp : cu.getImports()) {
            if (imp.getReference() == null)
                continue;
            String full = imp.getReference().toString();
            String candidate = full.endsWith("." + typeName) || full.equals(typeName) ? full
                    : full.endsWith(".*") ? full.substring(0, full.length() - 1) + typeName : null;
            if (candidate != null && (v = lookup(candidate, constName)) != null)
                return v;
        }
        return null;
    }

    /** Wrap a resolved value as an RJ literal predicate, or {@code null} if its type is not modeled. */
    public static Predicate asLiteralPredicate(Object value) {
        if (value instanceof Boolean)
            return Predicate.createLit(value.toString(), Types.BOOLEAN);
        if (value instanceof Integer || value instanceof Short || value instanceof Byte)
            return Predicate.createLit(value.toString(), Types.INT);
        if (value instanceof Long)
            return Predicate.createLit(value.toString(), Types.LONG);
        if (value instanceof Float)
            return Predicate.createLit(value.toString(), Types.FLOAT);
        if (value instanceof Double)
            return Predicate.createLit(value.toString(), Types.DOUBLE);
        if (value instanceof Character)
            return Predicate.createLit("'" + value + "'", Types.CHAR);
        if (value instanceof String s)
            return new Predicate(new LiteralString("\"" + s + "\""));
        return null;
    }

    /**
     * Speculative scan of common JDK packages for a class named {@code typeName} that has a {@code static final}
     * primitive/String field {@code constName}. Returns the fully-qualified class name (e.g.
     * {@code "javax.imageio.ImageWriteParam"}) of the first match, or {@code null} if no JDK match is found. Used to
     * suggest a missing import when {@link #resolve(String, String, CtElement)} fails.
     */
    public static String findJdkImportCandidate(String typeName, String constName) {
        for (String pkg : JDK_PACKAGE_HINTS) {
            String fqn = pkg + "." + typeName;
            if (lookup(fqn, constName) != null)
                return fqn;
        }
        return null;
    }

    private static final String[] JDK_PACKAGE_HINTS = { "java.io", "java.util", "java.util.concurrent",
            "java.util.regex", "java.nio", "java.nio.charset", "java.nio.file", "java.text", "java.time", "java.math",
            "java.net", "java.awt", "java.awt.event", "javax.swing", "javax.imageio", "javax.sound.sampled",
            "java.security", "java.sql" };

    private static Object lookup(String className, String fieldName) {
        try {
            return readStaticFinal(Class.forName(className).getField(fieldName));
        } catch (ClassNotFoundException | LinkageError | NoSuchFieldException | SecurityException ignored) {
            return null;
        }
    }

    private static Object readStaticFinal(Field jf) {
        if (!Modifier.isStatic(jf.getModifiers()) || !Modifier.isFinal(jf.getModifiers()))
            return null;
        try {
            jf.setAccessible(true);
            return jf.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }
}

package liquidjava.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.LiteralChar;
import liquidjava.rj_language.ast.LiteralString;
import liquidjava.utils.constants.Types;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtImportKind;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;

/** Resolution of {@code static final} primitive/String constants used in refinement predicates and field reads. */
public final class StaticConstants {

    private StaticConstants() {
    }

    /**
     * Resolve a Spoon {@code static final} field reference to its compile-time value, so reads like
     * {@code Integer.MAX_VALUE} or {@code MyConfig.LIMIT} fold to literals before SMT translation.
     *
     * <p>
     * Tries the source AST first ({@link CtLiteral} initializer in Spoon's model), then reflection via
     * {@link CtFieldReference#getActualField()} + {@link #readStaticFinal}. Returns {@code null} if the field isn't
     * static-final, has a non-literal initializer, or any lookup step fails.
     *
     * @see #resolve(String, String, CtElement) sibling for refinement-string {@code Type.CONST} references
     */
    public static Object resolve(CtFieldReference<?> ref) {
        if (!ref.isStatic() || !ref.isFinal())
            return null;
        CtField<?> decl = ref.getFieldDeclaration();
        Object v = sourceLiteralValue(decl);
        if (v != null)
            return v;
        try {
            return ref.getActualField()instanceof Field jf ? readStaticFinal(jf) : null;
        } catch (RuntimeException | LinkageError ignored) {
            // Spoon throws SpoonClassNotFoundException; reflection can throw LinkageError. Fall through.
            return null;
        }
    }

    /**
     * Resolve a {@code TypeName.CONST_NAME} reference (as it appears inside a refinement predicate string). Resolution
     * order matches Java scoping rules: fully-qualified/source-model name → explicit imports of {@code context}'s
     * compilation unit (single-type and on-demand) → implicit {@code java.lang}. {@code context} may be {@code null}.
     */
    public static Object resolve(String typeName, String constName, CtElement context) {
        Object v = lookupConstant(typeName, constName, context);
        if (v != null)
            return v;
        for (CtImport imp : localImports(context)) {
            String candidate = importCandidate(imp, typeName);
            if (candidate != null && (v = lookupConstant(candidate, constName, context)) != null)
                return v;
        }
        return lookup("java.lang." + typeName, constName);
    }

    /**
     * Look for a candidate class in any compilation unit's imports across the same Spoon model — useful for "did you
     * forget an import?" hints when the user already imported {@code typeName} in another file. Returns the
     * fully-qualified class name of the first match, or {@code null}.
     */
    public static String findImportCandidate(String typeName, String constName, CtElement context) {
        if (context == null || context.getFactory() == null)
            return null;
        for (CtCompilationUnit cu : context.getFactory().CompilationUnit().getMap().values()) {
            String fqn = findFqnInImports(typeName, constName, cu.getImports());
            if (fqn != null)
                return fqn;
        }
        return null;
    }

    /** Whether a {@link CtType} with the given simple name is present in {@code context}'s Spoon model. */
    public static boolean userTypeExists(String simpleName, CtElement context) {
        if (context == null || context.getFactory() == null)
            return false;
        for (CtType<?> t : context.getFactory().Type().getAll(true))
            if (simpleName.equals(t.getSimpleName()))
                return true;
        return false;
    }

    private static Iterable<CtImport> localImports(CtElement context) {
        if (context == null || context.getPosition() == null || !context.getPosition().isValidPosition())
            return java.util.Collections.emptyList();
        CtCompilationUnit cu = context.getPosition().getCompilationUnit();
        return cu == null ? java.util.Collections.emptyList() : cu.getImports();
    }

    private static String findFqnInImports(String typeName, String constName, Iterable<CtImport> imports) {
        for (CtImport imp : imports) {
            String candidate = importCandidate(imp, typeName);
            if (candidate != null && lookup(candidate, constName) != null)
                return candidate;
        }
        return null;
    }

    private static String importCandidate(CtImport imp, String typeName) {
        if (imp.getReference() == null)
            return null;
        // Use Spoon's typed APIs so nested classes get their binary name (Map$Entry, not Map.Entry).
        if (imp.getImportKind() == CtImportKind.TYPE && imp.getReference()instanceof CtTypeReference<?> tref) {
            String fqn = tref.getQualifiedName();
            if (fqn.equals(typeName) || fqn.endsWith("." + typeName) || fqn.endsWith("$" + typeName))
                return fqn;
            return null;
        }
        if (imp.getImportKind() == CtImportKind.ALL_TYPES && imp.getReference()instanceof CtPackageReference pref) {
            String pkg = pref.getQualifiedName();
            return pkg.isEmpty() ? typeName : pkg + "." + typeName;
        }
        return null; // FIELD / METHOD / ALL_STATIC_MEMBERS / UNRESOLVED — not relevant for type resolution.
    }

    private static Object lookupConstant(String typeName, String constName, CtElement context) {
        Object v = lookup(typeName, constName);
        return v != null ? v : lookupSource(typeName, constName, context);
    }

    private static Object lookupSource(String typeName, String constName, CtElement context) {
        if (context == null || context.getFactory() == null)
            return null;
        String currentPackage = packageName(context.getParent(CtType.class));
        for (CtType<?> t : context.getFactory().Type().getAll(true)) {
            boolean matches = typeName.equals(t.getQualifiedName())
                    || typeName.equals(t.getSimpleName()) && packageName(t).equals(currentPackage);
            if (matches) {
                Object v = sourceLiteralValue(t.getField(constName));
                if (v != null)
                    return v;
            }
        }
        return null;
    }

    private static Object sourceLiteralValue(CtField<?> field) {
        if (field == null || !field.isStatic() || !field.isFinal())
            return null;
        return field.getDefaultExpression()instanceof CtLiteral<?> lit ? lit.getValue() : null;
    }

    private static String packageName(CtType<?> type) {
        if (type == null || type.getPackage() == null)
            return "";
        return type.getPackage().getQualifiedName();
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
        if (value instanceof Character c)
            return new Predicate(new LiteralChar(c));
        if (value instanceof String s)
            return new Predicate(new LiteralString("\"" + s + "\""));
        return null;
    }

    /**
     * Reflectively read {@code className.fieldName} as a {@code public static final} constant — e.g.
     * {@code lookup("java.lang.Integer", "MAX_VALUE")} returns {@code 2147483647}, which is how
     * {@code Integer.MAX_VALUE} gets baked into the AST before SMT translation.
     *
     * <p>
     * Returns {@code null} on any failure ({@link ClassNotFoundException}, {@link NoSuchFieldException},
     * {@link LinkageError}, {@link SecurityException}, or non-static-final) so callers can fall through to the next
     * resolution strategy without a try/catch. Only public fields are visible to {@link Class#getField}.
     */
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
        } catch (IllegalAccessException | LinkageError ignored) {
            // LinkageError covers ExceptionInInitializerError, NoClassDefFoundError, etc.
            return null;
        }
    }
}

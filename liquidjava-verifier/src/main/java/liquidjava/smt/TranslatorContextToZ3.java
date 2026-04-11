package liquidjava.smt;

import com.microsoft.z3.Context;
import com.microsoft.z3.EnumSort;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FPExpr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Sort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import liquidjava.processor.context.AliasWrapper;
import liquidjava.processor.context.GhostFunction;
import liquidjava.processor.context.GhostState;
import liquidjava.processor.context.RefinedVariable;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.reference.CtTypeReference;

public class TranslatorContextToZ3 {

    static void translateVariables(Context z3, Map<String, CtTypeReference<?>> ctx,
            Map<String, Expr<?>> varTranslation) {

        translateEnumVariables(z3, ctx, varTranslation);

        for (Map.Entry<String, CtTypeReference<?>> entry : ctx.entrySet()) {
            String name = entry.getKey();
            CtTypeReference<?> type = entry.getValue();
            if (varTranslation.containsKey(name))
                continue;
            varTranslation.put(name, getExpr(z3, name, type));
        }

        varTranslation.put("true", z3.mkBool(true));
        varTranslation.put("false", z3.mkBool(false));
    }

    /**
     * Translates an enum type into a z3 EnumSort, caching it in enumSorts. Also registers enum literal constants in
     * varTranslation.
     */
    private static EnumSort<?> translateEnum(Context z3, Map<String, Expr<?>> varTranslation,
            Map<String, EnumSort<?>> enumSorts, CtTypeReference<?> type, CtEnum<?> enumDecl) {
        return enumSorts.computeIfAbsent(type.getQualifiedName(), k -> {
            String[] enumValueNames = enumDecl.getEnumValues().stream().map(ev -> ev.getSimpleName())
                    .toArray(String[]::new);
            EnumSort<?> enumSort = z3.mkEnumSort(k, enumValueNames);
            Expr<?>[] consts = enumSort.getConsts();
            for (int i = 0; i < enumValueNames.length; i++)
                varTranslation.put(enumDecl.getSimpleName() + "." + enumValueNames[i], consts[i]);
            return enumSort;
        });
    }

    public static void storeVariablesSubtypes(Context z3, List<RefinedVariable> variables,
            Map<String, List<Expr<?>>> varSuperTypes) {
        for (RefinedVariable v : variables) {
            if (!v.getSuperTypes().isEmpty()) {
                ArrayList<Expr<?>> a = new ArrayList<>();
                for (CtTypeReference<?> ctr : v.getSuperTypes())
                    a.add(getExpr(z3, v.getName(), ctr));
                varSuperTypes.put(v.getName(), a);
            }
        }
    }

    /**
     * Creates constants for enum variables in the context and adds them to the translation map if not already present
     */
    public static void translateEnumVariables(Context z3, Map<String, CtTypeReference<?>> ctx,
            Map<String, Expr<?>> varTranslation) {
        Map<String, EnumSort<?>> enumSorts = new HashMap<>();

        for (Map.Entry<String, CtTypeReference<?>> entry : ctx.entrySet()) {
            String name = entry.getKey();
            CtTypeReference<?> type = entry.getValue();
            if (type.isEnum() && type.getDeclaration()instanceof CtEnum<?> enumDecl) {
                EnumSort<?> enumSort = translateEnum(z3, varTranslation, enumSorts, type, enumDecl);
                // may already be registered as enum literal
                if (!varTranslation.containsKey(name)) {
                    varTranslation.put(name, z3.mkConst(name, enumSort));
                }
            }
        }
    }

    private static Expr<?> getExpr(Context z3, String name, CtTypeReference<?> type) {
        String typeName = type.getQualifiedName();

        return switch (typeName) {
        case "int", "short", "char", "java.lang.Integer", "java.lang.Short", "java.lang.Character" -> z3
                .mkIntConst(name);
        case "boolean", "java.lang.Boolean" -> z3.mkBoolConst(name);
        case "long", "java.lang.Long" -> z3.mkRealConst(name);
        case "float", "double", "java.lang.Float", "java.lang.Double" -> (FPExpr) z3.mkConst(name, z3.mkFPSort64());
        case "int[]" -> z3.mkArrayConst(name, z3.mkIntSort(), z3.mkIntSort());
        default -> z3.mkConst(name, z3.mkUninterpretedSort(typeName));
        };
    }

    static void addAliases(List<AliasWrapper> aliases, Map<String, AliasWrapper> aliasTranslation) {
        for (AliasWrapper a : aliases) {
            aliasTranslation.put(a.getName(), a);
        }
    }

    public static void addGhostFunctions(Context z3, List<GhostFunction> ghosts,
            Map<String, FuncDecl<?>> funcTranslation) {
        addBuiltinFunctions(z3, funcTranslation);
        if (!ghosts.isEmpty()) {
            for (GhostFunction gh : ghosts) {
                addGhostFunction(z3, gh, funcTranslation);
            }
        }
    }

    private static void addBuiltinFunctions(Context z3, Map<String, FuncDecl<?>> funcTranslation) {
        funcTranslation.put("length", z3.mkFuncDecl("length", getSort(z3, "int[]"), getSort(z3, "int"))); // ERRRRRRRRRRRRO!!!!!!!!!!!!!
        // Works only for int[] now! Change in the future
        // Ignore this message, it is a glorified TODO
        // TODO add built-in function
        Sort[] s = Stream.of(getSort(z3, "int[]"), getSort(z3, "int"), getSort(z3, "int")).toArray(Sort[]::new);
        funcTranslation.put("addToIndex", z3.mkFuncDecl("addToIndex", s, getSort(z3, "void")));

        s = Stream.of(getSort(z3, "int[]"), getSort(z3, "int")).toArray(Sort[]::new);
        funcTranslation.put("getFromIndex", z3.mkFuncDecl("getFromIndex", s, getSort(z3, "int")));
    }

    static Sort getSort(Context z3, String sort) {
        return switch (sort) {
        case "int", "short", "char", "java.lang.Integer", "java.lang.Short", "java.lang.Character" -> z3.getIntSort();
        case "boolean", "java.lang.Boolean" -> z3.getBoolSort();
        case "long", "java.lang.Long" -> z3.getRealSort();
        case "float", "java.lang.Float" -> z3.mkFPSort32();
        case "double", "java.lang.Double" -> z3.mkFPSortDouble();
        case "int[]" -> z3.mkArraySort(z3.mkIntSort(), z3.mkIntSort());
        case "String" -> z3.getStringSort();
        case "void" -> z3.mkUninterpretedSort("void");
        default -> z3.mkUninterpretedSort(sort);
        };
    }

    public static void addGhostStates(Context z3, List<GhostState> ghostState,
            Map<String, FuncDecl<?>> funcTranslation) {
        for (GhostState g : ghostState) {
            addGhostFunction(z3, g, funcTranslation);
        }
    }

    private static void addGhostFunction(Context z3, GhostFunction gh, Map<String, FuncDecl<?>> funcTranslation) {
        List<CtTypeReference<?>> paramTypes = gh.getParametersTypes();
        Sort ret = getSort(z3, gh.getReturnType().toString());
        Sort[] domain = paramTypes.stream().map(Object::toString).map(t -> getSort(z3, t)).toArray(Sort[]::new);
        String name = gh.getQualifiedName();
        funcTranslation.put(name, z3.mkFuncDecl(name, domain, ret));
    }
}

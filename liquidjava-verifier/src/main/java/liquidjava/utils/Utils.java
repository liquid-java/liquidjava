package liquidjava.utils;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import liquidjava.utils.constants.Types;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtTry;
import spoon.reflect.code.CtWhile;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

public class Utils {

    private static final Set<String> DEFAULT_NAMES = Set.of("old", "length", "addToIndex", "getFromIndex");

    public static CtTypeReference<?> getType(String type, Factory factory) {
        // TODO: complete with other types
        return switch (type) {
        case Types.INT -> factory.Type().INTEGER_PRIMITIVE;
        case Types.DOUBLE -> factory.Type().DOUBLE_PRIMITIVE;
        case Types.BOOLEAN -> factory.Type().BOOLEAN_PRIMITIVE;
        case Types.INT_LIST -> factory.createArrayReference(getType("int", factory));
        case Types.STRING -> factory.Type().STRING;
        case Types.LIST -> factory.Type().LIST;
        default -> factory.createReference(type);
        };
    }

    public static String getSimpleName(String name) {
        return name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : name;
    }

    public static String qualifyName(String prefix, String name) {
        if (DEFAULT_NAMES.contains(name) || prefix.isEmpty())
            return name; // dont qualify
        return String.format("%s.%s", prefix, name);
    }

    public static String getFile(CtElement element) {
        SourcePosition pos = element.getPosition();
        if (pos == null || pos.getFile() == null)
            return null;

        return pos.getFile().getAbsolutePath();
    }

    public static SourcePosition getLJAnnotationPosition(CtElement element, String refinement) {
        return element.getAnnotations().stream()
                .filter(a -> isLiquidJavaAnnotation(a) && hasRefinementValue(a, "\"" + refinement + "\"")).findFirst()
                .map(CtElement::getPosition).orElse(element.getPosition());
    }

    public static SourcePosition getFirstLJAnnotationPosition(CtElement element) {
        return element.getAnnotations().stream().filter(Utils::isLiquidJavaAnnotation).map(CtElement::getPosition)
                .min((p1, p2) -> {
                    if (p1.getLine() != p2.getLine())
                        return Integer.compare(p1.getLine(), p2.getLine());
                    return Integer.compare(p1.getColumn(), p2.getColumn());
                }).orElse(element.getPosition());
    }

    private static boolean isLiquidJavaAnnotation(CtAnnotation<?> annotation) {
        return annotation.getAnnotationType().getQualifiedName().startsWith("liquidjava.specification");
    }

    private static boolean hasRefinementValue(CtAnnotation<?> annotation, String refinement) {
        Map<String, ?> values = annotation.getValues();
        return Stream.of("value", "to", "from").anyMatch(key -> refinement.equals(String.valueOf(values.get(key))));
    }

    public static SourcePosition getRealPosition(CtElement element) {
        if (element instanceof CtBlock<?> block) {
            CtElement parent = block.getParent();
            if (parent instanceof CtIf ctIf) {
                if (ctIf.getThenStatement() == element) {
                    return ctIf.getPosition();
                }
            } else if (parent instanceof CtFor || parent instanceof CtForEach || parent instanceof CtWhile
                    || parent instanceof CtTry || parent instanceof CtMethod<?> || parent instanceof CtConstructor<?>) {
                return parent.getPosition();
            }
        }
        return element.getPosition();
    }
}

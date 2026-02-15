package liquidjava.utils;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import liquidjava.utils.constants.Types;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
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

    public static SourcePosition getAnnotationPosition(CtElement element, String refinement) {
        return element.getAnnotations().stream()
                .filter(a -> isLiquidJavaAnnotation(a) && hasRefinementValue(a, "\"" + refinement + "\"")).findFirst()
                .map(CtElement::getPosition).orElse(element.getPosition());
    }

    private static boolean isLiquidJavaAnnotation(CtAnnotation<?> annotation) {
        return annotation.getAnnotationType().getQualifiedName().startsWith("liquidjava.specification");
    }

    private static boolean hasRefinementValue(CtAnnotation<?> annotation, String refinement) {
        Map<String, ?> values = annotation.getValues();
        return Stream.of("value", "to", "from").anyMatch(key -> refinement.equals(String.valueOf(values.get(key))));
    }
}

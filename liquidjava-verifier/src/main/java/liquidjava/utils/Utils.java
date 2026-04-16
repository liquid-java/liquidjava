package liquidjava.utils;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
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
    private static final List<String> REFINEMENT_KEYS = List.of("value", "to", "from");

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

    // Get the position of the annotation with the given value
    public static SourcePosition getLJAnnotationPosition(CtElement element, String value) {
        String quotedValue = "\"" + value + "\"";
        return getLiquidJavaAnnotations(element)
                .flatMap(annotation -> getLJAnnotationValues(annotation)
                        .filter(expr -> quotedValue.equals(expr.toString()))
                        .map(expr -> expr.getPosition() != null ? expr.getPosition() : annotation.getPosition()))
                .findFirst().orElse(element.getPosition());
    }

    // Get the position of the first LJ annotation on the element
    public static SourcePosition getFirstLJAnnotationPosition(CtElement element) {
        return getLiquidJavaAnnotations(element).map(CtAnnotation::getPosition).filter(pos -> pos != null).findFirst()
                .orElse(element.getPosition());
    }

    // Get the position of the first value of the first LJ annotation on the element
    public static SourcePosition getFirstLJAnnotationValuePosition(CtElement element) {
        return getLiquidJavaAnnotations(element)
                .map(annotation -> getLJAnnotationValues(annotation).map(CtElement::getPosition)
                        .filter(pos -> pos != null).findFirst()
                        .orElse(annotation.getPosition() != null ? annotation.getPosition() : element.getPosition()))
                .findFirst().orElse(element.getPosition());
    }

    private static Stream<CtAnnotation<?>> getLiquidJavaAnnotations(CtElement element) {
        return element.getAnnotations().stream().filter(Utils::isLiquidJavaAnnotation);
    }

    private static boolean isLiquidJavaAnnotation(CtAnnotation<?> annotation) {
        return annotation.getAnnotationType().getQualifiedName().startsWith("liquidjava.specification");
    }

    private static Stream<CtElement> getLJAnnotationValues(CtAnnotation<?> annotation) {
        Map<String, ?> values = annotation.getAllValues();
        return REFINEMENT_KEYS.stream().map(values::get).filter(CtElement.class::isInstance).map(CtElement.class::cast);
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

    public static String getExpressionFromPosition(SourcePosition position) {
        if (position == null || position.getFile() == null)
            return null;
        try (Scanner scanner = new Scanner(position.getFile())) {
            int currentLine = 1;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (currentLine == position.getLine()) {
                    return line.substring(position.getColumn() - 2).trim();
                }
                currentLine++;
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}

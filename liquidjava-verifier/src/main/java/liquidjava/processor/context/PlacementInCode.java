package liquidjava.processor.context;

import java.lang.annotation.Annotation;

import liquidjava.utils.Utils;
import spoon.reflect.code.CtComment;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;

public class PlacementInCode {
    private final String text;
    private final SourcePosition position;
    private final SourcePosition annotationPosition;

    private PlacementInCode(String text, SourcePosition pos, SourcePosition annotationPosition) {
        this.text = text;
        this.position = pos;
        this.annotationPosition = annotationPosition;
    }

    public String getText() {
        return text;
    }

    public SourcePosition getPosition() {
        return position;
    }

    public SourcePosition getAnnotationPosition() {
        return annotationPosition;
    }

    public static PlacementInCode createPlacement(CtElement elem) {
        CtElement elemCopy = elem.clone();
        // cleanup annotations
        if (!elem.getAnnotations().isEmpty()) {
            for (CtAnnotation<? extends Annotation> a : elem.getAnnotations()) {
                elemCopy.removeAnnotation(a);
            }
        }
        // cleanup comments
        if (!elem.getComments().isEmpty()) {
            for (CtComment a : elem.getComments()) {
                elemCopy.removeComment(a);
            }
        }
        String elemText = elemCopy.toString();
        SourcePosition annotationPosition = Utils.getFirstLJAnnotationPosition(elem);
        return new PlacementInCode(elemText, elem.getPosition(), annotationPosition);
    }

    public String toString() {
        if (position.getFile() == null) {
            return "No position provided. Possibly asking for generated code";
        }
        return text + "  at:" + position.getFile().getName() + ":" + position.getLine() + ", " + position.getColumn();
    }
}

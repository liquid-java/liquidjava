package liquidjava.utils;

import spoon.reflect.reference.CtFieldReference;

public final class FieldNames {
    private FieldNames() {
    }

    public static String of(CtFieldReference<?> field) {
        return "this#" + field.getDeclaringType().getQualifiedName() + "." + field.getSimpleName();
    }
}

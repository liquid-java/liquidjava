package liquidjava.utils;

import spoon.reflect.reference.CtFieldReference;

public final class FieldNames {
    private FieldNames() {
    }

    public static String of(CtFieldReference<?> field) {
        StringBuilder name = new StringBuilder("this#");
        field.getDeclaringType().getQualifiedName().codePoints().forEach(c -> {
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_')
                name.appendCodePoint(c);
            else
                name.append('#').append(Integer.toHexString(c)).append('#');
        });
        return name.append('#').append(field.getSimpleName()).toString();
    }
}

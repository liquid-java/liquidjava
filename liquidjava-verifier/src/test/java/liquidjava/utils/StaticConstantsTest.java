package liquidjava.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import spoon.Launcher;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.visitor.filter.TypeFilter;

class StaticConstantsTest {
    @Test
    void resolvesInheritedLibraryConstant() {
        assertEquals(2, resolveRead(
                "class Repro { int mode = javax.imageio.plugins.jpeg.JPEGImageWriteParam.MODE_EXPLICIT; }"));
    }

    @Test
    void resolvesInheritedInterfaceConstant() {
        assertEquals(0, resolveRead("class Repro { int alignment = javax.swing.JLabel.CENTER; }"));
    }

    @Test
    void resolvesDeclaredLibraryConstant() {
        assertEquals(2, resolveRead("class Repro { int mode = javax.imageio.ImageWriteParam.MODE_EXPLICIT; }"));
    }

    @Test
    void resolvesPrivateSourceConstant() {
        assertEquals(3, resolveRead("class Repro { private static final int LIMIT = 3; int value = LIMIT; }"));
    }

    @Test
    void rejectsMutableSourceField() {
        assertNull(resolveRead("class Repro { static int LIMIT = 3; int value = LIMIT; }"));
    }

    @Test
    void rejectsInstanceSourceField() {
        assertNull(resolveRead("class Repro { final int LIMIT = 3; int value = LIMIT; }"));
    }

    private static Object resolveRead(String source) {
        var type = Launcher.parseClass(source);
        CtFieldRead<?> read = type.getElements(new TypeFilter<>(CtFieldRead.class)).get(0);
        return StaticConstants.resolve(read.getVariable());
    }
}

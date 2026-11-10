package liquidjava.diagnostics;

import spoon.reflect.cu.SourcePosition;

/**
 * A place in the source that a line of a diagnostic names, such as {@code line 134} in an origin note, for clients that
 * link to it
 *
 * @param message
 *            the whole line, a message note or a hint, as the diagnostic gives it
 * @param text
 *            the words of {@code message} that name the place, which occur in it once
 * @param file
 *            the path of the file
 * @param line
 *            the line, from 1
 * @param column
 *            the column, from 1
 */
public record SourceReference(String message, String text, String file, int line, int column) {

    public static SourceReference of(String message, String text, SourcePosition position) {
        return new SourceReference(message, text, position.getFile().getPath(), position.getLine(),
                position.getColumn());
    }
}

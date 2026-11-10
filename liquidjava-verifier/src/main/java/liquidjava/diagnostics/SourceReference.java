package liquidjava.diagnostics;

import spoon.reflect.cu.SourcePosition;

/**
 * A place in the source that a line of a diagnostic names, such as {@code line 134} in an origin note, for clients that
 * link to it
 *
 * @param message
 *            the whole line, a message note or a hint, as the diagnostic gives it
 * @param text
 *            the words of {@code message} that name the place
 * @param start
 *            where {@code text} starts in {@code message}, as an index of its characters
 * @param file
 *            the path of the file
 * @param line
 *            the line, from 1
 * @param column
 *            the column, from 1
 */
public record SourceReference(String message, String text, int start, String file, int line, int column) {

    /** {@code text} where it first occurs in {@code message}, naming {@code position} */
    public static SourceReference of(String message, String text, SourcePosition position) {
        return of(message, text, message.indexOf(text), position);
    }

    public static SourceReference of(String message, String text, int start, SourcePosition position) {
        return new SourceReference(message, text, start, position.getFile().getPath(), position.getLine(),
                position.getColumn());
    }
}

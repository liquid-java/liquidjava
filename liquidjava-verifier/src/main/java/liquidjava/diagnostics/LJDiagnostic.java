package liquidjava.diagnostics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import spoon.reflect.cu.SourcePosition;

public class LJDiagnostic extends RuntimeException {

    private final String title;
    private final String message;
    private final String accentColor;
    private String file;
    private ErrorPosition position;

    public LJDiagnostic(String title, String message, SourcePosition pos, String accentColor) {
        this.title = title;
        this.message = message;
        this.file = (pos != null && pos.getFile() != null) ? pos.getFile().getPath() : null;
        this.position = ErrorPosition.fromSpoonPosition(pos);
        this.accentColor = accentColor;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return ""; // to be overridden by subclasses
    }

    public ErrorPosition getPosition() {
        return position;
    }

    public void setPosition(SourcePosition pos) {
        if (pos == null)
            return;
        this.position = ErrorPosition.fromSpoonPosition(pos);
        this.file = pos.getFile().getPath();
    }

    public String getFile() {
        return file;
    }

    public String getAccentColor() {
        return accentColor;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // title
        sb.append("\n").append(accentColor).append(title).append(": ").append(Colors.RESET).append(message)
                .append("\n");

        // snippet
        String snippet = getSnippet();
        if (snippet != null) {
            sb.append(snippet);
        }

        // details
        String details = getDetails();
        if (!details.isEmpty()) {
            sb.append(" --> ").append(String.join("\n     ", details.split("\n"))).append("\n");
        }

        // location
        if (file != null && position != null) {
            sb.append("\n").append(file).append(":").append(position.lineStart()).append(Colors.RESET).append("\n");
        }

        return sb.toString();
    }

    public String getSnippet() {
        if (file == null || position == null)
            return null;

        Path path = Path.of(file);
        try {
            List<String> lines = Files.readAllLines(path);
            StringBuilder sb = new StringBuilder();

            // before and after lines for context
            int contextBefore = 2;
            int contextAfter = 2;
            int startLine = Math.max(1, position.lineStart() - contextBefore);
            int endLine = Math.min(lines.size(), position.lineEnd() + contextAfter);

            // calculate padding for line numbers
            int padding = String.valueOf(endLine).length();

            for (int i = startLine; i <= endLine; i++) {
                String lineNumStr = String.format("%" + padding + "d", i);
                String line = lines.get(i - 1);

                // add line
                sb.append(Colors.GREY).append(lineNumStr).append(" | ").append(line).append(Colors.RESET).append("\n");

                // add error markers on the line(s) with the error
                if (i >= position.lineStart() && i <= position.lineEnd()) {
                    int colStart = (i == position.lineStart()) ? position.colStart() : 1;
                    int colEnd = (i == position.lineEnd()) ? position.colEnd() : line.length();

                    if (colStart > 0 && colEnd > 0) {
                        // line number padding + " | " + column offset
                        String indent = " ".repeat(padding) + Colors.GREY + " | " + Colors.RESET
                                + " ".repeat(colStart - 1);
                        String markers = accentColor + "^".repeat(Math.max(1, colEnd - colStart + 1)) + Colors.RESET;
                        sb.append(indent).append(markers).append("\n");
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        LJDiagnostic other = (LJDiagnostic) obj;
        return title.equals(other.title) && message.equals(other.message)
                && ((file == null && other.file == null) || (file != null && file.equals(other.file)))
                && ((position == null && other.position == null)
                        || (position != null && position.equals(other.position)));
    }

    @Override
    public int hashCode() {
        int result = title.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + (file != null ? file.hashCode() : 0);
        result = 31 * result + (position != null ? position.hashCode() : 0);
        return result;
    }
}

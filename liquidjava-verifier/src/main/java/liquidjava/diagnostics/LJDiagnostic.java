package liquidjava.diagnostics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import spoon.reflect.cu.SourcePosition;

public class LJDiagnostic extends RuntimeException {

    private final String title;
    private final String message;
    private final String accentColor;
    private final String customMessage;
    private String file;
    private SourcePosition position;
    private String hint;
    private String counterexample;
    private static final String PIPE = " | ";

    public LJDiagnostic(String title, String message, SourcePosition pos, String accentColor, String customMessage) {
        this.title = title;
        this.message = message;
        this.file = (pos != null && pos.getFile() != null) ? pos.getFile().getPath() : null;
        this.position = pos;
        this.accentColor = accentColor;
        this.customMessage = customMessage;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getCustomMessage() {
        return customMessage;
    }

    public String getHint() {
        return hint;
    }

    public String getCounterexampleStr() {
        return counterexample;
    }

    public SourcePosition getPosition() {
        return position;
    }

    public SourcePosition getDeclarationPosition() {
        return null;
    }

    public void setPosition(SourcePosition pos) {
        if (pos == null || pos.getFile() == null)
            return;
        this.position = pos;
        this.file = pos.getFile().getPath();
    }

    public String getFile() {
        return file;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public void setCounterexampleStr(String counterexample) {
        this.counterexample = counterexample;
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

        // counterexample
        String counterexample = getCounterexampleStr();
        if (counterexample != null && !counterexample.isBlank()) {
            sb.append(" --> ").append(String.join("\n     ", counterexample.split("\n"))).append("\n");
        }

        // hint
        String hint = getHint();
        if (hint != null && !hint.isBlank()) {
            sb.append(" --> ").append(String.join("\n     ", hint.split("\n"))).append("\n");
        }

        // location
        if (file != null && position != null) {
            sb.append("\n").append(file).append(":").append(position.getLine()).append(Colors.RESET).append("\n");
        }

        // declaration position
        SourcePosition declPos = getDeclarationPosition();
        if (declPos != null && declPos.getFile() != null && !declPos.equals(position)) {
            sb.append(Colors.CYAN).append("\n--> Refinement declared here:\n").append(Colors.RESET);
            String declarationSnippet = getSnippet(declPos, 1, 0, Colors.CYAN, null, true);
            if (declarationSnippet != null)
                sb.append(declarationSnippet);

            sb.append(declPos.getFile().getPath()).append(":").append(declPos.getLine()).append(Colors.RESET)
                    .append("\n");
        }

        return sb.toString();
    }

    public String getSnippet() {
        return getSnippet(position, 2, 2, accentColor, customMessage, false);
    }

    private String getSnippet(SourcePosition snippetPosition, int contextBefore, int contextAfter, String markerColor,
            String markerMessage, boolean firstLineOnly) {
        if (snippetPosition == null || snippetPosition.getFile() == null)
            return null;

        Path path = snippetPosition.getFile().toPath();
        try {
            List<String> lines = Files.readAllLines(path);
            StringBuilder sb = new StringBuilder();

            int startLine = Math.max(1, snippetPosition.getLine() - contextBefore);
            int highlightedEndLine = firstLineOnly ? snippetPosition.getLine() : snippetPosition.getEndLine();
            int endLine = Math.min(lines.size(), highlightedEndLine + contextAfter);

            // calculate padding for line numbers
            int padding = String.valueOf(endLine).length();

            for (int i = startLine; i <= endLine; i++) {
                String lineNumStr = String.format("%" + padding + "d", i);
                String rawLine = lines.get(i - 1);
                String line = rawLine.replace("\t", "    ");

                // add line
                sb.append(Colors.GREY).append(lineNumStr).append(PIPE).append(line).append(Colors.RESET).append("\n");

                // add error markers on the line(s) with the error
                if (i >= snippetPosition.getLine() && i <= highlightedEndLine) {
                    int colStart = (i == snippetPosition.getLine()) ? snippetPosition.getColumn() : 1;
                    int colEnd = (i == snippetPosition.getEndLine()) ? snippetPosition.getEndColumn()
                            : rawLine.length();

                    if (colStart > 0 && colEnd > 0) {
                        int tabsBeforeStart = (int) rawLine.substring(0, Math.max(0, colStart - 1)).chars()
                                .filter(ch -> ch == '\t').count();
                        int tabsBeforeEnd = (int) rawLine.substring(0, Math.max(0, colEnd)).chars()
                                .filter(ch -> ch == '\t').count();
                        int visualColStart = colStart + tabsBeforeStart * 3;
                        int visualColEnd = colEnd + tabsBeforeEnd * 3;

                        // line number padding + pipe + column offset
                        String indent = " ".repeat(padding) + Colors.GREY + PIPE + Colors.RESET
                                + " ".repeat(visualColStart - 1);
                        String markers = markerColor + "^".repeat(Math.max(1, visualColEnd - visualColStart + 1));
                        sb.append(indent).append(markers);

                        // custom message
                        if (markerMessage != null && !markerMessage.isBlank()) {
                            String offset = " ".repeat(padding + visualColEnd + PIPE.length() + 1);
                            sb.append(" " + markerMessage.replace("\n", "\n" + offset));
                        }
                        sb.append(Colors.RESET).append("\n");
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
        return title.equals(other.title) && Objects.equals(message, other.message)
                && ((file == null && other.file == null) || (file != null && file.equals(other.file)))
                && ((position == null && other.position == null)
                        || (position != null && position.equals(other.position)));
    }

    @Override
    public int hashCode() {
        int result = title.hashCode();
        result = 31 * result + Objects.hashCode(message);
        result = 31 * result + (file != null ? file.hashCode() : 0);
        result = 31 * result + (position != null ? position.hashCode() : 0);
        return result;
    }
}

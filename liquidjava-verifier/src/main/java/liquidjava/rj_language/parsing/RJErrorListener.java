package liquidjava.rj_language.parsing;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

public class RJErrorListener implements ANTLRErrorListener {

    private int errors;
    public List<String> msgs;
    private String hint;

    public RJErrorListener() {
        super();
        errors = 0;
        msgs = new ArrayList<>();
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
            String msg, RecognitionException e) {
        if (hint == null) {
            String input = inputFor(offendingSymbol, e);
            int pos = charPositionInLine;
            if (input != null && pos >= 0 && pos < input.length()) {
                char c = input.charAt(pos);
                // Hint for == instead of =
                if (c == '=')
                    hint = "Predicates must be compared with == instead of =";
                // Hint for -> instead of --> (logical implication)
                else if (c == '>' && pos >= 1 && input.charAt(pos - 1) == '-'
                        && (pos < 2 || input.charAt(pos - 2) != '-'))
                    hint = "Logical implication is --> (two dashes), not ->";
            }
        }
        errors++;
        msgs.add("Error in " + msg + ", in the position " + charPositionInLine);
    }

    private static String inputFor(Object offendingSymbol, RecognitionException e) {
        if (e instanceof LexerNoViableAltException l)
            return l.getInputStream().toString();
        if (offendingSymbol instanceof Token t && t.getInputStream() != null)
            return t.getInputStream().toString();
        return null;
    }

    @Override
    public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact,
            BitSet ambigAlts, ATNConfigSet configs) {
    }

    @Override
    public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
            BitSet conflictingAlts, ATNConfigSet configs) {
    }

    @Override
    public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction,
            ATNConfigSet configs) {
    }

    public int getErrors() {
        return errors;
    }

    public String getHint() {
        return hint;
    }

    public String getMessages() {
        StringBuilder sb = new StringBuilder();
        String pl = errors == 1 ? "" : "s";
        sb.append("Found ").append(errors).append(" error").append(pl).append(", with the message").append(pl)
                .append(":\n");
        for (String s : msgs)
            sb.append("* ").append(s).append("\n");
        return sb.toString();
    }
}

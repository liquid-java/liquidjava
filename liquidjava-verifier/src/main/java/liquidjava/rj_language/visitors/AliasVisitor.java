package liquidjava.rj_language.visitors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import liquidjava.processor.facade.AliasDTO;
import liquidjava.utils.Pair;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import rj.grammar.RJParser.AliasContext;
import rj.grammar.RJParser.ArgDeclIDContext;
import rj.grammar.RJParser.PredContext;

public class AliasVisitor {
    /**
     * Gets information about the alias
     *
     * @param rc
     *
     * @return
     */
    public AliasDTO getAlias(ParseTree rc) {
        if (rc instanceof AliasContext ac) {
            String name = ac.ID_UPPER().getText();
            String ref = getText(ac.pred());
            List<Pair<String, String>> args = getArgsDecl(ac.argDeclID());

            List<String> varNames = args.stream().map(Pair::second).collect(Collectors.toList());
            List<String> varTypes = args.stream().map(Pair::first).collect(Collectors.toList());
            return new AliasDTO(name, varTypes, varNames, ref);

        } else if (rc.getChildCount() > 0) {
            int i = rc.getChildCount();
            if (i > 0) {
                return getAlias(rc.getChild(0));
            }
        }
        return null;
    }

    /**
     * Returns text with whitespaces
     *
     * @param pred
     *
     * @return
     */
    private String getText(PredContext pred) {
        int a = pred.start.getStartIndex();
        int b = pred.stop.getStopIndex();
        Interval interval = new Interval(a, b);
        return pred.start.getInputStream().getText(interval);
    }

    private List<Pair<String, String>> getArgsDecl(ArgDeclIDContext argDeclID) {
        List<Pair<String, String>> l = new ArrayList<>();
        auxGetArgsDecl(argDeclID, l);
        return l;
    }

    private void auxGetArgsDecl(ArgDeclIDContext argDeclID, List<Pair<String, String>> l) {
        String type = argDeclID.type().getText();
        String name = argDeclID.ID().getText();
        l.add(new Pair<>(type, name));
        if (argDeclID.argDeclID() != null)
            auxGetArgsDecl(argDeclID.argDeclID(), l);
    }
}

package liquidjava.rj_language.visitors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import liquidjava.processor.facade.GhostDTO;
import liquidjava.utils.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import rj.grammar.RJParser.ArgDeclContext;
import rj.grammar.RJParser.GhostContext;

public class GhostVisitor {

    public static GhostDTO getGhostDecl(ParseTree rc) {
        if (rc instanceof GhostContext gc) {
            String type = gc.type().getText();
            String name = gc.ID().getText();
            List<Pair<String, String>> args = getArgsDecl(gc.argDecl());
            List<String> ls = args.stream().map(Pair::first).collect(Collectors.toList());
            return new GhostDTO(name, ls, type);
        } else if (rc.getChildCount() > 0) {
            return getGhostDecl(rc.getChild(0));
        }
        return null;
    }

    private static List<Pair<String, String>> getArgsDecl(ArgDeclContext argDecl) {
        List<Pair<String, String>> l = new ArrayList<>();
        if (argDecl != null)
            auxGetArgsDecl(argDecl, l);
        return l;
    }

    private static void auxGetArgsDecl(ArgDeclContext argDecl, List<Pair<String, String>> l) {
        String type = argDecl.type().getText();
        String name = argDecl.ID() != null ? argDecl.ID().getText() : "";
        l.add(new Pair<>(type, name));
        if (argDecl.argDecl() != null)
            auxGetArgsDecl(argDecl.argDecl(), l);
    }
}

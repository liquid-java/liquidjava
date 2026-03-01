package liquidjava.rj_language.parsing;

import java.util.Optional;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.errors.SyntaxError;
import liquidjava.processor.facade.AliasDTO;
import liquidjava.processor.facade.GhostDTO;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.visitors.AliasVisitor;
import liquidjava.rj_language.visitors.CreateASTVisitor;
import liquidjava.rj_language.visitors.GhostVisitor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import rj.grammar.RJLexer;
import rj.grammar.RJParser;

public class RefinementsParser {

    public static Expression createAST(String toParse, String prefix) throws LJError {
        ParseTree pt = compile(toParse);
        CreateASTVisitor visitor = new CreateASTVisitor(prefix);
        return visitor.create(pt);
    }

    /**
     * The triple information of the ghost declaration in the order <type, name, list<type,name>>
     * 
     * @param s
     */
    public static GhostDTO getGhostDeclaration(String s) throws LJError {
        ParseTree rc = compile(s);
        GhostDTO g = GhostVisitor.getGhostDecl(rc);
        if (g == null)
            throw new SyntaxError("Invalid ghost declaration, expected e.g. @Ghost(\"int size\")", s);
        return g;
    }

    public static AliasDTO getAliasDeclaration(String s) throws LJError {
        CodePointCharStream input = CharStreams.fromString(s);
        RJErrorListener err = new RJErrorListener();
        RJLexer lexer = new RJLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(err);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        RJParser parser = new RJParser(tokens);
        parser.setBuildParseTree(true);
        parser.removeErrorListeners();
        parser.addErrorListener(err);

        RuleContext rc = parser.prog();
        AliasVisitor av = new AliasVisitor(input);
        AliasDTO alias = av.getAlias(rc);
        if (alias == null)
            throw new SyntaxError(
                    "Invalid alias definition, expected e.g. @RefinementAlias(\"Positive(int v) { v >= 0 }\")", s);
        return alias;
    }

    private static ParseTree compile(String toParse) throws LJError {
        Optional<String> s = getErrors(toParse);
        if (s.isPresent())
            throw new SyntaxError("Invalid refinement expression, expected a logical predicate", toParse);

        CodePointCharStream input = CharStreams.fromString(toParse);
        RJErrorListener err = new RJErrorListener();

        RJLexer lexer = new RJLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(err);

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        RJParser parser = new RJParser(tokens);
        parser.setBuildParseTree(true);
        parser.removeErrorListeners();
        parser.addErrorListener(err);
        return parser.prog();
    }

    /**
     * First passage to check if there are syntax errors
     *
     * @param toParse
     *
     * @return
     */
    private static Optional<String> getErrors(String toParse) {
        CodePointCharStream input = CharStreams.fromString(toParse);
        RJErrorListener err = new RJErrorListener();

        RJLexer lexer = new RJLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(err);

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        RJParser parser = new RJParser(tokens);
        parser.setBuildParseTree(true);
        parser.removeErrorListeners();
        parser.addErrorListener(err);
        parser.start(); // all consumed
        if (err.getErrors() > 0)
            return Optional.of(err.getMessages());
        return Optional.empty();
    }
}

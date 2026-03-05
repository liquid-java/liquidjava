package liquidjava.rj_language.parsing;

import java.util.Optional;

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
import org.antlr.v4.runtime.tree.ParseTree;
import rj.grammar.RJLexer;
import rj.grammar.RJParser;

public class RefinementsParser {

    /**
     * Parses a refinement expression from a string
     */
    public static Expression createAST(String toParse, String prefix) throws SyntaxError {
        ParseTree pt = compile(toParse, "Invalid refinement expression, expected a logical predicate");
        CreateASTVisitor visitor = new CreateASTVisitor(prefix);
        return visitor.create(pt);
    }

    /**
     * Parses the ghost declaration from the given string
     */
    public static GhostDTO parseGhostDeclaration(String toParse) throws SyntaxError {
        String errorMessage = "Invalid ghost declaration, expected e.g. @Ghost(\"int size\")";
        ParseTree rc = compile(toParse, errorMessage);
        GhostDTO g = GhostVisitor.getGhostDecl(rc);
        if (g == null)
            throw new SyntaxError(errorMessage, toParse);
        return g;
    }

    /**
     * Parses the alias declaration from the given string, throwing a SyntaxError if it is not valid
     */
    public static AliasDTO parseAliasDefinition(String toParse) throws SyntaxError {
        String errorMessage = "Invalid alias definition, expected e.g. @RefinementAlias(\"Positive(int v) { v >= 0 }\")";
        ParseTree rc = compile(toParse, errorMessage);
        AliasVisitor av = new AliasVisitor();
        AliasDTO alias = av.getAlias(rc);
        if (alias == null)
            throw new SyntaxError(errorMessage, toParse);
        return alias;
    }

    /**
     * Compiles the given string into a parse tree
     */
    private static ParseTree compile(String toParse, String errorMessage) throws SyntaxError {
        Optional<String> s = getErrors(toParse);
        if (s.isPresent())
            throw new SyntaxError(errorMessage, toParse);

        RJErrorListener err = new RJErrorListener();
        RJParser parser = createParser(toParse, err);
        return parser.prog();
    }

    /**
     * Checks if the given string can be parsed without syntax errors, returning the error messages if any
     */
    private static Optional<String> getErrors(String toParse) {
        RJErrorListener err = new RJErrorListener();
        RJParser parser = createParser(toParse, err);
        parser.start(); // all consumed
        if (err.getErrors() > 0)
            return Optional.of(err.getMessages());
        return Optional.empty();
    }

    /**
     * Creates a parser for the given input string and error listener
     */
    private static RJParser createParser(String toParse, RJErrorListener err) {
        CodePointCharStream input = CharStreams.fromString(toParse);
        RJLexer lexer = new RJLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(err);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        RJParser parser = new RJParser(tokens);
        parser.setBuildParseTree(true);
        parser.removeErrorListeners();
        parser.addErrorListener(err);
        return parser;
    }
}

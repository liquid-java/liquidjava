package liquidjava.rj_language.visitors;

import java.util.ArrayList;
import java.util.List;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.diagnostics.errors.SyntaxError;
import liquidjava.rj_language.ast.AliasInvocation;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.rj_language.ast.Ite;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.LiteralInt;
import liquidjava.rj_language.ast.LiteralReal;
import liquidjava.rj_language.ast.LiteralString;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;
import liquidjava.utils.Utils;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.NotImplementedException;
import rj.grammar.RJParser.AliasCallContext;
import rj.grammar.RJParser.ArgsContext;
import rj.grammar.RJParser.ExpBoolContext;
import rj.grammar.RJParser.ExpContext;
import rj.grammar.RJParser.ExpGroupContext;
import rj.grammar.RJParser.ExpOperandContext;
import rj.grammar.RJParser.FunctionCallContext;
import rj.grammar.RJParser.GhostCallContext;
import rj.grammar.RJParser.InvocationContext;
import rj.grammar.RJParser.IteContext;
import rj.grammar.RJParser.LitContext;
import rj.grammar.RJParser.LitGroupContext;
import rj.grammar.RJParser.LiteralContext;
import rj.grammar.RJParser.LiteralExpressionContext;
import rj.grammar.RJParser.OpArithContext;
import rj.grammar.RJParser.OpGroupContext;
import rj.grammar.RJParser.OpLiteralContext;
import rj.grammar.RJParser.OpMinusContext;
import rj.grammar.RJParser.OpNotContext;
import rj.grammar.RJParser.OpSubContext;
import rj.grammar.RJParser.OperandContext;
import rj.grammar.RJParser.PredContext;
import rj.grammar.RJParser.PredExpContext;
import rj.grammar.RJParser.PredGroupContext;
import rj.grammar.RJParser.PredLogicContext;
import rj.grammar.RJParser.PredNegateContext;
import rj.grammar.RJParser.ProgContext;
import rj.grammar.RJParser.StartContext;
import rj.grammar.RJParser.StartPredContext;
import rj.grammar.RJParser.TargetInvocationContext;
import rj.grammar.RJParser.VarContext;
import spoon.reflect.cu.SourcePosition;
import liquidjava.diagnostics.errors.ArgumentMismatchError;

/**
 * Create refinements language AST using antlr
 *
 * @author cgamboa
 */
public class CreateASTVisitor {

    String prefix;

    public CreateASTVisitor(String prefix) {
        this.prefix = prefix;
    }

    public Expression create(ParseTree rc) throws LJError {
        if (rc instanceof ProgContext)
            return progCreate((ProgContext) rc);
        else if (rc instanceof StartContext)
            return startCreate(rc);
        else if (rc instanceof PredContext)
            return predCreate(rc);
        else if (rc instanceof ExpContext)
            return expCreate(rc);
        else if (rc instanceof OperandContext)
            return operandCreate(rc);
        else if (rc instanceof LiteralExpressionContext)
            return literalExpressionCreate(rc);
        else if (rc instanceof FunctionCallContext)
            return functionCallCreate((FunctionCallContext) rc);
        else if (rc instanceof LiteralContext)
            return literalCreate((LiteralContext) rc);

        return null;
    }

    private Expression progCreate(ProgContext rc) throws LJError {
        if (rc.start() != null)
            return create(rc.start());
        return null;
    }

    private Expression startCreate(ParseTree rc) throws LJError {
        if (rc instanceof StartPredContext)
            return create(((StartPredContext) rc).pred());
        // alias and ghost do not have evaluation
        return null;
    }

    private Expression predCreate(ParseTree rc) throws LJError {
        if (rc instanceof PredGroupContext)
            return new GroupExpression(create(((PredGroupContext) rc).pred()));
        else if (rc instanceof PredNegateContext)
            return new UnaryExpression("!", create(((PredNegateContext) rc).pred()));
        else if (rc instanceof PredLogicContext)
            return new BinaryExpression(create(((PredLogicContext) rc).pred(0)),
                    ((PredLogicContext) rc).LOGOP().getText(), create(((PredLogicContext) rc).pred(1)));
        else if (rc instanceof IteContext)
            return new Ite(create(((IteContext) rc).pred(0)), create(((IteContext) rc).pred(1)),
                    create(((IteContext) rc).pred(2)));
        else
            return create(((PredExpContext) rc).exp());
    }

    private Expression expCreate(ParseTree rc) throws LJError {
        if (rc instanceof ExpGroupContext)
            return new GroupExpression(create(((ExpGroupContext) rc).exp()));
        else if (rc instanceof ExpBoolContext) {
            return new BinaryExpression(create(((ExpBoolContext) rc).exp(0)), ((ExpBoolContext) rc).BOOLOP().getText(),
                    create(((ExpBoolContext) rc).exp(1)));
        } else {
            ExpOperandContext eoc = (ExpOperandContext) rc;
            return create(eoc.operand());
        }
    }

    private Expression operandCreate(ParseTree rc) throws LJError {
        if (rc instanceof OpLiteralContext)
            return create(((OpLiteralContext) rc).literalExpression());
        else if (rc instanceof OpArithContext)
            return new BinaryExpression(create(((OpArithContext) rc).operand(0)),
                    ((OpArithContext) rc).ARITHOP().getText(), create(((OpArithContext) rc).operand(1)));
        else if (rc instanceof OpSubContext)
            return new BinaryExpression(create(((OpSubContext) rc).operand(0)), "-",
                    create(((OpSubContext) rc).operand(1)));
        else if (rc instanceof OpMinusContext)
            return new UnaryExpression("-", create(((OpMinusContext) rc).operand()));
        else if (rc instanceof OpNotContext)
            return new UnaryExpression("!", create(((OpNotContext) rc).operand()));
        else if (rc instanceof OpGroupContext)
            return new GroupExpression(create(((OpGroupContext) rc).operand()));
        assert false;
        return null;
    }

    private Expression literalExpressionCreate(ParseTree rc) throws LJError {
        if (rc instanceof LitGroupContext)
            return new GroupExpression(create(((LitGroupContext) rc).literalExpression()));
        else if (rc instanceof LitContext)
            return create(((LitContext) rc).literal());
        else if (rc instanceof VarContext) {
            return new Var(((VarContext) rc).ID().getText());
        } else if (rc instanceof TargetInvocationContext) {
            // TODO Finish Invocation with Target (a.len())
            return null;
        } else {
            return create(((InvocationContext) rc).functionCall());
        }
    }

    private Expression functionCallCreate(FunctionCallContext rc) throws LJError {
        if (rc.ghostCall() != null) {
            GhostCallContext gc = rc.ghostCall();
            String ref = gc.ID().getText();
            String name = Utils.qualifyName(prefix, ref);
            List<Expression> args = getArgs(gc.args());
            if (args.isEmpty())
                throw new ArgumentMismatchError("Ghost call cannot have empty arguments");
            return new FunctionInvocation(name, args);
        } else {
            AliasCallContext gc = rc.aliasCall();
            String ref = gc.ID_UPPER().getText();
            List<Expression> args = getArgs(gc.args());
            if (args.isEmpty())
                throw new ArgumentMismatchError("Alias call cannot have empty arguments");
            return new AliasInvocation(ref, args);
        }
    }

    private List<Expression> getArgs(ArgsContext args) throws LJError {
        List<Expression> le = new ArrayList<>();
        if (args != null)
            for (PredContext oc : args.pred()) {
                le.add(create(oc));
            }
        return le;
    }

    private Expression literalCreate(LiteralContext literalContext) throws LJError {
        if (literalContext.BOOL() != null)
            return new LiteralBoolean(literalContext.BOOL().getText());
        else if (literalContext.STRING() != null)
            return new LiteralString(literalContext.STRING().getText());
        else if (literalContext.INT() != null)
            return new LiteralInt(literalContext.INT().getText());
        else if (literalContext.REAL() != null)
            return new LiteralReal(literalContext.REAL().getText());
        throw new NotImplementedException("Literal type not implemented");
    }
}

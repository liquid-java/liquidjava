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
import liquidjava.rj_language.ast.LiteralLong;
import liquidjava.rj_language.ast.LiteralReal;
import liquidjava.rj_language.ast.LiteralString;
import liquidjava.rj_language.ast.LiteralNull;
import liquidjava.rj_language.ast.UnaryExpression;
import liquidjava.rj_language.ast.Var;
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Keys;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.NotImplementedException;
import rj.grammar.RJParser.AliasCallContext;
import rj.grammar.RJParser.ArgsContext;
import rj.grammar.RJParser.DotCallContext;
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
import rj.grammar.RJParser.VarContext;
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
        else if (rc instanceof DotCallContext)
            return dotCallCreate((DotCallContext) rc);
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
                args.add(new Var(Keys.THIS)); // implicit this: size() => this.size()

            return new FunctionInvocation(name, args);
        } else if (rc.aliasCall() != null) {
            AliasCallContext gc = rc.aliasCall();
            String ref = gc.ID_UPPER().getText();
            List<Expression> args = getArgs(gc.args());
            if (args.isEmpty())
                throw new ArgumentMismatchError("Alias call cannot have empty arguments");

            return new AliasInvocation(ref, args);
        } else {
            return dotCallCreate(rc.dotCall());
        }
    }

    /**
     * Handles both cases of dot calls: this.func(args) and targetFunc(this).func(args) Converts them to func(this,
     * args) and func(targetFunc(this), args) respectively
     */
    private Expression dotCallCreate(DotCallContext rc) throws LJError {
        if (rc.OBJECT_TYPE() != null) {
            String text = rc.OBJECT_TYPE().getText();

            // check if there are multiple fields (e.g. this.a.b)
            if (text.chars().filter(ch -> ch == '.').count() > 1)
                throw new SyntaxError("Multiple dot notation is not allowed", text);

            // this.func(args) => func(this, args)
            int dot = text.indexOf('.');
            String target = text.substring(0, dot);
            String simpleName = text.substring(dot + 1);
            String name = Utils.qualifyName(prefix, simpleName);
            List<Expression> args = getArgs(rc.args(0));
            if (!args.isEmpty() && args.get(0)instanceof Var v && v.getName().equals(Keys.THIS)
                    && target.equals(Keys.THIS))
                throw new SyntaxError("Cannot use both dot notation and explicit 'this' argument. Use either 'this."
                        + simpleName + "()' or '" + simpleName + "(this)'", text);

            args.add(0, new Var(target));
            return new FunctionInvocation(name, args);

        } else {
            // targetFunc(this).func(args) => func(targetFunc(this), args)
            String targetFunc = rc.ID(0).getText();
            String func = rc.ID(1).getText();
            String name = Utils.qualifyName(prefix, func);
            List<Expression> targetArgs = getArgs(rc.args(0));
            List<Expression> funcArgs = getArgs(rc.args(1));
            funcArgs.add(0, new FunctionInvocation(targetFunc, targetArgs));
            return new FunctionInvocation(name, funcArgs);
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
        else if (literalContext.INT() != null) {
            String text = literalContext.INT().getText();
            long value = Long.parseLong(text);
            return value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE ? new LiteralInt((int) value)
                    : new LiteralLong(value);
        } else if (literalContext.REAL() != null)
            return new LiteralReal(literalContext.REAL().getText());
        else if (literalContext.NULL() != null)
            return new LiteralNull();
        throw new NotImplementedException("Literal type not implemented");
    }
}

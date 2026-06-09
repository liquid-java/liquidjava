package liquidjava.processor.refinement_checker;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import liquidjava.diagnostics.DebugLog;
import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.context.*;
import liquidjava.processor.refinement_checker.general_checkers.MethodsFunctionsChecker;
import liquidjava.processor.refinement_checker.general_checkers.OperationsChecker;
import liquidjava.processor.refinement_checker.object_checkers.AuxStateHandler;
import liquidjava.rj_language.BuiltinFunctionPredicate;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Enum;
import liquidjava.utils.StaticConstants;
import liquidjava.utils.constants.Formats;
import liquidjava.utils.constants.Keys;
import liquidjava.utils.constants.Types;

import org.apache.commons.lang3.NotImplementedException;
import spoon.reflect.code.CtArrayRead;
import spoon.reflect.code.CtArrayWrite;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtBreak;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtContinue;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtThrow;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.support.reflect.code.CtVariableWriteImpl;

public class RefinementTypeChecker extends TypeChecker {
    // This class should do the following:
    // 1. Keep track of the context variable types
    // 2. Do type checking and inference

    // Auxiliary TypeCheckers
    OperationsChecker otc;
    MethodsFunctionsChecker mfc;
    Diagnostics diagnostics = Diagnostics.getInstance();
    ContextHistory contextHistory = ContextHistory.getInstance();

    public RefinementTypeChecker(Context context, Factory factory) {
        super(context, factory);
        otc = new OperationsChecker(this);
        mfc = new MethodsFunctionsChecker(this);
    }

    // --------------------- Visitors -----------------------------------

    /**
     * Under {@code -a} / {@code --all-context}, dump the context after every top-level statement. {@code super.scan}
     * runs the full {@code visitCtXxx} override first, so the statement's refinement effect is already applied to the
     * context by the time we save it. The {@code STATEMENT} role filter keeps this to actual statements (direct block /
     * case children), skipping sub-expressions like the {@code g(x)} in {@code f(g(x))}.
     */
    @Override
    public void scan(CtElement element) {
        super.scan(element);
        if (DebugLog.contextEnabled() && element instanceof CtStatement
                && element.getRoleInParent() == CtRole.STATEMENT) {
            contextHistory.saveContext(element, context);
        }
    }

    @Override
    public <T> void visitCtClass(CtClass<T> ctClass) {
        // System.out.println("CTCLASS:"+ctClass.getSimpleName());
        context.reinitializeContext();

        try {
            super.visitCtClass(ctClass);
        } catch (LJError e) {
            diagnostics.add(e);
        }

    }

    @Override
    public <T> void visitCtInterface(CtInterface<T> intrface) {
        // System.out.println("CT INTERFACE: " +intrface.getSimpleName());
        if (getExternalRefinement(intrface).isPresent()) {
            return;
        }
        try {
            super.visitCtInterface(intrface);
        } catch (LJError e) {
            diagnostics.add(e);
        }
    }

    @Override
    public <A extends Annotation> void visitCtAnnotationType(CtAnnotationType<A> annotationType) {
        super.visitCtAnnotationType(annotationType);
    }

    @Override
    public <T> void visitCtConstructor(CtConstructor<T> constructor) {
        context.enterContext();
        mfc.loadFunctionInfo(constructor);
        try {
            super.visitCtConstructor(constructor);
        } catch (LJError e) {
            diagnostics.add(e);
        }
        contextHistory.saveContext(constructor, context);
        context.exitContext();
    }

    public <R> void visitCtMethod(CtMethod<R> method) {
        context.enterContext();
        if (!method.getSignature().equals("main(java.lang.String[])")) {
            mfc.loadFunctionInfo(method);
        }
        try {
            super.visitCtMethod(method);
        } catch (LJError e) {
            diagnostics.add(e);
        }
        contextHistory.saveContext(method, context);
        context.exitContext();
    }

    @Override
    public <T> void visitCtLocalVariable(CtLocalVariable<T> localVariable) {
        super.visitCtLocalVariable(localVariable);
        // only declaration, no assignment
        if (localVariable.getAssignment() == null) {
            Optional<Predicate> a = getRefinementFromAnnotation(localVariable);
            RefinedVariable v = context.addVarToContext(localVariable.getSimpleName(), localVariable.getType(),
                    a.orElse(new Predicate()), localVariable);
            getMessageFromAnnotation(localVariable).ifPresent(v::setMessage);
        } else {
            String varName = localVariable.getSimpleName();
            CtExpression<?> e = localVariable.getAssignment();

            Predicate refinementFound = getRefinement(e);
            if (refinementFound == null) {
                refinementFound = new Predicate();
            }
            context.addVarToContext(varName, localVariable.getType(), new Predicate(), e);
            checkVariableRefinements(refinementFound, varName, localVariable.getType(), localVariable, localVariable);
            AuxStateHandler.addStateRefinements(this, varName, e);
        }
    }

    @Override
    public <T> void visitCtNewArray(CtNewArray<T> newArray) {
        super.visitCtNewArray(newArray);
        List<CtExpression<Integer>> l = newArray.getDimensionExpressions();
        // TODO only working for 1 dimension
        for (CtExpression<?> exp : l) {
            Predicate c = getExpressionRefinements(exp);
            String name = String.format(Formats.FRESH, context.getCounter());
            if (c.getVariableNames().contains(Keys.WILDCARD)) {
                c = c.substituteVariable(Keys.WILDCARD, name);
            } else {
                c = Predicate.createEquals(Predicate.createVar(name), c);
            }
            context.addVarToContext(name, factory.Type().INTEGER_PRIMITIVE, c, exp);
            Predicate ep;
            ep = Predicate.createEquals(BuiltinFunctionPredicate.length(Keys.WILDCARD, newArray),
                    Predicate.createVar(name));

            newArray.putMetadata(Keys.REFINEMENT, ep);
        }
    }

    @Override
    public <T> void visitCtThisAccess(CtThisAccess<T> thisAccess) {
        super.visitCtThisAccess(thisAccess);
        CtClass<?> c = thisAccess.getParent(CtClass.class);
        String s = c.getSimpleName();
        if (thisAccess.getParent() instanceof CtReturn) {
            String thisName = String.format(Formats.THIS, s);
            thisAccess.putMetadata(Keys.REFINEMENT,
                    Predicate.createEquals(Predicate.createVar(Keys.WILDCARD), Predicate.createVar(thisName)));
        }
    }

    @Override
    public <T, A extends T> void visitCtAssignment(CtAssignment<T, A> assignment) throws LJError {
        super.visitCtAssignment(assignment);
        visitAssignment(assignment);
    }

    @Override
    public <T, A extends T> void visitCtOperatorAssignment(CtOperatorAssignment<T, A> assignment) throws LJError {
        super.visitCtOperatorAssignment(assignment);
        visitAssignment(assignment);
    }

    /**
     * Handles simple and operator assignments after Spoon has visited their children
     */
    @SuppressWarnings("unchecked")
    private <T, A extends T> void visitAssignment(CtAssignment<T, A> assignment) throws LJError {
        CtExpression<T> ex = assignment.getAssigned();

        if (ex instanceof CtVariableWriteImpl) {
            CtVariableReference<?> var = ((CtVariableAccess<?>) ex).getVariable();
            CtVariable<T> varDecl = (CtVariable<T>) var.getDeclaration();
            String name = var.getSimpleName();
            checkAssignment(name, varDecl.getType(), ex, assignment.getAssignment(), assignment, varDecl);

        } else if (ex instanceof CtFieldWrite<?> fw) {
            CtFieldReference<?> cr = fw.getVariable();
            CtField<?> f = fw.getVariable().getDeclaration();
            String updatedVarName = String.format(Formats.THIS, cr.getSimpleName());
            checkAssignment(updatedVarName, cr.getType(), ex, assignment.getAssignment(), assignment, f);

            // corresponding ghost function update
            if (fw.getVariable().getType().toString().equals("int")) {
                AuxStateHandler.updateGhostField(fw, this);
            }
        }
    }

    @Override
    public <T> void visitCtArrayRead(CtArrayRead<T> arrayRead) {
        super.visitCtArrayRead(arrayRead);
        String name = String.format(Formats.INSTANCE, "arrayAccess", context.getCounter());
        context.addVarToContext(name, arrayRead.getType(), new Predicate(), arrayRead);
        arrayRead.putMetadata(Keys.REFINEMENT, Predicate.createVar(name));
        // TODO predicate for now is always TRUE
    }

    @Override
    public <T> void visitCtLiteral(CtLiteral<T> lit) {
        List<String> types = Arrays.asList(Types.IMPLEMENTED);
        String type = lit.getType().getQualifiedName();
        if (types.contains(type)) {
            lit.putMetadata(Keys.REFINEMENT, Predicate.createEquals(Predicate.createVar(Keys.WILDCARD),
                    Predicate.createLit(lit.getValue().toString(), type)));

        } else if (lit.getType().getQualifiedName().equals("java.lang.String")) {
            // Only taking care of strings inside refinements
        } else if (type.equals(Types.NULL)) {
            // Skip null literals
        } else {
            throw new NotImplementedException(
                    String.format("Literal of type %s not implemented:", lit.getType().getQualifiedName()));
        }
    }

    @Override
    public <T> void visitCtField(CtField<T> f) {
        super.visitCtField(f);
        Optional<Predicate> c = getRefinementFromAnnotation(f);
        String name = String.format(Formats.THIS, f.getSimpleName());
        Predicate ret = new Predicate();
        if (c.isPresent()) {
            ret = c.get().substituteVariable(Keys.WILDCARD, name).substituteVariable(f.getSimpleName(), name);
        }
        RefinedVariable v = context.addVarToContext(name, f.getType(), ret, f);
        getMessageFromAnnotation(f).ifPresent(v::setMessage);
        if (v instanceof Variable) {
            ((Variable) v).setLocation("this");
        }
    }

    @Override
    public <T> void visitCtFieldRead(CtFieldRead<T> fieldRead) {
        String fieldName = fieldRead.getVariable().getSimpleName();
        if (context.hasVariable(fieldName)) {
            RefinedVariable rv = context.getVariableByName(fieldName);
            if (rv instanceof Variable && ((Variable) rv).getLocation().isPresent()
                    && ((Variable) rv).getLocation().get().equals(fieldRead.getTarget().toString())) {
                fieldRead.putMetadata(Keys.REFINEMENT, context.getVariableRefinements(fieldName));
            } else {
                fieldRead.putMetadata(Keys.REFINEMENT,
                        Predicate.createEquals(Predicate.createVar(Keys.WILDCARD), Predicate.createVar(fieldName)));
            }

        } else if (context.hasVariable(String.format(Formats.THIS, fieldName))) {
            String thisName = String.format(Formats.THIS, fieldName);
            fieldRead.putMetadata(Keys.REFINEMENT,
                    Predicate.createEquals(Predicate.createVar(Keys.WILDCARD), Predicate.createVar(thisName)));

        } else if (fieldRead.getVariable().getSimpleName().equals("length")) {
            String targetName = fieldRead.getTarget().toString();
            fieldRead.putMetadata(Keys.REFINEMENT, Predicate.createEquals(Predicate.createVar(Keys.WILDCARD),
                    BuiltinFunctionPredicate.length(targetName, fieldRead)));
        } else if (fieldRead.getVariable().getDeclaringType().isEnum()) {
            String target = fieldRead.getVariable().getDeclaringType().getSimpleName();
            String enumLiteral = String.format(Formats.ENUM, target, fieldName);
            fieldRead.putMetadata(Keys.REFINEMENT,
                    Predicate.createEquals(Predicate.createVar(Keys.WILDCARD), Predicate.createVar(enumLiteral)));
        } else if (tryStaticFinalConstantRefinement(fieldRead)) {
            // refinement metadata set by helper
        } else {
            fieldRead.putMetadata(Keys.REFINEMENT, new Predicate());
            // TODO DO WE WANT THIS OR TO SHOW ERROR MESSAGE?
        }
        super.visitCtFieldRead(fieldRead);
    }

    /** Resolve a {@code static final} primitive/String constant to {@code #wild == Type.CONST}. */
    private <T> boolean tryStaticFinalConstantRefinement(CtFieldRead<T> fieldRead) {
        Predicate literal = StaticConstants.asLiteralPredicate(StaticConstants.resolve(fieldRead.getVariable()));
        if (literal == null)
            return false;
        Enum constant = new Enum(fieldRead.getVariable().getDeclaringType().getSimpleName(),
                fieldRead.getVariable().getSimpleName());
        constant.setResolvedLiteral(literal.getExpression());
        fieldRead.putMetadata(Keys.REFINEMENT,
                Predicate.createEquals(Predicate.createVar(Keys.WILDCARD), new Predicate(constant)));
        return true;
    }

    @Override
    public <T> void visitCtVariableRead(CtVariableRead<T> variableRead) {
        super.visitCtVariableRead(variableRead);
        CtVariable<T> varDecl = variableRead.getVariable().getDeclaration();
        getPutVariableMetadata(variableRead, varDecl.getSimpleName());
    }

    /**
     * Visitor for binary operations Adds metadata to the binary operations from the operands
     */
    @Override
    public <T> void visitCtBinaryOperator(CtBinaryOperator<T> operator) {
        super.visitCtBinaryOperator(operator);
        otc.getBinaryOpRefinements(operator);
    }

    @Override
    public <T> void visitCtUnaryOperator(CtUnaryOperator<T> operator) {
        super.visitCtUnaryOperator(operator);
        otc.getUnaryOpRefinements(operator);
    }

    public <R> void visitCtInvocation(CtInvocation<R> invocation) {
        super.visitCtInvocation(invocation);
        mfc.getInvocationRefinements(invocation);
    }

    @Override
    public <R> void visitCtReturn(CtReturn<R> ret) {
        super.visitCtReturn(ret);
        mfc.getReturnRefinements(ret);
    }

    @Override
    public void visitCtIf(CtIf ifElement) {
        CtExpression<Boolean> exp = ifElement.getCondition();
        Predicate expRefs = getExpressionRefinements(exp);

        String pathVarName = String.format(Formats.FRESH, context.getCounter());
        RefinedVariable freshRV;

        // When the condition's predicate uses Keys.WILDCARD as a stand-in for its boolean value (e.g. _ == true -->
        // state(this) or _ == k), the fresh path variable IS that value — assert it true in the then branch and false
        // in the else, since negating the whole predicate is unsound for implications and equality forms.
        boolean valueIsCondition = false;
        Predicate thenRefs;
        Predicate elseRefs;
        if (isUninformativeCondition(expRefs, exp)) {
            // No refinement means the condition is unknown, not true: model it as a fresh
            // boolean so the SMT solver may pick either truth value for each branch.
            expRefs = Predicate.createVar(pathVarName);
            thenRefs = expRefs;
            elseRefs = expRefs.negate();
            freshRV = context.addInstanceToContext(pathVarName, factory.Type().BOOLEAN_PRIMITIVE, new Predicate(), exp);
        } else {
            valueIsCondition = expRefs.getVariableNames().contains(Keys.WILDCARD);
            expRefs = expRefs.substituteVariable(Keys.WILDCARD, pathVarName);
            Predicate lastExpRefs = substituteAllVariablesForLastInstance(expRefs);
            expRefs = Predicate.createConjunction(expRefs, lastExpRefs);

            // TODO Change in future
            if (expRefs.getVariableNames().contains("null")) {
                expRefs = new Predicate();
                valueIsCondition = false;
            }

            thenRefs = expRefs;
            elseRefs = expRefs.negate();
            if (valueIsCondition) {
                Predicate freshIsTrue = Predicate.createEquals(Predicate.createVar(pathVarName),
                        Predicate.createLit("true", Types.BOOLEAN));
                Predicate freshIsFalse = Predicate.createEquals(Predicate.createVar(pathVarName),
                        Predicate.createLit("false", Types.BOOLEAN));
                thenRefs = Predicate.createConjunction(expRefs, freshIsTrue);
                elseRefs = Predicate.createConjunction(expRefs, freshIsFalse);
            }

            freshRV = context.addInstanceToContext(pathVarName, factory.Type().BOOLEAN_PRIMITIVE, thenRefs, exp);
        }
        vcChecker.addPathVariable(freshRV);

        context.variablesNewIfCombination();
        context.variablesSetBeforeIf();
        context.enterContext();

        // VISIT THEN
        context.enterContext();
        visitCtBlock(ifElement.getThenStatement());
        if (canCompleteNormally(ifElement.getThenStatement())) {
            context.variablesSetThenIf();
        }
        contextHistory.saveContext(ifElement.getThenStatement(), context);
        context.exitContext();

        // VISIT ELSE
        if (ifElement.getElseStatement() != null) {
            context.getVariableByName(pathVarName);
            context.newRefinementToVariableInContext(pathVarName, elseRefs);

            context.enterContext();
            visitCtBlock(ifElement.getElseStatement());
            if (canCompleteNormally(ifElement.getElseStatement())) {
                context.variablesSetElseIf();
            }
            contextHistory.saveContext(ifElement.getElseStatement(), context);
            context.exitContext();
        }
        // end
        // Reset the path variable's refinement to the original condition after the if,
        // so branch-local truth assertions (and any typestate they imply) don't leak past the join.
        context.newRefinementToVariableInContext(pathVarName, expRefs);
        vcChecker.removePathVariable(freshRV);
        context.exitContext();
        context.variablesCombineFromIf(expRefs);
        context.variablesFinishIfCombination();
    }

    /**
     * A condition is uninformative when its refinement is the trivial {@code true} predicate yet the expression itself
     * is not a boolean literal — i.e. the verifier has no symbolic information to relate the branch to. Treating such a
     * condition as {@code true} would force every if-then to be taken, producing spurious state-refinement errors.
     */
    private boolean isUninformativeCondition(Predicate conditionRefinement, CtExpression<Boolean> condition) {
        if (!conditionRefinement.isBooleanTrue())
            return false;
        return !(condition instanceof CtLiteral<?> literal && literal.getValue() instanceof Boolean);
    }

    /**
     * Best-effort normal-completion check (JLS §14.21): branches that always {@code return}, {@code throw},
     * {@code break} or {@code continue} cannot contribute state to code following the {@code if}, so their post-context
     * must be discarded at the join.
     *
     * <p>
     * Not currently handled (treated conservatively as completing normally): {@code switch} where every case exits,
     * labeled {@code break}/{@code continue} targets, {@code try}/{@code catch}/{@code finally} flow, and infinite
     * loops such as {@code while (true)}. Extending this list only tightens precision.
     */
    private boolean canCompleteNormally(CtStatement statement) {
        if (statement == null)
            return true;
        if (statement instanceof CtReturn<?> || statement instanceof CtThrow || statement instanceof CtBreak
                || statement instanceof CtContinue)
            return false;
        if (statement instanceof CtBlock<?> block) {
            List<CtStatement> statements = block.getStatements();
            return statements.isEmpty() || canCompleteNormally(statements.get(statements.size() - 1));
        }
        if (statement instanceof CtIf nestedIf) {
            CtStatement elseStatement = nestedIf.getElseStatement();
            // No else means the false path always falls through.
            if (elseStatement == null)
                return true;
            return canCompleteNormally(nestedIf.getThenStatement()) || canCompleteNormally(elseStatement);
        }
        return true;
    }

    @Override
    public <T> void visitCtArrayWrite(CtArrayWrite<T> arrayWrite) {
        super.visitCtArrayWrite(arrayWrite);
        CtExpression<?> index = arrayWrite.getIndexExpression();
        BuiltinFunctionPredicate fp = BuiltinFunctionPredicate.addToIndex(index.toString(), Keys.WILDCARD, arrayWrite);
        arrayWrite.putMetadata(Keys.REFINEMENT, fp);
    }

    @Override
    public <T> void visitCtConditional(CtConditional<T> conditional) {
        super.visitCtConditional(conditional);
        Predicate cond = getRefinement(conditional.getCondition());
        Predicate c = Predicate.createITE(cond, getRefinement(conditional.getThenExpression()),
                getRefinement(conditional.getElseExpression()));
        conditional.putMetadata(Keys.REFINEMENT, c);
    }

    @Override
    public <T> void visitCtConstructorCall(CtConstructorCall<T> ctConstructorCall) {
        super.visitCtConstructorCall(ctConstructorCall);
        mfc.getConstructorInvocationRefinements(ctConstructorCall);
    }

    @Override
    public <T> void visitCtNewClass(CtNewClass<T> newClass) {
        super.visitCtNewClass(newClass);
    }

    // ############################### Inner Visitors
    // ##########################################
    private void checkAssignment(String name, CtTypeReference<?> type, CtExpression<?> ex, CtExpression<?> assignment,
            CtElement parentElem, CtElement varDecl) throws LJError {
        getPutVariableMetadata(ex, name);

        Predicate refinementFound = getAssignmentRefinement(name, assignment, parentElem);
        if (refinementFound == null) {
            RefinedVariable rv = context.getVariableByName(name);
            if (rv instanceof Variable) {
                refinementFound = rv.getMainRefinement();
            } else {
                refinementFound = new Predicate();
            }
        }
        Optional<VariableInstance> r = context.getLastVariableInstance(name);
        // AQUI!!
        r.ifPresent(variableInstance -> vcChecker.removePathVariableThatIncludes(variableInstance.getName()));

        vcChecker.removePathVariableThatIncludes(name); // AQUI!!
        checkVariableRefinements(refinementFound, name, type, parentElem, varDecl);
    }

    /**
     * Get the refinement for operator assignments (e.g. x += 1)
     */
    private Predicate getAssignmentRefinement(String name, CtExpression<?> assignment, CtElement parentElem)
            throws LJError {
        if (parentElem instanceof CtOperatorAssignment<?, ?> operatorAssignment) {
            return otc.getOperatorAssignmentRefinement(name, operatorAssignment);
        }
        return getRefinement(assignment);
    }

    private Predicate getExpressionRefinements(CtExpression<?> element) throws LJError {
        if (element instanceof CtVariableRead<?>) {
            // CtVariableRead<?> elemVar = (CtVariableRead<?>) element;
            return getRefinement(element);
        } else if (element instanceof CtBinaryOperator<?>) {
            CtBinaryOperator<?> binop = (CtBinaryOperator<?>) element;
            visitCtBinaryOperator(binop);
            return getRefinement(binop);
        } else if (element instanceof CtUnaryOperator<?>) {
            CtUnaryOperator<?> op = (CtUnaryOperator<?>) element;
            visitCtUnaryOperator(op);
            return getRefinement(op);
        } else if (element instanceof CtLiteral<?>) {
            CtLiteral<?> l = (CtLiteral<?>) element;
            return new Predicate(l.getValue().toString(), l);
        } else if (element instanceof CtInvocation<?>) {
            CtInvocation<?> inv = (CtInvocation<?>) element;
            visitCtInvocation(inv);
            return getRefinement(inv);
        }
        return getRefinement(element);
    }

    private Predicate substituteAllVariablesForLastInstance(Predicate c) {
        Predicate ret = c;
        List<String> ls = c.getVariableNames();
        for (String s : ls) {
            Optional<VariableInstance> rv = context.getLastVariableInstance(s);
            if (rv.isPresent()) {
                VariableInstance vi = rv.get();
                ret = ret.substituteVariable(s, vi.getName());
            }
        }
        return ret;
    }

    // ############################### Get Metadata
    // ##########################################

    /**
     * Gets the variable refinement from the context and puts it as metadata in the element
     * 
     * @param elem
     * @param name
     */
    private void getPutVariableMetadata(CtElement elem, String name) {
        Predicate cref = Predicate.createEquals(Predicate.createVar(Keys.WILDCARD), Predicate.createVar(name));
        Optional<VariableInstance> ovi = context.getLastVariableInstance(name);
        if (ovi.isPresent()) {
            cref = Predicate.createEquals(Predicate.createVar(Keys.WILDCARD), Predicate.createVar(ovi.get().getName()));
        }
        elem.putMetadata(Keys.REFINEMENT, cref);
    }
}

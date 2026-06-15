package liquidjava.processor.refinement_checker;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.context.*;
import liquidjava.processor.refinement_checker.general_checkers.MethodsFunctionsChecker;
import liquidjava.processor.refinement_checker.general_checkers.OperationsChecker;
import liquidjava.processor.refinement_checker.object_checkers.AuxStateHandler;
import liquidjava.rj_language.BuiltinFunctionPredicate;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Enum;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.GroupExpression;
import liquidjava.utils.StaticConstants;
import liquidjava.utils.constants.Formats;
import liquidjava.utils.constants.Keys;
import liquidjava.utils.constants.Ops;
import liquidjava.utils.constants.Types;

import org.apache.commons.lang3.NotImplementedException;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtArrayRead;
import spoon.reflect.code.CtArrayWrite;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtBreak;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtContinue;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtLoop;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtThrow;
import spoon.reflect.code.CtTry;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.visitor.filter.TypeFilter;
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
            refinementFound = applyNarrowingCast(e, refinementFound);
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
            Predicate defaultValue = unestablishedExternalFieldDefault(fieldRead);
            if (defaultValue != null) {
                // SOUNDNESS: reading some other object's field (target is not `this`) only carries the field's
                // declared refinement if that refinement was actually established. A field with no initializer
                // holds its Java default value (0 / 0.0 / false) right after construction, which need not satisfy
                // the declared refinement. Model the read as that default value instead of trusting the
                // refinement, so e.g. `@Refinement("_ > 0") int x = o.n;` is correctly rejected when `n` has no
                // initializer. Reads through `this` keep the in-class field invariant (handled below), and fields
                // with an initializer keep their established refinement.
                fieldRead.putMetadata(Keys.REFINEMENT, defaultValue);
            } else {
                fieldRead.putMetadata(Keys.REFINEMENT,
                        Predicate.createEquals(Predicate.createVar(Keys.WILDCARD), Predicate.createVar(thisName)));
            }

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

    /**
     * When a refined instance field is read off another object (the read target is not {@code this}) and the field has
     * no initializer establishing its refinement, the field still holds its Java default value right after
     * construction. Returns {@code #wild == <default literal>} for such reads (so the declared refinement is NOT
     * assumed), or {@code null} when the current {@code this#field} behavior should be kept: reads through {@code this}
     * (in-class invariant), fields that have an initializer, or types without a representable primitive default.
     */
    private <T> Predicate unestablishedExternalFieldDefault(CtFieldRead<T> fieldRead) {
        // Reads through `this` (implicit or explicit) keep the in-class field invariant.
        if (fieldRead.getTarget() == null || fieldRead.getTarget() instanceof CtThisAccess)
            return null;

        CtField<?> field = fieldRead.getVariable().getDeclaration();
        // Without a resolvable declaration we cannot tell whether the field is established; stay conservative and
        // keep the existing behavior rather than risk rejecting an established field.
        if (field == null)
            return null;
        // A field with an initializer (or a non-null constant value) establishes its refinement; trust it.
        if (field.getDefaultExpression() != null)
            return null;

        String type = fieldRead.getType() != null ? fieldRead.getType().getSimpleName() : null;
        Predicate defaultLiteral = defaultLiteralForType(type);
        // Only primitives have a refinement-checkable default literal; for everything else (e.g. references that
        // default to null), keep the existing behavior to avoid spurious errors.
        if (defaultLiteral == null)
            return null;
        return Predicate.createEquals(Predicate.createVar(Keys.WILDCARD), defaultLiteral);
    }

    /** The Java default value, as a literal {@link Predicate}, for a primitive type name, or {@code null} otherwise. */
    private static Predicate defaultLiteralForType(String type) {
        if (type == null)
            return null;
        switch (type) {
        case Types.INT:
        case Types.SHORT:
        case Types.LONG:
            return Predicate.createLit("0", type);
        case Types.CHAR:
            // The char default is ' '; model it via its numeric code point 0.
            return Predicate.createLit("0", Types.INT);
        case Types.FLOAT:
        case Types.DOUBLE:
            return Predicate.createLit("0.0", type);
        case Types.BOOLEAN:
            return Predicate.createLit("false", Types.BOOLEAN);
        default:
            return null;
        }
    }

    @Override
    public <T> void visitCtVariableRead(CtVariableRead<T> variableRead) {
        super.visitCtVariableRead(variableRead);
        CtVariable<T> varDecl = variableRead.getVariable().getDeclaration();
        // Some CtVariableRead forms have no resolvable declaration (e.g. accesses to symbols outside the
        // model); with no name there is no context entry to attach, so leave the metadata as-is.
        if (varDecl == null)
            return;
        getPutVariableMetadata(variableRead, varDecl.getSimpleName());
    }

    /**
     * Visitor for binary operations Adds metadata to the binary operations from the operands
     */
    @Override
    public <T> void visitCtBinaryOperator(CtBinaryOperator<T> operator) {
        super.visitCtBinaryOperator(operator);
        otc.getBinaryOpRefinements(operator);
        forgetShortCircuitedAssignments(operator);
    }

    /**
     * The right operand of {@code &&}/{@code ||} runs only conditionally (it is short-circuited when the left operand
     * is already {@code false} resp. {@code true}). Spoon visits children before this method, so any assignment in that
     * operand (e.g. {@code false && ((x = 1) == 1)}) has already committed its value to the context as if it always
     * executed. That is unsound: at runtime the assignment may never happen, so the post-operator value of every
     * variable written there is uncertain. Havoc those variables (give them a fresh, unconstrained instance) so the
     * verifier can no longer assume the assigned value survives the operator.
     *
     * <p>
     * This is conservative: when the left operand is statically true (resp. false) the right operand does execute, yet
     * we still forget the value. Forgetting only ever weakens what is known, so it cannot accept an unsound program; it
     * costs precision only for the rare idiom of relying on a value assigned inside a short-circuited operand.
     */
    private void forgetShortCircuitedAssignments(CtBinaryOperator<?> operator) {
        BinaryOperatorKind kind = operator.getKind();
        if (kind != BinaryOperatorKind.AND && kind != BinaryOperatorKind.OR)
            return;

        havocVariablesWrittenIn(operator.getRightHandOperand());
    }

    /**
     * Havocs (see {@link #havocVariable}) every local or field variable that appears as a write target anywhere inside
     * {@code scope}. {@link CtVariableWrite} covers plain assignments ({@code x = ...}), compound assignments
     * ({@code x += ...}) and the operand of pre/post increment/decrement ({@code x++}, {@code --x}), and its subtype
     * {@link CtFieldWrite} covers field writes.
     */
    private void havocVariablesWrittenIn(CtElement scope) {
        if (scope == null)
            return;
        for (CtVariableWrite<?> write : scope.getElements(new TypeFilter<>(CtVariableWrite.class))) {
            CtVariableReference<?> ref = write.getVariable();
            if (ref == null)
                continue;
            String name = (write instanceof CtFieldWrite<?>) ? String.format(Formats.THIS, ref.getSimpleName())
                    : ref.getSimpleName();
            havocVariable(name, write);
        }
    }

    /**
     * Drops everything currently known about {@code name} by installing a fresh, unconstrained instance as its latest
     * value. Subsequent reads resolve to this instance and therefore carry no refinement.
     */
    private void havocVariable(String name, CtElement element) {
        RefinedVariable rv = context.getVariableByName(name);
        if (!(rv instanceof Variable))
            return;
        String freshName = String.format(Formats.INSTANCE, name, context.getCounter());
        context.addInstanceToContext(freshName, rv.getType(), new Predicate(), element);
        context.addRefinementInstanceToVariable(name, freshName);
        context.addRefinementToVariableInContext(name, rv.getType(), new Predicate(), element);
    }

    // ############################### Loops ##########################################

    /*
     * Loop bodies are visited a single time by the underlying CtScanner, which models exactly one iteration and then
     * commits the body's assignments as if that were the loop's post-state. That is unsound: a variable mutated in the
     * loop keeps a value computed for one pass, but at runtime the loop may iterate zero or many times, so its
     * post-loop value is unknown. After visiting each loop we therefore havoc (give a fresh, unconstrained instance to)
     * every variable written in the body and in the for-update, so nothing assumed about its in-loop value survives the
     * loop. Forgetting only weakens what is known, so it can never accept an unsound program; it costs precision only
     * for code that relies on a specific value established by the loop. visitCt*Loop still calls super first so that
     * any refinement/typestate error inside the body is still reported, exactly as before.
     */

    @Override
    public void visitCtWhile(CtWhile whileLoop) {
        super.visitCtWhile(whileLoop);
        havocLoopVariables(whileLoop);
    }

    @Override
    public void visitCtDo(CtDo doLoop) {
        super.visitCtDo(doLoop);
        havocLoopVariables(doLoop);
    }

    @Override
    public void visitCtFor(CtFor forLoop) {
        super.visitCtFor(forLoop);
        havocLoopVariables(forLoop);
        havocVariablesWrittenIn(forLoop.getExpression());
        for (CtStatement update : forLoop.getForUpdate())
            havocVariablesWrittenIn(update);
    }

    @Override
    public void visitCtForEach(CtForEach foreach) {
        super.visitCtForEach(foreach);
        havocLoopVariables(foreach);
    }

    private void havocLoopVariables(CtLoop loop) {
        havocVariablesWrittenIn(loop.getBody());
    }

    // ############################### Try/Catch ######################################

    /*
     * A try body is visited as straight-line code, so the underlying CtScanner commits every assignment in it as if the
     * body ran to completion. That is unsound: an exception can interrupt the try at any point, so a statement after a
     * throwing one may never execute and a variable assigned there may not hold that value once control leaves the try.
     * The same holds inside each catch block, whose statements run only on the (uncertain) exceptional path. After
     * visiting the try (so any refinement/typestate error inside the body or handlers is still reported, exactly as
     * before), we therefore havoc (give a fresh, unconstrained instance to) every variable written in the try body and
     * in each catch block, so nothing assumed about an in-try/in-catch value survives the statement. Forgetting only
     * weakens what is known, so it can never accept an unsound program; it costs precision only for code that relies on
     * a value established inside the try/catch. (A finally block always runs to completion, so its definite assignments
     * could remain trusted; we keep the minimal sound option and do not special-case it.)
     */

    @Override
    public void visitCtTry(CtTry tryBlock) {
        super.visitCtTry(tryBlock);
        havocVariablesWrittenIn(tryBlock.getBody());
        for (CtCatch catchBlock : tryBlock.getCatchers())
            havocVariablesWrittenIn(catchBlock.getBody());
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
        return applyNarrowingCast(assignment, getRefinement(assignment));
    }

    private Predicate getExpressionRefinements(CtExpression<?> element) throws LJError {
        if (element instanceof CtFieldRead<?> fieldRead) {
            visitCtFieldRead(fieldRead);
            return getRefinement(element);
        } else if (element instanceof CtVariableRead<?> varRead) {
            visitCtVariableRead(varRead);
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

    // ############################### Numeric Casts ##########################################

    /**
     * Models a Java floating-point-to-integral narrowing cast on the value of {@code ex} (e.g. {@code (int) 1.9}, which
     * is {@code 1} at runtime), so the refinement describes the truncated value rather than the original floating one.
     *
     * <p>
     * SOUNDNESS: without this, the cast is dropped and the value keeps its wide floating form, so a refinement like
     * {@code _ == 1.9} on {@code (int) 1.9} is accepted even though the runtime value is {@code 1}. We rewrite the
     * value-defining refinement {@code _ == E} into {@code _ == trunc(E)}, where {@code trunc} truncates toward zero
     * (JLS §5.1.3). Only floating-to-integral casts change the value here, so casts that leave the value unchanged (an
     * {@code int}-to-{@code int} cast such as {@code (int) one()}, or a widening cast such as {@code (float) 5L}) are
     * left untouched and stay precise.
     */
    private Predicate applyNarrowingCast(CtExpression<?> ex, Predicate refinement) {
        if (ex == null || refinement == null || !isFloatingToIntegralCast(ex))
            return refinement;
        Optional<Expression> value = wildcardEqualityValue(refinement);
        if (value.isEmpty())
            return refinement;
        Predicate truncated = Predicate.createInvocation(Keys.TRUNCATE, new Predicate(value.get()));
        return Predicate.createEquals(Predicate.createVar(Keys.WILDCARD), truncated);
    }

    /**
     * True when the outermost type cast on {@code ex} narrows a floating-point value to an integral one. The casts are
     * ordered outermost-first by Spoon, and {@code ex.getType()} is the type of the operand before any cast is applied.
     */
    private boolean isFloatingToIntegralCast(CtExpression<?> ex) {
        List<CtTypeReference<?>> casts = ex.getTypeCasts();
        if (casts == null || casts.isEmpty())
            return false;
        CtTypeReference<?> operandType = ex.getType();
        return operandType != null && isFloatingType(operandType.getSimpleName())
                && isIntegralType(casts.get(0).getSimpleName());
    }

    private static boolean isFloatingType(String type) {
        return Types.FLOAT.equals(type) || Types.DOUBLE.equals(type);
    }

    private static boolean isIntegralType(String type) {
        return Types.INT.equals(type) || Types.LONG.equals(type) || Types.SHORT.equals(type) || Types.CHAR.equals(type)
                || "byte".equals(type);
    }

    /** If {@code refinement} has the value-defining shape {@code _ == E} (possibly grouped), returns {@code E}. */
    private static Optional<Expression> wildcardEqualityValue(Predicate refinement) {
        Expression e = refinement.getExpression();
        while (e instanceof GroupExpression ge)
            e = ge.getExpression();
        if (e instanceof BinaryExpression be && Ops.EQ.equals(be.getOperator())
                && Keys.WILDCARD.equals(be.getFirstOperand().toString()))
            return Optional.of(be.getSecondOperand());
        return Optional.empty();
    }
}

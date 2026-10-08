package liquidjava.processor.refinement_checker;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Formats;
import liquidjava.utils.constants.Keys;
import liquidjava.utils.constants.Types;

import org.apache.commons.lang3.NotImplementedException;
import spoon.reflect.code.CtArrayRead;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtArrayWrite;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtBreak;
import spoon.reflect.code.CtCatchVariable;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtContinue;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtLoop;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtSuperAccess;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtThrow;
import spoon.reflect.code.CtTry;
import spoon.reflect.code.CtTryWithResource;
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
    TryChecker tryChecker;
    Diagnostics diagnostics = Diagnostics.getInstance();
    ContextHistory contextHistory = ContextHistory.getInstance();

    public RefinementTypeChecker(Context context, Factory factory) {
        super(context, factory);
        otc = new OperationsChecker(this);
        mfc = new MethodsFunctionsChecker(this);
        tryChecker = new TryChecker(this, context, vcChecker, factory);
    }

    // --------------------- Visitors -----------------------------------

    @Override
    public <T> void visitCtClass(CtClass<T> ctClass) {
        Context.ClassScope scope = context.enterClassScope();
        try {
            super.visitCtClass(ctClass);
        } catch (LJError e) {
            diagnostics.add(e);
        } finally {
            context.exitClassScope(scope);
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
        context.clearInstanceVariables();
        context.enterContext();
        context.restoreInstanceVariables();
        mfc.loadFunctionInfo(constructor);
        try {
            super.visitCtConstructor(constructor);
        } catch (LJError e) {
            diagnostics.add(e);
        }
        contextHistory.saveContext(constructor, context);
        context.exitContext();
        vcChecker.clearPathVariables();
    }

    public <R> void visitCtMethod(CtMethod<R> method) {
        context.clearInstanceVariables();
        context.enterContext();
        context.restoreInstanceVariables();
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
        vcChecker.clearPathVariables();
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
    public <T> void visitCtCatchVariable(CtCatchVariable<T> catchVariable) {
        super.visitCtCatchVariable(catchVariable);
        // like a method parameter, the caught object is a new variable with unknown state; Java forbids it to share a
        // name with a local in scope, so a variable with the same name in the context is an out-of-scope local
        context.removeVarFromContext(catchVariable.getSimpleName());
        context.addVarToContext(catchVariable.getSimpleName(), catchVariable.getType(), new Predicate(), catchVariable);
        // an instance lets refinements that refer to it outlive the catch block
        String instanceName = String.format(Formats.INSTANCE, catchVariable.getSimpleName(), context.getCounter());
        context.addInstanceToContext(instanceName, catchVariable.getType(), new Predicate(), catchVariable);
        context.addRefinementInstanceToVariable(catchVariable.getSimpleName(), instanceName);
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
            String updatedVarName = Utils.qualifyFieldName(cr);
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
        String name = Utils.qualifyFieldName(f.getReference());
        Predicate ret = new Predicate();
        if (c.isPresent()) {
            ret = c.get().substituteVariable(Keys.WILDCARD, name).substituteVariable(f.getSimpleName(), name);
        }
        RefinedVariable v = context.addVarToContext(name, f.getType(), ret, f);
        if (f.getAssignment() != null) {
            Predicate refinement = getRefinement(f.getAssignment());
            checkVariableRefinements(refinement != null ? refinement : new Predicate(), name, f.getType(), f, f);
            AuxStateHandler.addStateRefinements(this, name, f.getAssignment());
        }
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

        } else if (context.hasVariable(Utils.qualifyFieldName(fieldRead.getVariable()))) {
            String thisName = Utils.qualifyFieldName(fieldRead.getVariable());
            fieldRead.putMetadata(Keys.REFINEMENT,
                    Predicate.createEquals(Predicate.createVar(Keys.WILDCARD), Predicate.createVar(thisName)));

        } else if (fieldRead.getVariable().getSimpleName().equals("length")) {
            String targetName = fieldRead.getTarget().toString();
            fieldRead.putMetadata(Keys.REFINEMENT, Predicate.createEquals(Predicate.createVar(Keys.WILDCARD),
                    BuiltinFunctionPredicate.length(targetName, fieldRead)));
        } else if (fieldRead.getVariable().getDeclaringType().getDeclaration() instanceof CtEnum) {
            // only enums declared in the analyzed sources can be translated to SMT
            String target = fieldRead.getVariable().getDeclaringType().getSimpleName();
            String enumLiteral = String.format(Formats.ENUM, target, fieldName);
            fieldRead.putMetadata(Keys.REFINEMENT,
                    Predicate.createEquals(Predicate.createVar(Keys.WILDCARD), Predicate.createVar(enumLiteral)));
        } else if (tryStaticFinalConstantRefinement(fieldRead)) {
            // refinement metadata set by helper
        } else if (fieldRead.getVariable().getDeclaration() != null) {
            Predicate declared = getRefinementFromAnnotation(fieldRead.getVariable().getDeclaration())
                    .orElseGet(Predicate::new);
            fieldRead.putMetadata(Keys.REFINEMENT, declared.substituteVariable(fieldName, Keys.WILDCARD));
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
    public <T> void visitCtLambda(CtLambda<T> lambda) {
        Context.DeferredScope scope = context.enterDeferredScope();
        List<RefinedVariable> pathVariables = vcChecker.getPathVariables();
        try {
            // Only facts about immutable primitive captures survive until an unknown later invocation.
            Set<String> immutableNames = immutableLambdaCaptureNames(lambda);
            vcChecker
                    .replacePathVariables(pathVariables.stream()
                            .filter(path -> path.getRefinement().getVariableNames().stream()
                                    .allMatch(name -> name.equals(path.getName()) || immutableNames.contains(name)))
                            .toList());
            havocLambdaCaptures(lambda);
            for (CtParameter<?> parameter : lambda.getParameters()) {
                Predicate declared = getRefinementFromAnnotation(parameter).orElseGet(Predicate::new)
                        .substituteVariable(Keys.WILDCARD, parameter.getSimpleName());
                context.addVarToContext(parameter.getSimpleName(), parameter.getType(), declared, parameter);
            }
            super.visitCtLambda(lambda);
        } catch (LJError e) {
            diagnostics.add(e);
        } finally {
            context.exitDeferredScope(scope);
            vcChecker.replacePathVariables(pathVariables);
        }
    }

    private Set<String> immutableLambdaCaptureNames(CtLambda<?> lambda) {
        Set<String> names = new LinkedHashSet<>();
        for (CtVariableAccess<?> access : lambda
                .getElements(new TypeFilter<CtVariableAccess<?>>(CtVariableAccess.class))) {
            CtVariable<?> declaration = access.getVariable().getDeclaration();
            if (access instanceof CtFieldAccess<?> || access.getType() == null || !access.getType().isPrimitive()
                    || declaration == null || declaration.hasParent(lambda))
                continue;
            // Java requires captured locals and parameters to be final or effectively final, including blank
            // locals assigned before capture. Their primitive values cannot change before invocation.
            if (!(declaration instanceof CtLocalVariable<?> || declaration instanceof CtParameter<?>))
                continue;
            names.add(access.getVariable().getSimpleName());
        }
        for (RefinedVariable rv : context.getCtxInstanceVars())
            if (rv instanceof VariableInstance instance
                    && instance.getParent().map(parent -> names.contains(parent.getName())).orElse(false))
                names.add(instance.getName());
        return names;
    }

    private void havocLambdaCaptures(CtLambda<?> lambda) {
        Set<String> names = new LinkedHashSet<>();
        for (CtVariableAccess<?> access : lambda
                .getElements(new TypeFilter<CtVariableAccess<?>>(CtVariableAccess.class))) {
            CtVariable<?> declaration = access.getVariable().getDeclaration();
            if (declaration != null && declaration.hasParent(lambda))
                continue;
            if (access instanceof CtFieldAccess<?> field) {
                names.add(Utils.qualifyFieldName(field.getVariable()));
            } else if (access instanceof CtSuperAccess<?> parent) {
                if (parent.getTarget() == null || parent.getTarget().isImplicit())
                    names.add(Keys.THIS);
            } else if (access.getType() != null && !access.getType().isPrimitive()) {
                names.add(access.getVariable().getSimpleName());
            }
        }
        for (CtThisAccess<?> self : lambda.getElements(new TypeFilter<CtThisAccess<?>>(CtThisAccess.class))) {
            CtType<?> owner = self.getParent(CtType.class);
            if (owner != null && self.getType() != null
                    && self.getType().getQualifiedName().equals(owner.getQualifiedName()))
                names.add(Keys.THIS);
        }
        for (String name : names) {
            if (!(context.getVariableByName(name)instanceof Variable variable))
                continue;
            String instanceName = String.format(Formats.INSTANCE, name, context.getCounter());
            Predicate declared = variable.getMainRefinement().substituteVariable(name, instanceName);
            context.addInstanceToContext(instanceName, variable.getType(), declared, lambda);
            context.addRefinementInstanceToVariable(name, instanceName);
        }
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
        boolean thenCompletes = canCompleteNormally(ifElement.getThenStatement());
        if (thenCompletes) {
            context.variablesSetThenIf();
        }
        contextHistory.saveContext(ifElement.getThenStatement(), context);
        context.exitContext();

        // VISIT ELSE
        boolean elseCompletes = true;
        if (ifElement.getElseStatement() != null) {
            context.getVariableByName(pathVarName);
            context.newRefinementToVariableInContext(pathVarName, elseRefs);

            context.enterContext();
            visitCtBlock(ifElement.getElseStatement());
            elseCompletes = canCompleteNormally(ifElement.getElseStatement());
            if (elseCompletes) {
                context.variablesSetElseIf();
            }
            contextHistory.saveContext(ifElement.getElseStatement(), context);
            context.exitContext();
        }
        // end
        if (thenCompletes == elseCompletes) {
            // Reset the path variable's refinement to the original condition after the if,
            // so branch-local truth assertions (and any typestate they imply) don't leak past the join.
            context.newRefinementToVariableInContext(pathVarName, expRefs);
            vcChecker.removePathVariable(freshRV);
        } else {
            // Keep the refinement of the only branch that reaches the code after the if.
            context.newRefinementToVariableInContext(pathVarName, thenCompletes ? thenRefs : elseRefs);
        }
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
    boolean canCompleteNormally(CtStatement statement) {
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
    public void visitCtTry(CtTry tryBlock) {
        tryChecker.visitTry(tryBlock);
    }

    @Override
    public void visitCtTryWithResource(CtTryWithResource tryWithResource) {
        tryChecker.visitTry(tryWithResource);
    }

    @Override
    public void visitCtWhile(CtWhile whileLoop) {
        visitLoop(whileLoop, () -> {
            scan(whileLoop.getLoopingExpression());
            assumeLoopCondition(whileLoop.getLoopingExpression());
            scan(whileLoop.getBody());
        });
    }

    @Override
    public void visitCtFor(CtFor forLoop) {
        scan(forLoop.getForInit());
        visitLoop(forLoop, () -> {
            scan(forLoop.getExpression());
            assumeLoopCondition(forLoop.getExpression());
            List<RefinedVariable> pathVariables = vcChecker.getPathVariables();
            scan(forLoop.getBody());
            if (!forLoop.getBody().getElements(new TypeFilter<>(CtContinue.class)).isEmpty()) {
                // a continue reaches the update from any point of the body, skipping its facts and assignments
                vcChecker.restorePathVariables(pathVariables);
                havocChangedIn(forLoop);
            }
            // the update runs after the body, so the body sees the values the condition was checked on
            scan(forLoop.getForUpdate());
        });
    }

    @Override
    public void visitCtDo(CtDo doLoop) {
        // the condition is not checked before the first iteration, so it is not assumed in the body
        visitLoop(doLoop, () -> super.visitCtDo(doLoop));
    }

    @Override
    public void visitCtForEach(CtForEach forEach) {
        visitLoop(forEach, () -> super.visitCtForEach(forEach));
    }

    /**
     * Checks one arbitrary iteration of a loop: what the loop may change is havocked (keeps only its declared
     * refinement, which every assignment re-checks) before it and again after it, since the loop may run any number of
     * times. Path conditions added inside the loop (e.g. the loop condition, or an {@code if (...) break;}) are dropped
     * after it.
     */
    private void visitLoop(CtLoop loop, Runnable iteration) {
        List<RefinedVariable> pathVariables = vcChecker.getPathVariables();
        havocChangedIn(loop);
        iteration.run();
        vcChecker.restorePathVariables(pathVariables);
        havocChangedIn(loop);
    }

    /**
     * Havocs what the loop may change: the variables it writes, the objects it calls state-changing methods on, and the
     * fields if it calls any method or constructor (which may write them)
     */
    private void havocChangedIn(CtLoop loop) {
        List<CtVariableAccess<?>> changed = new ArrayList<>(
                loop.getElements(new TypeFilter<CtVariableWrite<?>>(CtVariableWrite.class)));
        List<CtAbstractInvocation<?>> calls = loop
                .getElements(new TypeFilter<CtAbstractInvocation<?>>(CtAbstractInvocation.class));
        for (CtAbstractInvocation<?> call : calls) {
            if (call instanceof CtInvocation<?> inv && inv.getTarget()instanceof CtVariableAccess<?> target
                    && context.getAllMethodsWithNameSize(inv.getExecutable().getSimpleName(), inv.getArguments().size())
                            .stream().anyMatch(f -> f.getAllStates().stream().anyMatch(ObjectState::hasTo)))
                changed.add(target);
        }
        Set<String> names = new LinkedHashSet<>();
        for (CtVariableAccess<?> access : changed) {
            CtVariable<?> declaration = access.getVariable().getDeclaration();
            if (declaration != null && declaration.hasParent(loop.getBody()))
                continue; // declared in the body: a new variable on each iteration
            String name = access.getVariable().getSimpleName();
            names.add(access instanceof CtFieldAccess<?> ? String.format(Formats.THIS, name) : name);
        }
        if (!calls.isEmpty())
            context.getCtxVars().stream().map(RefinedVariable::getName)
                    .filter(n -> n.startsWith(String.format(Formats.THIS, ""))).forEach(names::add);
        for (String name : names) {
            if (!(context.getVariableByName(name)instanceof Variable variable))
                continue;
            vcChecker.removePathVariableThatIncludes(name);
            String instanceName = String.format(Formats.INSTANCE, name, context.getCounter());
            Predicate declared = variable.getMainRefinement().substituteVariable(name, instanceName);
            context.addInstanceToContext(instanceName, variable.getType(), declared, loop);
            context.addRefinementInstanceToVariable(name, instanceName);
        }
    }

    /**
     * Assumes the loop condition in the body, as a path condition on the values it was evaluated on (same encoding as
     * the condition of an if, see visitCtIf). Re-assigning a variable in the body drops the conditions on it.
     */
    private void assumeLoopCondition(CtExpression<Boolean> condition) {
        // conditions with side effects (e.g. (n = read()) > 0) are not encoded, so they assume nothing
        if (condition == null || !condition.getElements(new TypeFilter<>(CtVariableWrite.class)).isEmpty())
            return;
        Predicate refs = getRefinement(condition);
        if (isUninformativeCondition(refs, condition))
            return;
        String pathVarName = String.format(Formats.FRESH, context.getCounter());
        boolean valueIsCondition = refs.getVariableNames().contains(Keys.WILDCARD);
        refs = refs.substituteVariable(Keys.WILDCARD, pathVarName);
        refs = Predicate.createConjunction(refs, substituteAllVariablesForLastInstance(refs));
        if (valueIsCondition) {
            refs = Predicate.createConjunction(refs, Predicate.createEquals(Predicate.createVar(pathVarName),
                    Predicate.createLit("true", Types.BOOLEAN)));
        }
        vcChecker.addPathVariable(
                context.addInstanceToContext(pathVarName, factory.Type().BOOLEAN_PRIMITIVE, refs, condition));
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
}

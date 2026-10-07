package liquidjava.processor.refinement_checker;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import liquidjava.processor.context.Context;
import liquidjava.processor.context.RefinedVariable;
import liquidjava.processor.context.Variable;
import liquidjava.processor.context.VariableInstance;
import liquidjava.rj_language.Predicate;
import liquidjava.utils.constants.Formats;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtResource;
import spoon.reflect.code.CtTry;
import spoon.reflect.code.CtTryWithResource;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

/**
 * Checks try statements. The try block and its catch blocks are combined like an if-else-if chain with unknown
 * conditions. An exception may leave the try block after any of its statements, so each catch block starts with every
 * variable in any of the states it had before or during the try block. The finally block also runs after returns and
 * uncaught exceptions, so it starts with any of the states from the try and catch blocks.
 */
class TryChecker {

    private final RefinementTypeChecker rtc;
    private final Context context;
    private final VCChecker vcChecker;
    private final Factory factory;

    TryChecker(RefinementTypeChecker rtc, Context context, VCChecker vcChecker, Factory factory) {
        this.rtc = rtc;
        this.context = context;
        this.vcChecker = vcChecker;
        this.factory = factory;
    }

    void visitTry(CtTry tryBlock) {
        // last instance of each variable before the try statement, or null if it has none
        Map<Variable, VariableInstance> before = new IdentityHashMap<>();
        for (RefinedVariable rv : context.getCtxVars())
            if (rv instanceof Variable v)
                before.put(v, v.getLastInstance().orElse(null));
        List<RefinedVariable> pathBefore = vcChecker.getPathVariables();

        context.startRecordingInstances();
        Map<Variable, List<VariableInstance>> inTryAndCatches;
        try {
            visitBranches(tryBlock, 0, before, null);
        } finally {
            inTryAndCatches = context.stopRecordingInstances();
        }
        if (tryBlock.getFinalizer() != null)
            visitFinally(tryBlock, before, inTryAndCatches, pathBefore);
    }

    /**
     * Visits the try block (branch 0) and the catch blocks (branches 1..n) from branch {@code i} like an if-else-if
     * chain, combining each variable after it from the branches that can complete normally
     *
     * @return whether some branch from {@code i} can complete normally
     */
    private boolean visitBranches(CtTry tryBlock, int i, Map<Variable, VariableInstance> before,
            Map<Variable, List<VariableInstance>> inTry) {
        if (i == tryBlock.getCatchers().size())
            return visitBranch(tryBlock, i, before, inTry);

        String pathVarName = String.format(Formats.FRESH, context.getCounter());
        Predicate cond = Predicate.createVar(pathVarName);
        RefinedVariable pathVar = context.addInstanceToContext(pathVarName, factory.Type().BOOLEAN_PRIMITIVE,
                new Predicate(), tryBlock);
        vcChecker.addPathVariable(pathVar);
        context.variablesNewIfCombination();
        context.variablesSetBeforeIf();
        context.enterContext();
        // path conditions from a branch hold neither in the next ones, which may start before it ends, nor after them
        List<RefinedVariable> pathBefore = vcChecker.getPathVariables();

        context.enterContext();
        if (i == 0)
            context.startRecordingInstances();
        boolean thenCompletes;
        try {
            thenCompletes = visitBranch(tryBlock, i, before, inTry);
        } finally {
            if (i == 0)
                inTry = context.stopRecordingInstances();
        }
        if (thenCompletes)
            context.variablesSetThenIf();
        context.exitContext();
        vcChecker.restorePathVariables(pathBefore);

        context.newRefinementToVariableInContext(pathVarName, cond.negate());
        context.enterContext();
        boolean elseCompletes = visitBranches(tryBlock, i + 1, before, inTry);
        if (elseCompletes)
            context.variablesSetElseIf();
        context.exitContext();
        vcChecker.restorePathVariables(pathBefore);

        if (thenCompletes == elseCompletes) {
            context.newRefinementToVariableInContext(pathVarName, new Predicate()); // either branch may have run
            vcChecker.removePathVariable(pathVar);
        } else {
            context.newRefinementToVariableInContext(pathVarName, thenCompletes ? cond : cond.negate());
        }
        context.exitContext();
        context.variablesCombineFromIf(cond);
        context.variablesFinishIfCombination();
        return thenCompletes || elseCompletes;
    }

    /** @return whether branch {@code i} can complete normally */
    private boolean visitBranch(CtTry tryBlock, int i, Map<Variable, VariableInstance> before,
            Map<Variable, List<VariableInstance>> inTry) {
        if (i == 0) {
            if (tryBlock instanceof CtTryWithResource tryWithResource)
                visitResourcesAndBody(tryWithResource);
            else
                rtc.scan(tryBlock.getBody());
            return rtc.canCompleteNormally(tryBlock.getBody());
        }
        CtCatch catcher = tryBlock.getCatchers().get(i - 1);
        addAnyOfInstances(before, inTry, catcher);
        rtc.scan(catcher);
        return rtc.canCompleteNormally(catcher.getBody());
    }

    /**
     * Visits the finally block from any of the states of the try statement, then continues from the states where the
     * try statement completed normally, updated with the variables changed in the finally block
     */
    private void visitFinally(CtTry tryBlock, Map<Variable, VariableInstance> before,
            Map<Variable, List<VariableInstance>> inTryAndCatches, List<RefinedVariable> pathBefore) {
        List<RefinedVariable> pathAfterTry = vcChecker.getPathVariables();
        vcChecker.restorePathVariables(pathBefore);
        context.enterContext();
        addAnyOfInstances(before, inTryAndCatches, tryBlock.getFinalizer());
        context.startRecordingInstances();
        Map<Variable, List<VariableInstance>> inFinally;
        try {
            rtc.scan(tryBlock.getFinalizer());
        } finally {
            inFinally = context.stopRecordingInstances();
        }
        inFinally.keySet().retainAll(before.keySet());
        Map<Variable, VariableInstance> lastInFinally = new IdentityHashMap<>();
        inFinally.keySet().forEach(v -> v.getLastInstance().ifPresent(vi -> lastInFinally.put(v, vi)));
        context.exitContext();

        lastInFinally.forEach((v, vi) -> context.addRefinementInstanceToVariable(v.getName(), vi.getName()));
        // the path conditions of the normal completion hold again, except the ones on variables changed in the finally
        for (RefinedVariable rv : pathAfterTry)
            if (!pathBefore.contains(rv))
                vcChecker.addPathVariable(rv);
        for (Variable v : inFinally.keySet()) {
            vcChecker.removePathVariableThatIncludes(v.getName());
            if (before.get(v) != null)
                vcChecker.removePathVariableThatIncludes(before.get(v).getName());
            inTryAndCatches.getOrDefault(v, List.of())
                    .forEach(vi -> vcChecker.removePathVariableThatIncludes(vi.getName()));
        }
    }

    /** Gives each variable in {@code recorded} an instance that may be its instance before or any recorded one */
    private void addAnyOfInstances(Map<Variable, VariableInstance> before,
            Map<Variable, List<VariableInstance>> recorded, CtElement element) {
        recorded.forEach((v, instances) -> {
            if (!before.containsKey(v)) // declared inside the try statement
                return;
            String name = String.format(Formats.INSTANCE, v.getName(), context.getCounter());
            Predicate anyOf = new Predicate(); // unknown if the variable had no instance before
            if (before.get(v) != null) {
                anyOf = before.get(v).getRenamedRefinements(name);
                for (VariableInstance vi : instances)
                    anyOf = Predicate.createDisjunction(anyOf, vi.getRenamedRefinements(name));
            }
            context.addInstanceToContext(name, v.getType(), anyOf, element);
            context.addRefinementInstanceToVariable(v.getName(), name);
        });
    }

    /** Visits the resources and the body of a try-with-resources, which ends by closing the resources */
    private void visitResourcesAndBody(CtTryWithResource tryWithResource) {
        // a resource is either a declaration (`try (R r = ...)`) or a reference to an existing variable (Java 9 `try
        // (r)`)
        List<CtResource<?>> resources = tryWithResource.getResources();
        rtc.scan(resources);
        rtc.scan(tryWithResource.getBody());

        // the resources are closed when the body ends, in reverse order, before any catch or finally block runs
        for (int i = resources.size() - 1; i >= 0; i--)
            rtc.scan(createImplicitClose(resources.get(i), tryWithResource));
    }

    /** Builds the {@code resource.close()} that Java inserts at the end of a try-with-resources block */
    private CtInvocation<?> createImplicitClose(CtResource<?> resource, CtTryWithResource tryWithResource) {
        CtExpression<?> target = resource instanceof CtLocalVariable<?> variable
                ? factory.Code().createVariableRead(variable.getReference(), false)
                : ((CtVariableRead<?>) resource).clone();
        CtTypeReference<?> type = target.getType();
        CtExecutableReference<?> close = type.getAllExecutables().stream()
                .filter(e -> e.getSimpleName().equals("close") && e.getParameters().isEmpty()).findFirst()
                .orElseGet(() -> factory.Executable().createReference(type, factory.Type().VOID_PRIMITIVE, "close"));
        CtInvocation<?> invocation = factory.Code().createInvocation(target, close);
        invocation.setParent(tryWithResource);
        invocation.setPosition(resource.getPosition());
        target.setPosition(resource.getPosition());
        return invocation;
    }
}

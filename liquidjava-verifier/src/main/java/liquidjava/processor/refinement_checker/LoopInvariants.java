package liquidjava.processor.refinement_checker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.context.RefinedVariable;
import liquidjava.processor.context.VariableInstance;
import liquidjava.rj_language.Predicate;
import liquidjava.utils.constants.Ops;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtLoop;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtWhile;

/**
 * The invariants inferred for the {@code int} local variables a loop changes, with the Houdini algorithm (Flanagan and
 * Leino): of their candidates (see {@link InvariantCandidates}), those that the facts before the loop prove are assumed
 * at the loop head; each check of the body drops those not proved where the next iteration starts, until a check drops
 * none. What remains holds before the loop and after every iteration that goes on to the next one, so it holds at the
 * loop head on every iteration. The proofs are silent: a candidate that is not proved is only dropped.
 *
 * <p>
 * Where the continues of the body join, at its end, the values of what the loop changes are forgotten. The facts proved
 * at every continue and at the end of the body are kept there: the candidates, and whether each variable grew or shrank
 * since the loop head.
 *
 * <p>
 * An error that stops a check of the body leaves the candidates unchecked on the paths after it, so it is not reported
 * with them. The statement of the body where it stopped is skipped in the next checks, as if it changed anything it may
 * change, until a check drops no candidate; then the body is checked again without skipping it, which reports the error
 * with candidates proved on every path. If the statement cannot be skipped, all candidates are dropped.
 */
class LoopInvariants {

    /** The most checks of the body that drop candidates, after which all are dropped */
    static final int MAX_ROUNDS = 8;

    private final RefinementTypeChecker checker;
    private final CtLoop loop;
    private final List<String> variables;
    private final List<Predicate> candidates = new ArrayList<>();
    // the instance of each variable at the loop head
    private final Map<String, VariableInstance> head = new LinkedHashMap<>();
    // the refinement of each instance before facts were added to it
    private final Map<VariableInstance, Predicate> declared = new IdentityHashMap<>();
    private int rounds;
    private boolean dropped;
    // whether the point of the iteration being checked can be reached
    private boolean reachable;
    // the facts proved at every continue reached so far, null if none was reached
    private List<Predicate> atContinues;
    // the statements of the body whose check stopped at an error, skipped in the checks that prove the candidates
    private final Set<CtStatement> skipped = Collections.newSetFromMap(new IdentityHashMap<>());
    // the statement of the body where an error stopped the iteration being checked, if any
    private CtStatement stoppedAt;
    // whether the iteration being checked follows a fixpoint reached with statements skipped, to report their error
    private boolean reporting;

    /**
     * Keeps the candidates that the facts before the loop prove; to be created before what the loop changes is
     * forgotten
     *
     * @param variables
     *            the {@code int} local variables the loop changes
     */
    LoopInvariants(RefinementTypeChecker checker, CtLoop loop, List<String> variables) {
        this.checker = checker;
        this.loop = loop;
        this.variables = variables;
        for (String variable : variables) {
            Optional<String> before = checker.getContext().getLastVariableInstance(variable)
                    .map(RefinedVariable::getName);
            for (Predicate candidate : InvariantCandidates.of(variable, before, condition(loop)))
                if (!candidates.contains(candidate) && proved(candidate))
                    candidates.add(candidate);
        }
    }

    /** Starts checking an iteration: the variables at the loop head satisfy the candidates */
    void startIteration() {
        for (String variable : variables)
            checker.getContext().getLastVariableInstance(variable).ifPresent(instance -> head.put(variable, instance));
        addToLastInstances(candidates);
        dropped = false;
        reachable = true;
        atContinues = null;
        stoppedAt = null;
    }

    void recordContinue() {
        List<Predicate> proved = provedForNextIteration();
        if (atContinues != null)
            proved.retainAll(atContinues);
        atContinues = proved;
    }

    /**
     * At the end of the body, where its continues join, before the values there are forgotten
     *
     * @param completes
     *            whether the end of the body can be reached without a continue
     *
     * @return the facts that hold on every path that reaches the join, for {@link #joined}
     */
    List<Predicate> atJoin(boolean completes) {
        if (completes)
            recordContinue();
        return atContinues == null ? List.of() : atContinues;
    }

    /** After the continues joined: the new instances of the variables satisfy the facts {@link #atJoin} returned */
    void joined(List<Predicate> facts) {
        addToLastInstances(facts);
    }

    /**
     * Marks the end of the body
     *
     * @param completes
     *            whether the end of the body can be reached without a continue
     */
    void endBody(boolean completes) {
        reachable = completes || atContinues != null;
    }

    /** The next iteration starts here, if this point is reachable: drops the candidates not proved here */
    void recordNextIteration() {
        if (reachable)
            dropped |= candidates.removeIf(candidate -> !proved(candidate));
    }

    /**
     * Whether the iteration just checked dropped no candidate, so it is the last one to check. Once {@link #MAX_ROUNDS}
     * checks dropped some, all are dropped.
     */
    boolean reachedFixpoint() {
        reporting = false;
        if (dropped && ++rounds >= MAX_ROUNDS)
            candidates.clear();
        return !dropped;
    }

    /**
     * After a loop that only exits through its condition, once what it changes is forgotten: the variables satisfy the
     * invariants, and the condition is false, as at the loop head where it exited
     */
    void afterLoop() {
        List<Predicate> facts = new ArrayList<>(candidates);
        InvariantCandidates.condition(condition(loop)).ifPresent(condition -> facts.add(condition.negate()));
        addToLastInstances(facts);
    }

    /**
     * At a fixpoint reached with statements skipped: the next iteration checks them, to report their error
     *
     * @return whether the body is to be checked again
     */
    boolean checkSkippedAgain() {
        if (skipped.isEmpty())
            return false;
        skipped.clear();
        reporting = true;
        return true;
    }

    /** Whether a statement of the body is skipped in the iteration being checked */
    boolean skips(CtStatement statement) {
        return skipped.contains(statement);
    }

    /** Records the statement of the body where an error stops the iteration being checked */
    void stoppedAt(CtStatement statement) {
        if (stoppedAt == null)
            stoppedAt = statement;
    }

    /**
     * After an error stopped the iteration being checked: skips the statement where it stopped, if it can be skipped,
     * else drops all candidates
     *
     * @return whether the body is to be checked again, else the error is reported: it was found from candidates proved
     *         on every path, or from none and with no statement skipped
     */
    boolean checkAgainAfterError(java.util.function.Predicate<CtStatement> canSkip) {
        if (reporting || (candidates.isEmpty() && skipped.isEmpty()))
            return false;
        if (!candidates.isEmpty() && stoppedAt != null && canSkip.test(stoppedAt)) {
            skipped.add(stoppedAt);
        } else {
            candidates.clear();
            skipped.clear();
        }
        return true;
    }

    /** The candidates, and that each variable grew or shrank since the loop head, that are proved here */
    private List<Predicate> provedForNextIteration() {
        List<Predicate> facts = new ArrayList<>(candidates);
        head.forEach((variable, instance) -> {
            Predicate name = Predicate.createVar(variable), atHead = Predicate.createVar(instance.getName());
            facts.add(Predicate.createOperation(name, Ops.GE, atHead));
            facts.add(Predicate.createOperation(name, Ops.LE, atHead));
        });
        facts.removeIf(fact -> !proved(fact));
        return facts;
    }

    /**
     * Adds to the last instance of each variable the facts that mention it, on the last instances of the variables.
     * Unlike a path condition, the refinement of an instance is not dropped when the variable is assigned.
     */
    private void addToLastInstances(List<Predicate> facts) {
        for (String variable : variables) {
            Optional<VariableInstance> last = checker.getContext().getLastVariableInstance(variable);
            if (last.isEmpty())
                continue;
            Predicate refinement = declared.computeIfAbsent(last.get(), RefinedVariable::getRefinement);
            for (Predicate fact : facts)
                if (fact.getVariableNames().contains(variable))
                    refinement = Predicate.createConjunction(refinement, onLastInstances(fact));
            last.get().setRefinement(refinement);
        }
    }

    /** Whether a fact on variable names holds for their last instances, from what is known here */
    private boolean proved(Predicate fact) {
        try {
            return checker.checkStateSMT(new Predicate(), onLastInstances(fact), loop.getPosition(), true);
        } catch (LJError e) {
            return false;
        }
    }

    private Predicate onLastInstances(Predicate fact) {
        Predicate result = fact;
        for (String name : fact.getVariableNames()) {
            Optional<VariableInstance> last = checker.getContext().getLastVariableInstance(name);
            if (last.isPresent())
                result = result.substituteVariable(name, last.get().getName());
        }
        return result;
    }

    /** The condition of a loop, or null if it has none */
    static CtExpression<Boolean> condition(CtLoop loop) {
        if (loop instanceof CtWhile whileLoop)
            return whileLoop.getLoopingExpression();
        if (loop instanceof CtDo doLoop)
            return doLoop.getLoopingExpression();
        if (loop instanceof CtFor forLoop)
            return forLoop.getExpression();
        return null;
    }
}

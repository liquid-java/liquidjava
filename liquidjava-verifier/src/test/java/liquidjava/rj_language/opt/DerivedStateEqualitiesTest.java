package liquidjava.rj_language.opt;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import liquidjava.processor.context.GhostState;
import liquidjava.processor.facade.AliasDTO;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.opt.derivation_node.BinaryDerivationNode;
import liquidjava.rj_language.opt.derivation_node.StateDerivationNode;
import liquidjava.rj_language.opt.derivation_node.ValDerivationNode;
import liquidjava.rj_language.parsing.RefinementsParser;

/**
 * Test suite for state-equality derivation inside the expression simplifier.
 *
 * Derivation rewrites internal ghost-state equalities (e.g. {@code state1(a) == state1(b)}) into developer-facing state
 * predicates (e.g. {@code knownState(b)}) so {@code StateRefinementError} diagnostics show meaningful typestate names.
 */
class DerivedStateEqualitiesTest {

    private static Expression parse(String sut) {
        return RefinementsParser.createAST(sut, "");
    }

    /**
     * Builds a developer state named {@code stateName} whose refinement is backed by the internal ghost-state function
     * {@code internalName} (the {@code stateN} dimension).
     */
    private static GhostState ghostState(String stateName, String internalName) {
        GhostState gs = new GhostState(stateName, null, null, "", "");
        gs.setRefinement(new Predicate(parse(internalName + "(wild) == 0")));
        return gs;
    }

    @Test
    void testBasicDerivationDerivesKnownStateForOtherOperand() {
        Expression expression = parse("state1(a) == state1(b) && knownState(a)");
        List<GhostState> ghostStates = List.of(ghostState("knownState", "state1"));

        ValDerivationNode result = ExpressionSimplifier.simplify(expression, Map.of(), ghostStates);

        assertEquals("knownState(a) && knownState(b)", result.getValue().toString(),
                "Equality should be dropped and knownState(b) derived from knownState(a)");
    }

    @Test
    void testDerivationMatchesRightOperandOfEquality() {
        Expression expression = parse("state1(a) == state1(b) && knownState(b)");
        List<GhostState> ghostStates = List.of(ghostState("knownState", "state1"));

        ValDerivationNode result = ExpressionSimplifier.simplify(expression, Map.of(), ghostStates);

        assertEquals("knownState(b) && knownState(a)", result.getValue().toString(),
                "A known state on the right operand should derive the state for the left operand");
    }

    @Test
    void testChainDerivationResolvesTransitiveEqualities() {
        Expression expression = parse("state1(a) == state1(b) && state1(b) == state1(c) && knownState(a)");
        List<GhostState> ghostStates = List.of(ghostState("knownState", "state1"));

        ValDerivationNode result = ExpressionSimplifier.simplify(expression, Map.of(), ghostStates);

        String actual = result.getValue().toString();
        assertTrue(actual.contains("knownState(b)"), "chain should derive knownState(b), got: " + actual);
        assertTrue(actual.contains("knownState(c)"), "chain should derive knownState(c), got: " + actual);
    }

    @Test
    void testNoMatchingKnownStateKeepsEquality() {
        Expression expression = parse("state1(a) == state1(b) && knownState(c)");
        List<GhostState> ghostStates = List.of(ghostState("knownState", "state1"));

        ValDerivationNode result = ExpressionSimplifier.simplify(expression, Map.of(), ghostStates);

        assertEquals("state1(a) == state1(b) && knownState(c)", result.getValue().toString(),
                "With no known state for either operand the equality is kept and nothing is derived");
    }

    @Test
    void testDerivationFiresOnResolvedTernaryBranchNotRawIte() {
        // The state conjunct is a conditional; mode == 2 makes the (mode == 1 ? ...) condition false, so the
        // asserted state is the else-branch otherState(a). Derivation must fire on that resolved fact, never
        // on the raw Ite (which would wrongly derive knownState from the unreachable then-branch).
        Expression expression = parse(
                "mode == 2 && (mode == 1 ? knownState(a) : otherState(a)) && state1(a) == state1(b)");
        List<GhostState> ghostStates = List.of(ghostState("knownState", "state1"), ghostState("otherState", "state1"));

        ValDerivationNode result = ExpressionSimplifier.simplify(expression, Map.of(), ghostStates);

        String actual = result.getValue().toString();
        assertEquals("otherState(a) && otherState(b)", actual, "Derivation should fire on the resolved else-branch");
        assertFalse(actual.contains("knownState"), "Must not derive from the unreachable then-branch: " + actual);
    }

    @Test
    void testDerivedStateAlreadyPresentIsNotDuplicated() {
        Expression expression = parse("state1(a) == state1(b) && knownState(a) && knownState(b)");
        List<GhostState> ghostStates = List.of(ghostState("knownState", "state1"));

        ValDerivationNode result = ExpressionSimplifier.simplify(expression, Map.of(), ghostStates);

        assertEquals("knownState(a) && knownState(b)", result.getValue().toString(),
                "A derived state already present as a conjunct must not be duplicated");
    }

    @Test
    void testTwoIndependentGhostDimensionsDeriveSeparately() {
        Expression expression = parse(
                "state1(a) == state1(b) && state2(a) == state2(b) && dim1State(a) && dim2State(a)");
        List<GhostState> ghostStates = List.of(ghostState("dim1State", "state1"), ghostState("dim2State", "state2"));

        ValDerivationNode result = ExpressionSimplifier.simplify(expression, Map.of(), ghostStates);

        assertEquals("dim1State(a) && dim2State(a) && dim1State(b) && dim2State(b)", result.getValue().toString(),
                "Each ghost dimension should derive from its own equality only");
    }

    @Test
    void testDerivedStateCarriesStateDerivationNodeProvenance() {
        Expression expression = parse("state1(a) == state1(b) && knownState(a)");
        List<GhostState> ghostStates = List.of(ghostState("knownState", "state1"));

        ValDerivationNode result = ExpressionSimplifier.simplify(expression, Map.of(), ghostStates);

        assertEquals("knownState(a) && knownState(b)", result.getValue().toString());
        assertInstanceOf(BinaryDerivationNode.class, result.getOrigin());
        BinaryDerivationNode conjunction = (BinaryDerivationNode) result.getOrigin();

        // The conjunct that was not derived keeps its original (here origin-less) node.
        ValDerivationNode kept = conjunction.getLeft();
        assertEquals("knownState(a)", kept.getValue().toString());
        assertNull(kept.getOrigin(), "A conjunct that was not derived should keep its original node");

        // The derived conjunct carries a StateDerivationNode recording how it was derived.
        ValDerivationNode derived = conjunction.getRight();
        assertEquals("knownState(b)", derived.getValue().toString());
        assertInstanceOf(StateDerivationNode.class, derived.getOrigin(),
                "A derived state should carry a StateDerivationNode origin");
        StateDerivationNode provenance = (StateDerivationNode) derived.getOrigin();
        assertEquals("state1(a) == state1(b)", provenance.getSourceEquality().getValue().toString(),
                "Provenance should reference the equality the state was derived through");
        assertEquals("knownState(a)", provenance.getSourceState().getValue().toString(),
                "Provenance should reference the known state the derivation started from");
    }

    @Test
    void testDerivationDoesNotFireFromDisjunctionBranch() {
        // A state inside a disjunction is not an asserted fact: the object is in knownState OR otherState,
        // not definitely either. Derivation must not carry such a state across the equality.
        Expression expression = parse("state1(a) == state1(b) && (knownState(a) || otherState(a))");
        List<GhostState> ghostStates = List.of(ghostState("knownState", "state1"), ghostState("otherState", "state1"));

        ValDerivationNode result = ExpressionSimplifier.simplify(expression, Map.of(), ghostStates);

        String actual = result.getValue().toString();
        assertFalse(actual.contains("knownState(b)"), "Must not derive a state from a disjunction branch: " + actual);
        assertFalse(actual.contains("otherState(b)"), "Must not derive a state from a disjunction branch: " + actual);
        assertTrue(actual.contains("state1(a) == state1(b)"),
                "The equality must be kept when nothing can be soundly derived: " + actual);
    }

    @Test
    void testDerivationDoesNotFireFromNegatedState() {
        // !knownState(a) asserts the object is NOT in knownState; it is not a known state to carry across
        // the equality. Derivation must leave the equality intact.
        Expression expression = parse("state1(a) == state1(b) && !knownState(a)");
        List<GhostState> ghostStates = List.of(ghostState("knownState", "state1"));

        ValDerivationNode result = ExpressionSimplifier.simplify(expression, Map.of(), ghostStates);

        String actual = result.getValue().toString();
        assertFalse(actual.contains("knownState(b)"), "Must not derive a state from a negated state: " + actual);
        assertTrue(actual.contains("state1(a) == state1(b)"),
                "The equality must be kept when nothing can be soundly derived: " + actual);
    }
}

package liquidjava.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import liquidjava.processor.context.*;
import liquidjava.rj_language.Predicate;
import liquidjava.utils.Pair;
import liquidjava.utils.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import spoon.Launcher;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

/**
 * Integration tests for complete verification workflows Tests the interaction of utilities, context, predicates, and
 * refinements in realistic scenarios
 */
class VerificationWorkflowIntegrationTest {

    private Context context;
    private Factory factory;

    @BeforeEach
    void setUp() {
        Launcher launcher = new Launcher();
        factory = launcher.getFactory();
        context = Context.getInstance();
        context.reinitializeAllContext();
    }

    @Test
    void testUtilityFunctionsIntegration() {
        // Test Utils with Factory for type resolution
        CtTypeReference<?> intType = Utils.getType("int", factory);
        CtTypeReference<?> stringType = Utils.getType("String", factory);
        CtTypeReference<?> boolType = Utils.getType("boolean", factory);

        assertNotNull(intType, "Int type resolved");
        assertNotNull(stringType, "String type resolved");
        assertNotNull(boolType, "Boolean type resolved");

        assertTrue(intType.isPrimitive(), "Int is primitive");
        assertFalse(stringType.isPrimitive(), "String is not primitive");

        // Test name qualification
        String qualified = Utils.qualifyName("com.example", "MyClass");
        assertEquals("com.example.MyClass", qualified);

        // Reserved names should not be qualified
        assertEquals("old", Utils.qualifyName("com.example", "old"));
        assertEquals("length", Utils.qualifyName("com.example", "length"));

        // Test simple name extraction
        assertEquals("MyClass", Utils.getSimpleName("com.example.MyClass"));
        assertEquals("MyClass", Utils.getSimpleName("MyClass"));
    }

    @Test
    void testPairUtility() {
        // Test Pair utility class in context of verification
        Pair<String, CtTypeReference<?>> varTypePair = new Pair<>("x", factory.Type().integerPrimitiveType());

        assertEquals("x", varTypePair.getFirst());
        assertNotNull(varTypePair.getSecond());

        // Use in a map scenario
        List<Pair<String, String>> argPairs = List.of(new Pair<>("arg1", "int"), new Pair<>("arg2", "String"),
                new Pair<>("arg3", "boolean"));

        assertEquals(3, argPairs.size());
        assertEquals("arg1", argPairs.get(0).getFirst());
        assertEquals("int", argPairs.get(0).getSecond());
    }

    @Test
    void testFunctionPreconditionPostconditionWorkflow() {
        // Complete workflow: Define function with pre/post conditions and verify
        CtTypeReference<Integer> intType = factory.Type().integerPrimitiveType();

        // Create function: int divide(int x, int y) with precondition y != 0
        RefinedFunction divideFunc = new RefinedFunction();
        divideFunc.setName("divide");
        divideFunc.setClass("MathUtils");
        divideFunc.setType(intType);

        // Precondition: y != 0
        Predicate yNotZero = Predicate.createOperation(Predicate.createVar("y"), "!=", Predicate.createLit("0", "int"));

        // Add arguments with refinements
        divideFunc.addArgRefinements("x", intType, new Predicate());
        divideFunc.addArgRefinements("y", intType, yNotZero);

        // Postcondition: result * y == x (approximately)
        Predicate postcondition = Predicate.createEquals(
                Predicate.createOperation(Predicate.createVar("result"), "*", Predicate.createVar("y")),
                Predicate.createVar("x"));
        divideFunc.setRefReturn(postcondition);

        context.addFunctionToContext(divideFunc);

        // Verify function can be retrieved
        RefinedFunction retrieved = context.getFunction("divide", "MathUtils", 2);
        assertNotNull(retrieved, "Function should be retrievable");

        List<Variable> args = retrieved.getArguments();
        assertEquals(2, args.size(), "Should have 2 arguments");

        // Verify second argument has the precondition
        Variable yArg = args.get(1);
        Predicate yRefinement = yArg.getRefinement();
        assertTrue(yRefinement.toString().contains("y"), "y refinement should reference y");
        assertTrue(yRefinement.toString().contains("0"), "y refinement should check for zero");
    }

    @Test
    void testCompleteVariableRefinementWorkflow() {
        // Scenario: Track variable through multiple assignments with refinement updates
        CtTypeReference<Integer> intType = factory.Type().integerPrimitiveType();

        // Initial state: int x;
        Variable x = new Variable("x", intType, new Predicate());
        context.addVarToContext(x);

        // Assignment 1: x = 5;
        VariableInstance x1 = new VariableInstance("x_1", intType,
                Predicate.createEquals(Predicate.createVar("x_1"), Predicate.createLit("5", "int")));
        x.addInstance(x1);
        context.addSpecificVariable(x1);
        context.addRefinementInstanceToVariable("x", "x_1");

        // Verify refinement
        assertTrue(context.hasVariable("x_1"), "Instance should be in context");

        // Assignment 2: x = x + 10;
        VariableInstance x2 = new VariableInstance("x_2", intType, Predicate.createEquals(Predicate.createVar("x_2"),
                Predicate.createOperation(Predicate.createVar("x_1"), "+", Predicate.createLit("10", "int"))));
        x.addInstance(x2);
        context.addSpecificVariable(x2);
        context.addRefinementInstanceToVariable("x", "x_2");

        // At this point, x_2 should be x_1 + 10, which is 5 + 10 = 15
        assertEquals("x_2", x.getLastInstance().get().getName());

        // Get all variables - should include x, x_1, x_2
        List<RefinedVariable> allVars = context.getAllVariables();
        assertTrue(allVars.stream().anyMatch(v -> v.getName().equals("x")));
    }

    @Test
    void testGhostStateVerificationWorkflow() {
        // Scenario: Define ghost states and track state transitions
        context.addGhostClass("Stack");

        // Define states
        List<CtTypeReference<?>> emptyList = List.of();
        GhostState empty = new GhostState("isEmpty", emptyList, factory.Type().booleanPrimitiveType(), "Stack");
        empty.setRefinement(
                Predicate.createEquals(Predicate.createInvocation("Stack.size", Predicate.createVar("this")),
                        Predicate.createLit("0", "int")));

        GhostState nonEmpty = new GhostState("isNonEmpty", emptyList, factory.Type().booleanPrimitiveType(), "Stack");
        nonEmpty.setRefinement(
                Predicate.createOperation(Predicate.createInvocation("Stack.size", Predicate.createVar("this")), ">",
                        Predicate.createLit("0", "int")));

        context.addToGhostClass("Stack", empty);
        context.addToGhostClass("Stack", nonEmpty);

        // Retrieve states
        List<GhostState> states = context.getGhostState("Stack");
        assertEquals(2, states.size(), "Should have 2 states");

        // Verify refinements are set
        assertNotNull(states.get(0).getRefinement(), "First state should have refinement");
        assertNotNull(states.get(1).getRefinement(), "Second state should have refinement");
    }

    @Test
    void testMethodOverloadingResolution() {
        // Test resolution of overloaded methods
        CtTypeReference<Integer> intType = factory.Type().integerPrimitiveType();
        CtTypeReference<String> stringType = factory.Type().stringType();

        // Add overloaded methods: process(int), process(int, int), process(String)
        RefinedFunction process1 = new RefinedFunction();
        process1.setName("process");
        process1.setClass("Processor");
        process1.setType(intType);
        process1.addArgRefinements("x", intType, new Predicate());
        context.addFunctionToContext(process1);

        RefinedFunction process2 = new RefinedFunction();
        process2.setName("process");
        process2.setClass("Processor");
        process2.setType(intType);
        process2.addArgRefinements("x", intType, new Predicate());
        process2.addArgRefinements("y", intType, new Predicate());
        context.addFunctionToContext(process2);

        RefinedFunction process3 = new RefinedFunction();
        process3.setName("process");
        process3.setClass("Processor");
        process3.setType(intType);
        process3.addArgRefinements("s", stringType, new Predicate());
        context.addFunctionToContext(process3);

        // Resolve by arity
        assertNotNull(context.getFunction("process", "Processor", 1), "Should find process(int)");
        assertNotNull(context.getFunction("process", "Processor", 2), "Should find process(int, int)");

        List<RefinedFunction> allWithName1 = context.getAllMethodsWithNameSize("process", 1);
        List<RefinedFunction> allWithName2 = context.getAllMethodsWithNameSize("process", 2);

        assertTrue(allWithName1.size() >= 2, "Should find at least 2 single-arg variants");
        assertEquals(1, allWithName2.size(), "Should find 1 two-arg variant");
    }

    @Test
    void testScopedVariableLifetime() {
        // Test variables in nested scopes with same names
        CtTypeReference<Integer> intType = factory.Type().integerPrimitiveType();

        // Base scope x
        context.addVarToContext("x", intType, Predicate.createLit("0", "int"), factory.createLiteral(0));
        assertEquals(1, context.getAllVariables().size());

        // Enter scope 1
        context.enterContext();
        context.addVarToContext("x", intType, Predicate.createLit("1", "int"), factory.createLiteral(0));
        context.addVarToContext("y", intType, Predicate.createLit("2", "int"), factory.createLiteral(0));
        assertEquals(3, context.getAllVariables().size(), "Global x + scope1 x + scope1 y");

        // Enter scope 2
        context.enterContext();
        context.addVarToContext("z", intType, Predicate.createLit("3", "int"), factory.createLiteral(0));
        assertEquals(4, context.getAllVariables().size());

        // Exit scope 2
        context.exitContext();
        assertEquals(3, context.getAllVariables().size());

        // Exit scope 1
        context.exitContext();
        assertEquals(1, context.getAllVariables().size());
        assertTrue(context.hasVariable("x"), "Base scope x remains");
    }

    @Test
    void testComplexRefinementConjunction() {
        // Build complex refinement from multiple conditions
        Predicate x = Predicate.createVar("x");
        Predicate y = Predicate.createVar("y");
        Predicate z = Predicate.createVar("z");

        // x > 0
        Predicate cond1 = Predicate.createOperation(x, ">", Predicate.createLit("0", "int"));

        // y < 100
        Predicate cond2 = Predicate.createOperation(y, "<", Predicate.createLit("100", "int"));

        // z == x + y
        Predicate cond3 = Predicate.createEquals(z, Predicate.createOperation(x, "+", y));

        // Combine: (x > 0) && (y < 100) && (z == x + y)
        Predicate combined = Predicate.createConjunction(cond1, cond2);
        combined = Predicate.createConjunction(combined, cond3);

        List<String> vars = combined.getVariableNames();
        assertTrue(vars.contains("x") && vars.contains("y") && vars.contains("z"),
                "Should contain all three variables");

        String result = combined.toString();
        assertTrue(result.contains("&&"), "Should contain conjunction operators");
    }

    @Test
    void testTypeResolutionWithArrays() {
        // Test array type resolution through Utils
        CtTypeReference<?> intArray = Utils.getType("int[]", factory);
        assertNotNull(intArray, "Int array type should be resolved");
        assertTrue(intArray.isArray(), "Should be array type");

        // Use in context
        context.addVarToContext("numbers", intArray, new Predicate(), factory.createLiteral(0));
        assertTrue(context.hasVariable("numbers"), "Array variable should be in context");

        RefinedVariable var = context.getVariableByName("numbers");
        assertTrue(var.getType().isArray(), "Variable should have array type");
    }

    @Test
    void testContextResetPreservesGlobals() {
        // Verify context reinitialize preserves what it should
        CtTypeReference<Integer> intType = factory.Type().integerPrimitiveType();

        // Add global variable
        context.addGlobalVariableToContext("GLOBAL_MAX", intType, Predicate.createLit("100", "int"));

        // Add local variable
        context.addVarToContext("local", intType, new Predicate(), factory.createLiteral(0));

        // Add function
        RefinedFunction func = new RefinedFunction();
        func.setName("test");
        func.setClass("TestClass");
        func.setType(intType);
        context.addFunctionToContext(func);

        // Reinitialize (not all)
        context.reinitializeContext();

        // Check what persists
        assertTrue(context.hasVariable("GLOBAL_MAX"), "Global variable persists");
        assertNotNull(context.getFunction("test", "TestClass"), "Function persists");
        assertFalse(context.hasVariable("local"), "Local variable cleared");
    }

    @Test
    void testStringUtilityFunctions() {
        // Test string utility functions
        String stripped = Utils.stripParens("(expression)");
        assertEquals("expression", stripped, "Parens should be stripped");

        String notStripped = Utils.stripParens("expression");
        assertEquals("expression", notStripped, "Non-paren string unchanged");

        String emptyParens = Utils.stripParens("()");
        assertEquals("", emptyParens, "Empty parens result in empty string");
    }

    @Test
    void testVariableInstanceParenting() {
        // Test parent-child relationship between Variable and VariableInstance
        CtTypeReference<Integer> intType = factory.Type().integerPrimitiveType();

        Variable parent = new Variable("x", intType, new Predicate());
        context.addVarToContext(parent);

        VariableInstance child = new VariableInstance("x_1", intType,
                Predicate.createEquals(Predicate.createVar("x_1"), Predicate.createLit("5", "int")));

        parent.addInstance(child);
        context.addSpecificVariable(child);
        context.addRefinementInstanceToVariable("x", "x_1");

        // Verify relationship
        Variable retrievedParent = context.getVariableFromInstance(child);
        assertNotNull(retrievedParent, "Should find parent");
        assertEquals(parent, retrievedParent, "Parent should match");
    }
}

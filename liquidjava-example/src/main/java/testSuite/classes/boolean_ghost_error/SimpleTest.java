package testSuite.classes.boolean_ghost_error;

public class SimpleTest {
	public static void main(String[] args) {
		SimpleStateMachine ssm = new SimpleStateMachine();
		ssm.open();
		ssm.close();
		ssm.execute(); // State Refinement Error
	}
}
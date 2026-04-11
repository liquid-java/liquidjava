package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
class ErrorEnumNull {
	enum Color {
		Red, Green, Blue
	}

	public static void main(String[] args) {
		@Refinement("c == Color.Red || c == Color.Green")
		Color c = null; // Refinement Error
	}
}
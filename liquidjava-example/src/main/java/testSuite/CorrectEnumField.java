package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
class CorrectEnumField {
    enum Color {
        Red, Green, Blue
    }

    @Refinement("color != Color.Blue")
    Color color;

    void setColor(@Refinement("c != Color.Blue") Color c) {
        color = c;
    }

    public static void main(String[] args) {
        CorrectEnumField cef = new CorrectEnumField();
        cef.setColor(Color.Red);   // correct
        cef.setColor(Color.Green); // correct
    }
}

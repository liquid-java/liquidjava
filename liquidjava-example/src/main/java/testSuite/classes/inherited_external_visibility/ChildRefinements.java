package testSuite.classes.inherited_external_visibility;

import liquidjava.specification.ExternalRefinementsFor;

@ExternalRefinementsFor("testSuite.classes.inherited_external_visibility.Child")
public interface ChildRefinements {
    void privateMethod(); // Expect: Warning
    void packageMethod(); // Expect: Warning
    void interfaceStatic(); // Expect: Warning
    void protectedMethod();
}

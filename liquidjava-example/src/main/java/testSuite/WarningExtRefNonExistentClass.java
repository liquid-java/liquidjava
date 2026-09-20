package testSuite;

import liquidjava.specification.ExternalRefinementsFor;

@ExternalRefinementsFor("non.existent.Class") // Expect: Warning
public interface WarningExtRefNonExistentClass {
    public void NonExistentClass(); 
}

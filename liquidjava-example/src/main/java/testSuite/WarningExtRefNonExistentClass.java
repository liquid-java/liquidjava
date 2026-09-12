package testSuite;

import liquidjava.specification.ExternalRefinementsFor;

@ExternalRefinementsFor("non.existent.Class") // Warning
public interface WarningExtRefNonExistentClass {
    public void NonExistentClass(); 
}

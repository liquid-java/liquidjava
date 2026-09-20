package testSuite;

import liquidjava.specification.RefinementAlias;

@RefinementAlias("Positive(v) { v > 0 }") // Expect: Syntax Error
public class ErrorMissingAliasTypeParameter {}

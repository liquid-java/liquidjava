package testSuite;

import liquidjava.specification.RefinementAlias;

// No import for javax.imageio.ImageWriteParam, so the constant in the alias cannot be resolved
@SuppressWarnings("unused")
@RefinementAlias("Explicit(int x) { x == ImageWriteParam.MODE_EXPLICIT }") // Expect: Not Found Error
public class ErrorStaticFinalInAliasMissingImport {
}

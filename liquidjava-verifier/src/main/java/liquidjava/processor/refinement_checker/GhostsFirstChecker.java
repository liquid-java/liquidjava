package liquidjava.processor.refinement_checker;

import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.context.Context;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;

/**
 * First visit to the Spoon AST: registers the state sets and ghosts of every type, so that a spec read afterwards can
 * use those of any type, whatever the order of the types in the package or of the interfaces in a spec file
 */
public class GhostsFirstChecker extends TypeChecker {
    private final Diagnostics diagnostics = Diagnostics.getInstance();

    public GhostsFirstChecker(Context context, Factory factory) {
        super(context, factory);
    }

    @Override
    public <T> void visitCtClass(CtClass<T> ctClass) {
        register(ctClass);
        super.visitCtClass(ctClass);
    }

    @Override
    public <T> void visitCtInterface(CtInterface<T> intrface) {
        if (getExternalRefinement(intrface).isPresent())
            new ExternalRefinementTypeChecker(context, factory).registerGhosts(intrface);
        else
            register(intrface);
        super.visitCtInterface(intrface);
    }

    private void register(CtType<?> type) {
        try {
            registerGhostsAndStateSets(type);
        } catch (LJError e) {
            diagnostics.add(e);
        }
    }
}

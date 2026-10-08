package liquidjava.processor;

import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.ann_generation.FieldGhostsGeneration;
import liquidjava.processor.context.Context;
import liquidjava.processor.refinement_checker.ExternalRefinementTypeChecker;
import liquidjava.processor.refinement_checker.MethodsFirstChecker;
import liquidjava.processor.refinement_checker.RefinementTypeChecker;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.factory.Factory;

/** Finds circular dependencies between packages */
public class RefinementProcessor extends AbstractProcessor<CtPackage> {

    Factory factory;
    Diagnostics diagnostics = Diagnostics.getInstance();

    public RefinementProcessor(Factory factory) {
        this.factory = factory;
    }

    @Override
    public boolean isToBeProcessed(CtPackage pkg) {
        // Only let Spoon invoke us for the root package;
        // we handle sub-packages ourselves to guarantee parent-before-child order.
        return pkg.isUnnamedPackage();
    }

    @Override
    public void process(CtPackage pkg) {
        Context c = Context.getInstance();
        c.reinitializeAllContext();
        processPackage(pkg, c);
    }

    private void processPackage(CtPackage pkg, Context c) {
        try {
            // first pass: gather refinements. External specifications are registered for every type before any class's
            // own methods and constructors, which may rely on them (an unannotated constructor inherits the state of
            // the
            // specified super constructor it calls; an override is checked against its supertype's spec), whatever the
            // order of the types in the package.
            pkg.getTypes().forEach(type -> type.accept(new FieldGhostsGeneration(c, factory)));
            pkg.getTypes().forEach(type -> type.accept(new ExternalRefinementTypeChecker(c, factory)));
            pkg.getTypes().forEach(type -> type.accept(new MethodsFirstChecker(c, factory)));

            // second pass: check refinements
            pkg.getTypes().forEach(type -> {
                type.accept(new RefinementTypeChecker(c, factory));
            });

            // recurse into sub-packages (inheriting context)
            for (CtPackage subPkg : pkg.getPackages()) {
                processPackage(subPkg, c);
            }
        } catch (LJError e) {
            diagnostics.add(e);
        }
    }
}

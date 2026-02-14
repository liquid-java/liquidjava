package liquidjava.processor.refinement_checker;

import java.util.ArrayList;
import java.util.List;

import liquidjava.diagnostics.Diagnostics;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.context.Context;
import liquidjava.processor.refinement_checker.general_checkers.MethodsFunctionsChecker;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

/** First visit to Spoon AST to get the method's refinements */
public class MethodsFirstChecker extends TypeChecker {
    MethodsFunctionsChecker mfc;
    List<String> visitedClasses;
    Diagnostics diagnostics = Diagnostics.getInstance();

    public MethodsFirstChecker(Context context, Factory factory) {
        super(context, factory);
        mfc = new MethodsFunctionsChecker(this);
        visitedClasses = new ArrayList<>();
    }

    @Override
    public <T> void visitCtClass(CtClass<T> ctClass) {
        context.reinitializeContext();
        if (visitedClasses.contains(ctClass.getQualifiedName()))
            return;
        else
            visitedClasses.add(ctClass.getQualifiedName());
        // visitInterfaces
        if (!ctClass.getSuperInterfaces().isEmpty())
            for (CtTypeReference<?> t : ctClass.getSuperInterfaces()) {
                if (t.isInterface()) {
                    CtType<?> ct = t.getDeclaration();
                    if (ct instanceof CtInterface)
                        visitCtInterface((CtInterface<?>) ct);
                }
            }
        // visitSubclasses
        CtTypeReference<?> sup = ctClass.getSuperclass();
        if (sup != null && sup.isClass()) {
            CtType<?> ct = sup.getDeclaration();
            if (ct instanceof CtClass)
                visitCtClass((CtClass<?>) ct);
        }
        // first try-catch: process class-level annotations)
        // errors here should not prevent visiting methods, constructors or fields of the class
        try {
            getRefinementFromAnnotation(ctClass);
            handleStateSetsFromAnnotation(ctClass);
        } catch (LJError e) {
            diagnostics.add(e);
        }
        // second try-catch: visit class children (methods, constructors, fields)
        // errors from one child should not prevent visiting sibling elements
        try {
            super.visitCtClass(ctClass);
        } catch (LJError e) {
            diagnostics.add(e);
        }
    }

    @Override
    public <T> void visitCtInterface(CtInterface<T> intrface) {
        if (visitedClasses.contains(intrface.getQualifiedName()))
            return;
        else
            visitedClasses.add(intrface.getQualifiedName());
        if (getExternalRefinement(intrface).isPresent())
            return;

        // first try-catch: process interface-level annotations
        // errors here should not prevent visiting the interface's methods
        try {
            getRefinementFromAnnotation(intrface);
            handleStateSetsFromAnnotation(intrface);
        } catch (LJError e) {
            diagnostics.add(e);
        }
        // second try-catch: visit interface children (methods)
        // errors from one child should not prevent visiting sibling methods
        try {
            super.visitCtInterface(intrface);
        } catch (LJError e) {
            diagnostics.add(e);
        }
    }

    @Override
    public <T> void visitCtConstructor(CtConstructor<T> c) {
        context.enterContext();
        try {
            getRefinementFromAnnotation(c);
            mfc.getConstructorRefinements(c);
            super.visitCtConstructor(c);
        } catch (LJError e) {
            diagnostics.add(e);
        }
        context.exitContext();
    }

    public <R> void visitCtMethod(CtMethod<R> method) {
        context.enterContext();
        try {
            mfc.getMethodRefinements(method);
            super.visitCtMethod(method);
        } catch (LJError e) {
            diagnostics.add(e);
        }
        context.exitContext();
    }
}

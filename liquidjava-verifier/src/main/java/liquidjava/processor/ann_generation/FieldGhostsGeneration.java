package liquidjava.processor.ann_generation;

import liquidjava.processor.context.Context;
import liquidjava.specification.Ghost;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

public class FieldGhostsGeneration extends CtScanner {
    Context context;
    Factory factory;

    public FieldGhostsGeneration(Context context, Factory factory) {
        this.context = context;
        this.factory = factory;
    }

    public Context getContext() {
        return context;
    }

    public Factory getFactory() {
        return factory;
    }

    @Override
    public <T> void visitCtClass(CtClass<T> ctClass) {
        ctClass.getDeclaredFields().stream()
                .filter(fld -> !(fld.isStatic() && fld.isFinal()) && fld.getType().getQualifiedName().equals("int"))
                .forEach(fld -> {
                    CtTypeReference<?> fldType = fld.getType();
                    CtAnnotation<?> genAnn = factory.createAnnotation(factory.createCtTypeReference(Ghost.class));
                    genAnn.addValue("value", fldType.getSimpleName() + " " + fld.getSimpleName());
                    ctClass.addAnnotation(genAnn);
                });

        super.visitCtClass(ctClass);
    }
}

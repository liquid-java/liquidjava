package liquidjava.processor.refinement_checker.general_checkers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.context.*;
import liquidjava.processor.refinement_checker.TypeChecker;
import liquidjava.utils.constants.Formats;
import liquidjava.utils.constants.Keys;
import liquidjava.processor.refinement_checker.object_checkers.AuxHierarchyRefinementsPassage;
import liquidjava.processor.refinement_checker.object_checkers.AuxStateHandler;
import liquidjava.rj_language.Predicate;
import liquidjava.utils.Utils;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

public class MethodsFunctionsChecker {

    private final TypeChecker rtc;

    public MethodsFunctionsChecker(TypeChecker rtc) {
        this.rtc = rtc;
    }

    public void getConstructorRefinements(CtConstructor<?> c) throws LJError {
        RefinedFunction f = new RefinedFunction();
        f.setName(c.getSimpleName());
        f.setType(c.getType());
        handleFunctionRefinements(f, c, c.getParameters());
        f.setRefReturn(new Predicate());
        CtTypeReference<?> declaring = c.getDeclaringType() != null ? c.getDeclaringType().getReference() : null;
        if (declaring != null) {
            f.setSignature(String.format("%s.%s", declaring.getQualifiedName(), c.getSignature()));
        } else {
            f.setSignature(c.getSignature());
        }
        if (c.getParent()instanceof CtClass<?> klass) {
            f.setClass(klass.getQualifiedName());
        }
        rtc.getContext().addFunctionToContext(f);
        AuxStateHandler.handleConstructorState(c, f, rtc);
    }

    public void getConstructorInvocationRefinements(CtConstructorCall<?> ctConstructorCall) throws LJError {
        CtExecutableReference<?> exe = ctConstructorCall.getExecutable();
        if (exe != null) {
            RefinedFunction f = rtc.getContext().getFunction(exe.getSimpleName(),
                    exe.getDeclaringType().getQualifiedName(), ctConstructorCall.getArguments().size());
            if (f != null) {
                Map<String, String> map = checkInvocationRefinements(ctConstructorCall,
                        ctConstructorCall.getArguments(), ctConstructorCall.getTarget(), f.getName(),
                        f.getTargetClass());
                AuxStateHandler.constructorStateMetadata(Keys.REFINEMENT, f, map, ctConstructorCall);
            }
        }
    }

    // ################### VISIT METHOD ##############################
    public <R> void getMethodRefinements(CtMethod<R> method) throws LJError {
        RefinedFunction f = new RefinedFunction();
        f.setName(method.getSimpleName().replaceAll("\\p{C}", "")); // remove any empty chars from string
        f.setType(method.getType());
        f.setRefReturn(new Predicate());

        CtClass<?> klass = null;
        if (method.getParent() instanceof CtClass) {
            klass = (CtClass<?>) method.getParent();
            f.setClass(klass.getQualifiedName());
        }
        if (method.getParent()instanceof CtInterface<?> inter) {
            f.setClass(inter.getQualifiedName());
        }
        String owner = f.getTargetClass();
        if (owner != null)
            f.setSignature(String.format("%s.%s", owner, method.getSignature()));
        else
            f.setSignature(method.getSignature());
        rtc.getContext().addFunctionToContext(f);

        auxGetMethodRefinements(method, f);
        String prefix = method.getDeclaringType().getQualifiedName();
        AuxStateHandler.handleMethodState(method, f, rtc, prefix);

        if (klass != null)
            AuxHierarchyRefinementsPassage.checkFunctionInSupertypes(klass, method, f, rtc);
    }

    public <R> void getMethodRefinements(CtMethod<R> method, String prefix) throws LJError {
        String constructorName = "<init>";
        String k = Utils.getSimpleName(prefix);

        String functionName = String.format("%s.%s", prefix, method.getSimpleName());
        if (k.equals(method.getSimpleName())) { // is a constructor
            functionName = String.format(constructorName);
        }

        RefinedFunction f = new RefinedFunction();
        f.setName(functionName.replaceAll("\\p{C}", "")); // remove any empty chars from string
        f.setType(method.getType());
        f.setRefReturn(new Predicate());
        f.setClass(prefix);
        f.setSignature(String.format("%s.%s", prefix, method.getSignature()));
        rtc.getContext().addFunctionToContext(f);
        auxGetMethodRefinements(method, f);

        AuxStateHandler.handleMethodState(method, f, rtc, prefix);
        if (functionName.equals(constructorName) && !f.hasStateChange()) {
            AuxStateHandler.setDefaultState(f, rtc);
        }
    }

    private <R> void auxGetMethodRefinements(CtMethod<R> method, RefinedFunction rf) throws LJError {
        // main cannot have refinement - for now
        if (method.getSignature().equals("main(java.lang.String[])"))
            return;
        List<CtParameter<?>> params = method.getParameters();
        Predicate ref = handleFunctionRefinements(rf, method, params);
        method.putMetadata(Keys.REFINEMENT, ref);
    }

    /**
     * Joins all the refinements from parameters and return
     *
     * @param f
     * @param method
     * @param params
     *
     * @return Conjunction of all
     */
    private Predicate handleFunctionRefinements(RefinedFunction f, CtElement method, List<CtParameter<?>> params)
            throws LJError {
        Predicate joint = new Predicate();
        for (CtParameter<?> param : params) {
            String paramName = param.getSimpleName();
            Optional<Predicate> oc = rtc.getRefinementFromAnnotation(param);
            Predicate c = new Predicate();
            if (oc.isPresent())
                c = oc.get().substituteVariable(Keys.WILDCARD, paramName);
            param.putMetadata(Keys.REFINEMENT, c);
            RefinedVariable v = rtc.getContext().addVarToContext(param.getSimpleName(), param.getType(), c, param);
            rtc.getMessageFromAnnotation(param).ifPresent(v::setMessage);
            if (v instanceof Variable)
                f.addArgRefinements((Variable) v);
            joint = Predicate.createConjunction(joint, c);
        }
        Optional<Predicate> oret = rtc.getRefinementFromAnnotation(method);
        Predicate ret = oret.orElse(new Predicate());
        ret = ret.substituteVariable("return", Keys.WILDCARD);
        f.setRefReturn(ret);
        rtc.getMessageFromAnnotation(method).ifPresent(f::setMessage);
        return Predicate.createConjunction(joint, ret);
    }

    public <R> void getReturnRefinements(CtReturn<R> ret) throws LJError {
        CtClass<?> c = ret.getParent(CtClass.class);
        String className = c.getSimpleName();
        if (ret.getReturnedExpression() != null) {
            // check if there are refinements
            if (rtc.getRefinement(ret.getReturnedExpression()) == null)
                ret.getReturnedExpression().putMetadata(Keys.REFINEMENT, new Predicate());
            CtMethod<?> method = ret.getParent(CtMethod.class);
            // check if method has refinements
            if (rtc.getRefinement(method) == null)
                return;
            if (method.getParent() instanceof CtClass) {
                RefinedFunction fi = rtc.getContext().getFunction(method.getSimpleName(),
                        ((CtClass<?>) method.getParent()).getQualifiedName(), method.getParameters().size());
                if (fi == null)
                    return;

                List<Variable> lv = fi.getArguments();
                for (Variable v : lv) {
                    rtc.getContext().addVarToContext(v);
                }

                // Both return and the method have metadata
                String thisName = String.format(Formats.THIS, className);
                rtc.getContext().addInstanceToContext(thisName, c.getReference(), new Predicate(), ret);

                String returnVarName = String.format(Formats.RET, rtc.getContext().getCounter());
                Predicate cretRef = rtc.getRefinement(ret.getReturnedExpression())
                        .substituteVariable(Keys.WILDCARD, returnVarName).substituteVariable(Keys.THIS, returnVarName);
                Predicate cexpectedType = fi.getRefReturn().substituteVariable(Keys.WILDCARD, returnVarName)
                        .substituteVariable(Keys.THIS, returnVarName);

                rtc.getContext().addVarToContext(returnVarName, method.getType(), cretRef, ret);
                rtc.checkSMT(cexpectedType, ret, fi.getMessage());
                rtc.getContext().newRefinementToVariableInContext(returnVarName, cexpectedType);
            }
        }
    }

    // ############################### VISIT INVOCATION
    // ################################3

    public <R> void getInvocationRefinements(CtInvocation<R> invocation) throws LJError {
        CtExecutable<?> method = invocation.getExecutable().getDeclaration();
        if (method == null) {

            CtExecutableReference<?> cte = invocation.getExecutable();

            if (cte != null)
                searchMethodInLibrary(cte, invocation);

        } else if (method.getParent() instanceof CtClass) {
            String ctype = ((CtClass<?>) method.getParent()).getQualifiedName();
            int argSize = invocation.getArguments().size();
            RefinedFunction f = rtc.getContext().getFunction(method.getSimpleName(), ctype, argSize);
            if (f != null) { // inside rtc.context
                checkInvocationRefinements(invocation, invocation.getArguments(), invocation.getTarget(),
                        method.getSimpleName(), ctype);
            }
        }
    }

    public RefinedFunction getRefinementFunction(String methodName, String className, int size) {
        RefinedFunction f = rtc.getContext().getFunction(methodName, className, size);
        if (f == null)
            f = rtc.getContext().getFunction(String.format("%s.%s", className, methodName), className, size);
        return f;
    }

    private void searchMethodInLibrary(CtExecutableReference<?> ctr, CtInvocation<?> invocation) throws LJError {
        CtTypeReference<?> ctref = ctr.getDeclaringType();
        if (ctref == null) {
            // Plan B: get us get the definition from the invocation.
            CtExpression<?> o = invocation.getTarget();
            ctref = o.getType();
        }
        String ctype = (ctref != null) ? ctref.toString() : null;

        String name = ctr.getSimpleName(); // missing
        int argSize = invocation.getArguments().size();
        String qualifiedSignature = null;
        if (ctype != null) {
            qualifiedSignature = String.format("%s.%s", ctype, ctr.getSignature());
            if (rtc.getContext().getFunction(qualifiedSignature, ctype, argSize) != null) {
                checkInvocationRefinements(invocation, invocation.getArguments(), invocation.getTarget(),
                        qualifiedSignature, ctype);
                return;
            }
        }
        String signature = ctr.getSignature();
        if (rtc.getContext().getFunction(signature, ctype, argSize) != null) {
            checkInvocationRefinements(invocation, invocation.getArguments(), invocation.getTarget(), signature, ctype);
            return;
        }
        if (rtc.getContext().getFunction(name, ctype, argSize) != null) { // inside rtc.context
            checkInvocationRefinements(invocation, invocation.getArguments(), invocation.getTarget(), name, ctype);
            return;
        }
        if (qualifiedSignature != null) {
            String completeName = String.format("%s.%s", ctype, name);
            if (rtc.getContext().getFunction(completeName, ctype, argSize) != null) {
                checkInvocationRefinements(invocation, invocation.getArguments(), invocation.getTarget(), completeName,
                        ctype);
            }
        }
    }

    private Map<String, String> checkInvocationRefinements(CtElement invocation, List<CtExpression<?>> arguments,
            CtExpression<?> target, String methodName, String className) throws LJError {
        // -- Part 1: Check if the invocation is possible
        int si = arguments.size();
        RefinedFunction f = rtc.getContext().getFunction(methodName, className, si);
        if (f == null)
            return new HashMap<>();
        Map<String, String> map = mapInvocation(arguments, f);

        if (target != null) {
            AuxStateHandler.checkTargetChanges(rtc, f, target, map, invocation);
        }
        if (f.allRefinementsTrue()) {
            invocation.putMetadata(Keys.REFINEMENT, new Predicate());
            return map;
        }

        checkParameters(invocation, arguments, f, map);

        // -- Part 2: Apply changes
        // applyRefinementsToArguments(element, arguments, f, map);
        Predicate methodRef = f.getRefReturn();

        if (methodRef != null) {
            boolean equalsThis = methodRef.toString().equals("_ == this"); // TODO change for better
            List<String> vars = methodRef.getVariableNames();
            for (String s : vars)
                if (map.containsKey(s))
                    methodRef = methodRef.substituteVariable(s, map.get(s));

            String varName = null;
            if (invocation.getMetadata(Keys.TARGET) != null) {
                VariableInstance vi = (VariableInstance) invocation.getMetadata(Keys.TARGET);
                methodRef = methodRef.substituteVariable(Keys.THIS, vi.getName());
                Variable v = rtc.getContext().getVariableFromInstance(vi);
                if (v != null)
                    varName = v.getName();
            }

            String viName = String.format(Formats.INSTANCE, f.getName(), rtc.getContext().getCounter());
            VariableInstance vi = (VariableInstance) rtc.getContext().addInstanceToContext(viName, f.getType(),
                    methodRef.substituteVariable(Keys.WILDCARD, viName), invocation); // TODO REVIEW!!
            if (varName != null && f.hasStateChange() && equalsThis)
                rtc.getContext().addRefinementInstanceToVariable(varName, viName);
            invocation.putMetadata(Keys.TARGET, vi);
            invocation.putMetadata(Keys.REFINEMENT, methodRef);
        }
        return map;
    }

    private Map<String, String> mapInvocation(List<CtExpression<?>> arguments, RefinedFunction f) {
        Map<String, String> mapInvocation = new HashMap<>();
        List<Variable> functionParams = f.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            Variable fArg = functionParams.get(i);
            CtExpression<?> iArg = arguments.get(i);
            String invStr;
            if (iArg instanceof CtFieldRead) {
                invStr = createVariableRepresentingArgument(iArg, fArg);
            } else if (iArg instanceof CtVariableRead<?> vr) {
                Optional<VariableInstance> ovi = rtc.getContext()
                        .getLastVariableInstance(vr.getVariable().getSimpleName());
                invStr = ovi.map(Refined::getName).orElse(vr.toString());
            } else // create new variable with the argument refinement
                invStr = createVariableRepresentingArgument(iArg, fArg);

            mapInvocation.put(fArg.getName(), invStr);
        }
        return mapInvocation;
    }

    private String createVariableRepresentingArgument(CtExpression<?> iArg, Variable fArg) {
        Predicate met = (Predicate) iArg.getMetadata(Keys.REFINEMENT);
        if (met == null)
            met = new Predicate();
        if (!met.getVariableNames().contains(Keys.WILDCARD))
            met = Predicate.createEquals(Predicate.createVar(Keys.WILDCARD), met);
        String nVar = String.format(Formats.INSTANCE, fArg.getName(), rtc.getContext().getCounter());
        rtc.getContext().addInstanceToContext(nVar, fArg.getType(), met.substituteVariable(Keys.WILDCARD, nVar), iArg);
        return nVar;
    }

    private void checkParameters(CtElement invocation, List<CtExpression<?>> arguments, RefinedFunction f,
            Map<String, String> map) throws LJError {
        List<Variable> functionParams = f.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            Variable fArg = functionParams.get(i);
            Predicate c = fArg.getMainRefinement();
            c = c.substituteVariable(fArg.getName(), map.get(fArg.getName()));
            List<String> vars = c.getVariableNames();
            for (String s : vars)
                if (map.containsKey(s))
                    c = c.substituteVariable(s, map.get(s));
            if (invocation.getMetadata(Keys.TARGET) != null) {
                VariableInstance vi = (VariableInstance) invocation.getMetadata(Keys.TARGET);
                c = c.substituteVariable(Keys.THIS, vi.getName());
            }
            rtc.checkSMT(c, invocation, fArg.getMessage());
        }
    }

    // IN CONSTRUCTION _ NOT USED
    @SuppressWarnings("unused")
    private void applyRefinementsToArguments(CtElement element, List<CtExpression<?>> arguments, RefinedFunction f,
            Map<String, String> map) {
        Context context = rtc.getContext();
        List<Variable> functionParams = f.getArguments();

        for (int i = 0; i < arguments.size(); i++) {
            Variable fArg = functionParams.get(i);
            Predicate inferredRefinement = fArg.getRefinement();

            CtExpression<?> e = arguments.get(i);
            if (e instanceof CtVariableRead<?> v) {
                String varName = v.getVariable().getSimpleName(); // TODO CHANGE
                RefinedVariable rv = context.getVariableByName(varName);
                String instanceName = String.format(Formats.INSTANCE, varName, context.getCounter());

                inferredRefinement = inferredRefinement.substituteVariable(fArg.getName(), instanceName);
                context.addInstanceToContext(instanceName, rv.getType(), inferredRefinement, element);
                context.addRefinementInstanceToVariable(varName, instanceName);
            } // TODO else's?
        }
    }

    public void loadFunctionInfo(CtExecutable<?> method) {
        String className = null;
        if (method.getParent() instanceof CtClass) {
            className = ((CtClass<?>) method.getParent()).getQualifiedName();
        } else if (method.getParent() instanceof CtInterface<?>) {
            className = ((CtInterface<?>) method.getParent()).getQualifiedName();
        }
        if (className != null) {
            RefinedFunction fi = getRefinementFunction(method.getSimpleName(), className,
                    method.getParameters().size());
            if (fi != null) {
                List<Variable> lv = fi.getArguments();
                for (Variable v : lv)
                    rtc.getContext().addVarToContext(v);
            }
        }
    }
}

package liquidjava.processor.refinement_checker.general_checkers;

import java.util.ArrayList;
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
        f.setPlacementInCode(c);
        handleFunctionRefinements(f, c, c.getParameters());
        f.setRefReturn(new Predicate());
        CtTypeReference<?> declaring = c.getDeclaringType() != null ? c.getDeclaringType().getReference() : null;
        if (declaring != null) {
            f.setSignature(Utils.qualifyName(declaring.getQualifiedName(), c.getSignature()));
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
            List<CtTypeReference<?>> paramTypes = exe.getParameters();
            RefinedFunction f = rtc.getContext().getFunction(exe.getSimpleName(),
                    exe.getDeclaringType().getQualifiedName(), paramTypes);
            if (f != null) {
                // explicit constructor refinements
                Map<String, String> map = checkInvocationRefinements(ctConstructorCall,
                        ctConstructorCall.getArguments(), ctConstructorCall.getTarget(), f.getName(),
                        f.getTargetClass(), paramTypes);
                AuxStateHandler.constructorStateMetadata(Keys.REFINEMENT, f, map, ctConstructorCall);
            } else {
                // default constructor refinements
                CtTypeReference<?> type = exe.getDeclaringType() != null ? exe.getDeclaringType()
                        : ctConstructorCall.getType();
                if (type != null)
                    ctConstructorCall.putMetadata(Keys.REFINEMENT, AuxStateHandler.getDefaultState(rtc,
                            type.getQualifiedName(), Predicate.createVar(Keys.THIS)));
            }
        }
    }

    // ################### VISIT METHOD ##############################
    public <R> void getMethodRefinements(CtMethod<R> method) throws LJError {
        String className = parentQualifiedName(method);
        String signature = (className != null) ? Utils.qualifyName(className, method.getSignature())
                : method.getSignature();
        RefinedFunction f = buildAndRegisterFunction(method, method.getSimpleName(), className, signature);

        String prefix = method.getDeclaringType().getQualifiedName();
        AuxStateHandler.handleMethodState(method, f, rtc, prefix);

        if (method.getParent()instanceof CtClass<?> klass)
            AuxHierarchyRefinementsPassage.checkFunctionInSupertypes(klass, method, f, rtc);
    }

    public <R> void getMethodRefinements(CtMethod<R> method, String prefix) throws LJError {
        String constructorName = "<init>";
        boolean isConstructor = Utils.getSimpleName(prefix).equals(method.getSimpleName());
        String functionName = isConstructor ? constructorName : Utils.qualifyName(prefix, method.getSimpleName());
        String signature = Utils.qualifyName(prefix, method.getSignature());

        RefinedFunction f = buildAndRegisterFunction(method, functionName, prefix, signature);

        AuxStateHandler.handleMethodState(method, f, rtc, prefix);
        if (isConstructor && !f.hasStateChange()) {
            AuxStateHandler.setDefaultState(f, rtc);
        }
    }

    /**
     * Creates a {@link RefinedFunction} with the sanitized name, type, empty return refinement, placement, optional
     * owning class and signature, and registers it in the context, and processes its parameter/return
     */
    private RefinedFunction buildAndRegisterFunction(CtMethod<?> method, String name, String className,
            String signature) throws LJError {
        RefinedFunction f = new RefinedFunction();
        f.setName(name.replaceAll("\\p{C}", "")); // remove any empty chars from string
        f.setType(method.getType());
        f.setRefReturn(new Predicate());
        f.setPlacementInCode(method);
        if (className != null)
            f.setClass(className);
        f.setSignature(signature);
        rtc.getContext().addFunctionToContext(f);
        auxGetMethodRefinements(method, f);
        return f;
    }

    /**
     * Qualified name of the type ({@link CtClass} or {@link CtInterface}) declaring {@code element}, or {@code null}.
     */
    private static String parentQualifiedName(CtElement element) {
        CtElement parent = element.getParent();
        if (parent instanceof CtClass<?> c)
            return c.getQualifiedName();
        if (parent instanceof CtInterface<?> i)
            return i.getQualifiedName();
        return null;
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
        if (ret.getReturnedExpression() == null) {
            return;
        }

        // check if there are refinements
        if (rtc.getRefinement(ret.getReturnedExpression()) == null)
            ret.getReturnedExpression().putMetadata(Keys.REFINEMENT, new Predicate());
        CtMethod<?> method = ret.getParent(CtMethod.class);

        // check if method has refinements
        if (rtc.getRefinement(method) == null || !(method.getParent() instanceof CtClass))
            return;

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

    // ############################### VISIT INVOCATION
    // ################################

    public <R> void getInvocationRefinements(CtInvocation<R> invocation) throws LJError {
        CtExecutable<?> method = invocation.getExecutable().getDeclaration();
        if (method == null) {

            CtExecutableReference<?> cte = invocation.getExecutable();

            if (cte != null)
                searchMethodInLibrary(cte, invocation);

        } else if (method.getParent() instanceof CtClass) {
            String ctype = ((CtClass<?>) method.getParent()).getQualifiedName();
            List<CtTypeReference<?>> paramTypes = invocation.getExecutable().getParameters();
            RefinedFunction f = rtc.getContext().getFunction(method.getSimpleName(), ctype, paramTypes);
            if (f != null) { // inside rtc.context
                checkInvocationRefinements(invocation, invocation.getArguments(), invocation.getTarget(),
                        method.getSimpleName(), ctype, paramTypes);
            }
        }
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
        List<CtTypeReference<?>> paramTypes = ctr.getParameters();

        // Try each candidate key in order; a null key (when ctype is unknown) is skipped.
        String qualifiedSignature = (ctype != null) ? Utils.qualifyName(ctype, ctr.getSignature()) : null;
        String completeName = (ctype != null) ? Utils.qualifyName(ctype, name) : null;

        if (tryRefinements(invocation, qualifiedSignature, ctype, paramTypes)
                || tryRefinements(invocation, ctr.getSignature(), ctype, paramTypes)
                || tryRefinements(invocation, name, ctype, paramTypes)) {
            return;
        }
        tryRefinements(invocation, completeName, ctype, paramTypes);
    }

    /**
     * Look up {@code key} in the context and, if a refined function is found, check the invocation against it. Returns
     * whether a match was found. A {@code null} key is treated as no match.
     */
    private boolean tryRefinements(CtInvocation<?> invocation, String key, String ctype,
            List<CtTypeReference<?>> paramTypes) throws LJError {
        if (key == null) {
            return false;
        }
        RefinedFunction f = rtc.getContext().getFunction(key, ctype, paramTypes);
        if (f == null) {
            return false;
        }
        checkInvocationRefinements(invocation, invocation.getArguments(), invocation.getTarget(), key, ctype,
                paramTypes);
        return true;
    }

    private Map<String, String> checkInvocationRefinements(CtElement invocation, List<CtExpression<?>> arguments,
            CtExpression<?> target, String methodName, String className, List<CtTypeReference<?>> paramTypes)
            throws LJError {
        // -- Part 1: Check if the invocation is possible
        RefinedFunction f = null;
        if (paramTypes != null)
            f = rtc.getContext().getFunction(methodName, className, paramTypes);
        if (f == null)
            return new HashMap<>();
        Map<String, String> map = mapInvocation(arguments, f);

        String returnViName = prepareReturnInstance(invocation, f);

        if (target != null)
            AuxStateHandler.prepareInvocationTarget(rtc, target, invocation);

        if (f.allRefinementsTrue()) {
            if (target != null)
                AuxStateHandler.checkTargetChanges(rtc, f, target, map, invocation);

            // Expose `_ == returnViName` so the if-condition path variable ties to this return value.
            Predicate returnRef = returnViName != null
                    ? Predicate.createEquals(Predicate.createVar(Keys.WILDCARD), Predicate.createVar(returnViName))
                    : new Predicate();
            invocation.putMetadata(Keys.REFINEMENT, returnRef);
            return map;
        }

        checkParameters(invocation, arguments, f, map);

        if (target != null)
            AuxStateHandler.checkTargetChanges(rtc, f, target, map, invocation);

        // -- Part 2: Apply changes
        applyReturnRefinement(invocation, f, map, returnViName);
        return map;
    }

    /**
     * Computes a stable return-value instance name so {@code _}/{@code return} in a {@code @StateRefinement to=} clause
     * matches the post-call VariableInstance. Returns {@code null} for void methods. When the function has no
     * refinements to check, the (empty) instance is registered eagerly.
     */
    private String prepareReturnInstance(CtElement invocation, RefinedFunction f) {
        CtTypeReference<?> retType = f.getType();
        if (retType == null || "void".equals(retType.toString()))
            return null;
        String returnViName = String.format(Formats.INSTANCE, f.getName(), rtc.getContext().getCounter());
        invocation.putMetadata(Keys.RETURN_VAR_NAME, returnViName);
        if (f.allRefinementsTrue())
            rtc.getContext().addInstanceToContext(returnViName, retType, new Predicate(), invocation);
        return returnViName;
    }

    /**
     * Part 2 of an invocation check: applies the function's return refinement to the context, substituting argument and
     * target variables and registering the resulting return-value instance.
     */
    private void applyReturnRefinement(CtElement invocation, RefinedFunction f, Map<String, String> map,
            String returnViName) {
        Predicate methodRef = f.getRefReturn();
        if (methodRef == null)
            return;

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

        String viName = returnViName != null ? returnViName
                : String.format(Formats.INSTANCE, f.getName(), rtc.getContext().getCounter());
        VariableInstance vi = (VariableInstance) rtc.getContext().addInstanceToContext(viName, f.getType(),
                methodRef.substituteVariable(Keys.WILDCARD, viName), invocation); // TODO REVIEW!!
        if (varName != null && f.hasStateChange() && equalsThis)
            rtc.getContext().addRefinementInstanceToVariable(varName, viName);
        invocation.putMetadata(Keys.TARGET, vi);
        invocation.putMetadata(Keys.REFINEMENT, methodRef);
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

    public void loadFunctionInfo(CtExecutable<?> method) {
        String className = parentQualifiedName(method);
        if (className != null) {
            List<CtTypeReference<?>> paramTypes = new ArrayList<>();
            for (CtParameter<?> p : method.getParameters())
                paramTypes.add(p.getType());
            RefinedFunction fi = rtc.getContext().getFunction(method.getSimpleName(), className, paramTypes);
            if (fi != null) {
                List<Variable> lv = fi.getArguments();
                for (Variable v : lv)
                    rtc.getContext().addVarToContext(v);
            }
        }
    }
}

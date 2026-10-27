package liquidjava.processor.refinement_checker.object_checkers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * A binary constructor whose whole body is a call to a superclass constructor, read from its bytecode: which superclass
 * constructor it calls and what it passes for each parameter. Only that exact shape is recognized, so the object ends
 * up in whatever state the superclass constructor leaves it in:
 *
 * <pre>
 * aload_0
 * (a load of one of its parameters | a constant [checkcast])*
 * invokespecial Superclass.&lt;init&gt;
 * return
 * </pre>
 *
 * Anything else (field writes, method calls, this(...), branches) is not a delegation. Classes are read from the JDK
 * only: the verifier runs without the analyzed program's classpath, and the verifier's own libraries may differ from
 * the versions the program uses. The result therefore depends on the JDK the verifier runs on, which may not be the one
 * the program is compiled against.
 *
 * @param superclass
 *            qualified name of the superclass whose constructor is called
 * @param superParameterTypes
 *            qualified names of that constructor's parameter types
 * @param arguments
 *            what is passed for each of them
 */
public record ConstructorDelegation(String superclass, List<String> superParameterTypes, List<Argument> arguments) {

    /** A value passed to the superclass constructor. */
    public sealed interface Argument permits Parameter,Constant {
    }

    /** The delegating constructor's parameter at {@code index}, unchanged. */
    public record Parameter(int index) implements Argument {
    }

    /**
     * A constant, with its value when a state can refer to it (an {@link Integer} or a {@link Boolean}, by the type of
     * the parameter it is passed for); empty when it cannot (e.g. {@code null}, a string).
     */
    public record Constant(Optional<Object> value) implements Argument {
    }

    private record Key(String className, List<String> parameterTypes) {
    }

    private static final Map<Key, Optional<ConstructorDelegation>> CACHE = new ConcurrentHashMap<>();

    /**
     * How the constructor of {@code className} with these parameter types delegates to a superclass constructor, if its
     * bytecode is available and has exactly that shape.
     */
    public static Optional<ConstructorDelegation> of(String className, List<String> parameterTypes) {
        return CACHE.computeIfAbsent(new Key(className, parameterTypes), k -> read(className, parameterTypes));
    }

    private static Optional<ConstructorDelegation> read(String className, List<String> parameterTypes) {
        String resource = className.replace('.', '/') + ".class";
        try (InputStream in = ClassLoader.getPlatformClassLoader().getResourceAsStream(resource)) {
            if (in == null)
                return Optional.empty();
            ClassReader reader = new ClassReader(in);
            if (reader.getSuperName() == null)
                return Optional.empty(); // java.lang.Object
            ConstructorFinder finder = new ConstructorFinder(reader.getSuperName(), parameterTypes);
            reader.accept(finder, ClassReader.SKIP_DEBUG);
            return finder.delegation();
        } catch (IOException | RuntimeException e) {
            return Optional.empty(); // unreadable or unsupported class file: not known to delegate
        }
    }

    /** Hands the constructor with the wanted parameter types to a {@link DelegationMatcher}. */
    private static final class ConstructorFinder extends ClassVisitor {
        private final String superclass;
        private final List<String> parameterTypes;
        private DelegationMatcher matcher;

        ConstructorFinder(String superclass, List<String> parameterTypes) {
            super(Opcodes.ASM9);
            this.superclass = superclass;
            this.parameterTypes = parameterTypes;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
            Type[] params = Type.getArgumentTypes(descriptor);
            if (!name.equals("<init>")
                    || !Arrays.stream(params).map(Type::getClassName).toList().equals(parameterTypes))
                return null;
            matcher = new DelegationMatcher(superclass, params);
            return matcher;
        }

        Optional<ConstructorDelegation> delegation() {
            return matcher == null ? Optional.empty() : matcher.delegation();
        }
    }

    /**
     * Follows a constructor's instructions through the delegating shape, failing on the first one that does not fit.
     * Labels, line numbers, frames and local variable tables are not instructions and are ignored.
     */
    private static final class DelegationMatcher extends MethodVisitor {
        private enum Step {
            RECEIVER, ARGUMENTS, RETURN, DONE, FAILED
        }

        private final String superclass;
        private final Type[] params;
        /** What each pushed value is, given the type of the superclass parameter it ends up passed for. */
        private final List<Function<Type, Argument>> pushed = new ArrayList<>();
        private boolean lastPushedNull;
        private Step step = Step.RECEIVER;
        private ConstructorDelegation delegation;

        DelegationMatcher(String superclass, Type[] params) {
            super(Opcodes.ASM9);
            this.superclass = superclass;
            this.params = params;
        }

        Optional<ConstructorDelegation> delegation() {
            return step == Step.DONE ? Optional.of(delegation) : Optional.empty();
        }

        @Override
        public void visitVarInsn(int opcode, int slot) {
            if (step == Step.RECEIVER && opcode == Opcodes.ALOAD && slot == 0)
                step = Step.ARGUMENTS;
            else if (step == Step.ARGUMENTS)
                parameterAt(slot).filter(i -> opcode == params[i].getOpcode(Opcodes.ILOAD))
                        .ifPresentOrElse(i -> push(t -> new Parameter(i)), this::fail);
            else
                fail();
        }

        /** The parameter stored in local variable {@code slot} (slot 0 is {@code this}; long and double take two). */
        private Optional<Integer> parameterAt(int slot) {
            int next = 1;
            for (int i = 0; i < params.length; next += params[i].getSize(), i++)
                if (next == slot)
                    return Optional.of(i);
            return Optional.empty();
        }

        @Override
        public void visitInsn(int opcode) {
            if (step == Step.RETURN && opcode == Opcodes.RETURN)
                step = Step.DONE;
            else if (step == Step.ARGUMENTS && opcode == Opcodes.ACONST_NULL) {
                push(t -> new Constant(Optional.empty()));
                lastPushedNull = true;
            } else if (step == Step.ARGUMENTS && opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5)
                pushInt(opcode - Opcodes.ICONST_0);
            else if (step == Step.ARGUMENTS && opcode >= Opcodes.LCONST_0 && opcode <= Opcodes.DCONST_1)
                push(t -> new Constant(Optional.empty()));
            else
                fail();
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            if (step == Step.ARGUMENTS && (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH))
                pushInt(operand);
            else
                fail();
        }

        /** An int constant: a boolean, a number, or a char a state cannot write, depending on the parameter. */
        private void pushInt(int value) {
            push(t -> new Constant(switch (t.getSort()) {
            case Type.BOOLEAN -> Optional.of(value != 0);
            case Type.INT, Type.SHORT, Type.BYTE -> Optional.of(value);
            default -> Optional.empty();
            }));
        }

        private void push(Function<Type, Argument> argument) {
            pushed.add(argument);
            lastPushedNull = false;
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (step == Step.ARGUMENTS && value instanceof Integer i)
                pushInt(i);
            else if (step == Step.ARGUMENTS)
                push(t -> new Constant(Optional.empty())); // strings, classes, other constants
            else
                fail();
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            // javac casts a null argument to pick an overload, e.g. super((Throwable) null)
            if (step != Step.ARGUMENTS || opcode != Opcodes.CHECKCAST || !lastPushedNull)
                fail();
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            Type[] superParams = Type.getArgumentTypes(descriptor);
            if (step != Step.ARGUMENTS || opcode != Opcodes.INVOKESPECIAL || !owner.equals(superclass)
                    || !name.equals("<init>") || superParams.length != pushed.size()) {
                fail();
                return;
            }
            List<Argument> arguments = new ArrayList<>();
            for (int i = 0; i < superParams.length; i++)
                arguments.add(pushed.get(i).apply(superParams[i]));
            delegation = new ConstructorDelegation(Type.getObjectType(superclass).getClassName(),
                    Arrays.stream(superParams).map(Type::getClassName).toList(), arguments);
            step = Step.RETURN;
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            fail();
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrap,
                Object... bootstrapArguments) {
            fail();
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            fail();
        }

        @Override
        public void visitIincInsn(int slot, int increment) {
            fail();
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            fail();
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            fail();
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int dimensions) {
            fail();
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            fail();
        }

        private void fail() {
            step = Step.FAILED;
        }
    }
}

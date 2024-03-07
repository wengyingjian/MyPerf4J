package cn.myperf4j.premain.aop;

import cn.myperf4j.plugin.PluginAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Created by LinShunkang on 2018/4/15
 */
public class ProfilingDynamicMethodVisitor extends AdviceAdapter {

    private static final String PROFILING_ASPECT_INNER_NAME = Type.getInternalName(ProfilingAspect.class);

    private int startTimeIdentifier;

    private String innerClassName;
    private String methodName;

    public ProfilingDynamicMethodVisitor(int access,
                                         String name,
                                         String desc,
                                         MethodVisitor mv, String innerClassName) {
        super(ASM9, mv, access, name, desc);
        this.methodName = name;
        this.innerClassName = innerClassName;
    }

    @Override
    protected void onMethodEnter() {
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
        startTimeIdentifier = newLocal(Type.LONG_TYPE);
        mv.visitVarInsn(LSTORE, startTimeIdentifier);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (PluginAdapter.onMethodExitInject(this, startTimeIdentifier, innerClassName, methodName)) {
            return;
        }

        if ((IRETURN <= opcode && opcode <= RETURN) || opcode == ATHROW) {
            mv.visitVarInsn(LLOAD, startTimeIdentifier);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, PROFILING_ASPECT_INNER_NAME, "profiling",
                    "(JLjava/lang/reflect/Method;)V", false);
        }
    }
}

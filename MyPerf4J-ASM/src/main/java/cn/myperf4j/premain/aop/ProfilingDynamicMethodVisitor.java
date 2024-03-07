package cn.myperf4j.premain.aop;

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
        if ("invoke".equals(methodName) && "com/baomidou/mybatisplus/core/override/MybatisMapperProxy".equals(innerClassName)) {
            //注入开始时间
            mv.visitVarInsn(LLOAD, startTimeIdentifier);

            //注入methopd属性
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/baomidou/mybatisplus/core/override/MybatisMapperProxy", "mapperInterface", "Ljava/lang/Class;"); // 获取 field target 对象

            //注入参数
            Type[] argumentTypes = getArgumentTypes();
            mv.visitLdcInsn(argumentTypes.length + 1);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

            for (int i = 0; i < argumentTypes.length; i++) {
                mv.visitInsn(DUP);
                mv.visitLdcInsn(i);
                // 将参数加载到数组中
                loadArg(i);
                box(argumentTypes[i]);
                mv.visitInsn(AASTORE);
            }

            mv.visitMethodInsn(INVOKESTATIC, PROFILING_ASPECT_INNER_NAME, "dbprof", "(JLjava/lang/Object;[Ljava/lang/Object;)V", false);
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

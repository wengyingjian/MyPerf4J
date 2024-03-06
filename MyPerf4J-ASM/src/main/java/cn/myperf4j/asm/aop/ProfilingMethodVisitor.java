package cn.myperf4j.asm.aop;

import cn.myperf4j.asm.ASMRecorderMaintainer;
import cn.myperf4j.base.MethodTag;
import cn.myperf4j.base.config.Config;
import cn.myperf4j.base.config.ProfilingConfig;
import cn.myperf4j.core.MethodTagMaintainer;
import cn.myperf4j.core.recorder.AbstractRecorderMaintainer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Created by LinShunkang on 2018/4/15
 */
public class ProfilingMethodVisitor extends AdviceAdapter {

    private static final String PROFILING_ASPECT_INNER_NAME = Type.getInternalName(ProfilingAspect.class);

    private static final MethodTagMaintainer methodTagMaintainer = MethodTagMaintainer.getInstance();

    private final AbstractRecorderMaintainer maintainer = ASMRecorderMaintainer.getInstance();

    private final Config.RecorderConfig recorderConf = ProfilingConfig.recorderConfig();

    private final String innerClassName;

    private final String simpleClassName;

    private final String methodName;

    private final int methodTagId;

    private int startTimeIdentifier;

    public ProfilingMethodVisitor(int access, String name, String desc, MethodVisitor mv, String innerClassName, String fullClassName, String simpleClassName, String classLevel, String humanMethodDesc) {
        super(ASM9, mv, access, name, desc);
        this.methodName = name;
        this.methodTagId = methodTagMaintainer.addMethodTag(getMethodTag(fullClassName, simpleClassName, classLevel, name, humanMethodDesc));
        this.innerClassName = innerClassName;
        this.simpleClassName = simpleClassName;
    }

    private MethodTag getMethodTag(String fullClassName, String simpleClassName, String classLevel, String methodName, String humanMethodDesc) {
        return MethodTag.getGeneralInstance(fullClassName, simpleClassName, classLevel, methodName, humanMethodDesc);
    }

    /**
     * 指定字节码修改逻辑：每次方法调用的时候会做什么操作
     * 该方法本身只有在字节码加载的时候调用一次
     */
    @Override
    protected void onMethodEnter() {
        if (profiling()) {
            //记录方法的基本信息：key为id，value为方法元数据
            maintainer.addRecorder(methodTagId, recorderConf.getProfilingParam(innerClassName + "/" + methodName));

            //记录方法进入当前时间
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
            startTimeIdentifier = newLocal(Type.LONG_TYPE);
            mv.visitVarInsn(LSTORE, startTimeIdentifier);
        }
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (targetProfiling(ProfilingAspect.ENDPOINTS_CLASSIFIER)) {
            return;
        }

        if (targetProfiling(ProfilingAspect.RPC_CLASSIFIER)) {
            return;
        }

        if ("com/ebaolife/bedrock/entity/QueryDslBaseDao".equals(innerClassName)) {
            //注入开始时间
            mv.visitVarInsn(LLOAD, startTimeIdentifier);

            //注入this
            mv.visitVarInsn(ALOAD, 0);

            mv.visitLdcInsn(methodName);

            mv.visitMethodInsn(INVOKESTATIC, PROFILING_ASPECT_INNER_NAME, "dbdslprof", "(JLjava/lang/Object;Ljava/lang/String;)V", false);
            return;
        }


        if ("execute".equals(methodName) && "com/xxl/job/core/handler/impl/MethodJobHandler".equals(innerClassName)) {
            //注入开始时间
            mv.visitVarInsn(LLOAD, startTimeIdentifier);

            //注入methopd属性
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/xxl/job/core/handler/impl/MethodJobHandler", "method", "Ljava/lang/reflect/Method;"); // 获取 field target 对象
            mv.visitMethodInsn(INVOKESTATIC, PROFILING_ASPECT_INNER_NAME, "jobprof", "(JLjava/lang/Object;)V", false);
            return;
        }


        if (profiling() && ((IRETURN <= opcode && opcode <= RETURN) || opcode == ATHROW)) {
            mv.visitVarInsn(LLOAD, startTimeIdentifier);
            mv.visitLdcInsn(methodTagId);
            mv.visitMethodInsn(INVOKESTATIC, PROFILING_ASPECT_INNER_NAME, "profiling", "(JI)V", false);
        }
    }

    private boolean targetProfiling(String targetClassifier) {
        String classifier = innerClassName + "#" + methodName;
        if (!targetClassifier.equals(classifier)) {
            return false;
        }

        //第一个参数：开始时间
        mv.visitVarInsn(LLOAD, startTimeIdentifier);

        //第二个参数：方法名称
        mv.visitLdcInsn(classifier);

        //第三个参数：方法原参数
        // 创建一个 Object 数组来存储参数
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

        mv.visitMethodInsn(INVOKESTATIC, "cn/myperf4j/asm/aop/ProfilingAspect", "executeWithArguments", "(JLjava/lang/String;[Ljava/lang/Object;)V", false);
        return true;
    }


    private boolean profiling() {
        return methodTagId >= 0;
    }
}

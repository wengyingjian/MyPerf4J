package cn.myperf4j.premain.aop;

import cn.myperf4j.common.MethodTag;
import cn.myperf4j.common.config.Config;
import cn.myperf4j.common.config.ProfilingConfig;
import cn.myperf4j.core.MethodTagMaintainer;
import cn.myperf4j.core.recorder.AbstractRecorderMaintainer;
import cn.myperf4j.plugin.PluginAdapter;
import cn.myperf4j.premain.ASMRecorderMaintainer;
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
        if (PluginAdapter.onMethodExitInject(this, mv, startTimeIdentifier, innerClassName, methodName)) {
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


        if (profiling() && ((IRETURN <= opcode && opcode <= RETURN) || opcode == ATHROW)) {
            mv.visitVarInsn(LLOAD, startTimeIdentifier);
            mv.visitLdcInsn(methodTagId);
            mv.visitMethodInsn(INVOKESTATIC, PROFILING_ASPECT_INNER_NAME, "profiling", "(JI)V", false);
        }
    }


    private boolean profiling() {
        return methodTagId >= 0;
    }
}

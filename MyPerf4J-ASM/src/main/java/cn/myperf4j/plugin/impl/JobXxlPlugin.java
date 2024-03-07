package cn.myperf4j.plugin.impl;

import cn.myperf4j.core.prometheus.MethodObserver;
import cn.myperf4j.plugin.InjectPlugin;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;

import static cn.myperf4j.core.prometheus.MethodObserver.JOB_METRIC;

public class JobXxlPlugin implements InjectPlugin {

    @Override
    public boolean matches(String classifier) {
        return "com/xxl/job/core/handler/impl/MethodJobHandler#execute".equals(classifier);
    }

    @Override
    public boolean injectFields(MethodVisitor mv) {
        //注入methopd属性
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, "com/xxl/job/core/handler/impl/MethodJobHandler", "method", "Ljava/lang/reflect/Method;"); // 获取 field target 对象
        return true;
    }

    @Override
    public void onMethodExitRecord(long startNanos, Object thisObj, Object fields, Object[] params) {
        Method method = ((Method) fields);
        String uri = method.getDeclaringClass().getSimpleName() + "#" + method.getName();

        long endNanos = System.nanoTime();
        MethodObserver.observe(JOB_METRIC, uri, startNanos, endNanos);
    }
}

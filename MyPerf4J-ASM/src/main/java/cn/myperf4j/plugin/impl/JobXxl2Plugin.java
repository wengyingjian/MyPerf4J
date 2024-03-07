package cn.myperf4j.plugin.impl;

import cn.myperf4j.core.prometheus.MethodObserver;
import cn.myperf4j.plugin.BaseInjectPlugin;
import cn.myperf4j.plugin.FieldArgs;
import org.objectweb.asm.commons.AdviceAdapter;

import java.lang.reflect.Method;
import java.util.Arrays;

import static cn.myperf4j.core.prometheus.MethodObserver.JOB_METRIC;

public class JobXxl2Plugin extends BaseInjectPlugin {

    @Override
    public boolean matches(String classifier) {
        return "com/xxl/job/core/handler/impl/MethodJobHandler#execute".equals(classifier);
    }

    @Override
    public boolean injectFields(AdviceAdapter adapter) {
        return injectFields(adapter, Arrays.asList(
                FieldArgs.builder()
                        .owner("com/xxl/job/core/handler/impl/MethodJobHandler")
                        .name("method")
                        .descriptor("Ljava/lang/reflect/Method;")
                        .build()
        ));
    }

    @Override
    public void onMethodExitRecord(long startNanos, String classifier, Object thisObj, Object[] fields, Object[] params) {
        Method method = ((Method) fields[0]);
        String uri = method.getDeclaringClass().getSimpleName() + "#" + method.getName();

        long endNanos = System.nanoTime();
        MethodObserver.observe(JOB_METRIC, uri, startNanos, endNanos);
    }
}

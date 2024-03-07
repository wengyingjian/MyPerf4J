package cn.myperf4j.plugin.impl;

import cn.myperf4j.core.prometheus.MethodObserver;
import cn.myperf4j.plugin.BaseInjectPlugin;
import cn.myperf4j.plugin.FieldArgs;
import org.objectweb.asm.commons.AdviceAdapter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import static cn.myperf4j.core.prometheus.MethodObserver.DB_METRIC;

public class DBMybtisPlusPlugin extends BaseInjectPlugin {
    @Override
    public boolean matches(String classifier) {
        return "com/baomidou/mybatisplus/core/override/MybatisMapperProxy#invoke".equals(classifier);
    }

    @Override
    public boolean injectParams(AdviceAdapter adapter) {
        return injectAllParams(adapter);
    }

    @Override
    public boolean injectFields(AdviceAdapter adapter) {
        return injectFields(adapter, Arrays.asList(
                FieldArgs.builder()
                        .owner("com/baomidou/mybatisplus/core/override/MybatisMapperProxy")
                        .name("mapperInterface")
                        .descriptor("Ljava/lang/Class;")
                        .build()
        ));
    }

    @Override
    public void onMethodExitRecord(long startNanos, String classifier, Object thisObj, Object[] fields, Object[] params) {
        Class clazz = (Class) fields[0];
        Method method = (Method) params[1];

        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String target = clazz.getSimpleName();
        String uri = Objects.equals(className, target) ?
                String.format("%s#%s", className, methodName)
                : String.format("%s#%s(%s)", className, methodName, target);

        long endNanos = System.nanoTime();
        MethodObserver.observe(DB_METRIC, uri, startNanos, endNanos);

    }
}

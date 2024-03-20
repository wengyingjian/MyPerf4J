package cn.myperf4j.plugin.impl;

import com.ebaolife.bedrock.sidecar.common.util.StrUtils;
import cn.myperf4j.core.prometheus.MethodObserver;
import cn.myperf4j.metric.constant.MetricEnum;
import cn.myperf4j.plugin.BaseInjectPlugin;
import cn.myperf4j.plugin.FieldArgs;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Method;
import java.util.Arrays;

public class RpcFeign9Plugin extends BaseInjectPlugin {

    public static final String RPC_CLASSIFIER = "feign/SynchronousMethodHandler#invoke";

    @Override
    public boolean matches(String classifier) {
        return RPC_CLASSIFIER.equals(classifier);
    }

    @Override
    public boolean injectFields(MethodVisitor mv) {
        return injectFields(mv, Arrays.asList(
                FieldArgs.builder()
                        .owner("feign/SynchronousMethodHandler")
                        .name("metadata")
                        .descriptor("Lfeign/MethodMetadata;")
                        .build()
        ));
    }

    @Override
    public void onMethodExitRecord(long startNanos, String classifier, Object thisObj, Object[] fields, Object[] params) {
        //ClientInsAgentApi2#server()
        String uri = getMetaDataKey(fields[0]);
        uri = StrUtils.formatMethod(uri);
        long endNanos = System.nanoTime();
        MethodObserver.observe(MetricEnum.RPC.getMetric(), uri, startNanos, endNanos);
    }

    /**
     * 通过反射调用 configKey() 方法
     *
     * @param methodMetadata feign/MethodMetadata
     * @return
     */
    private String getMetaDataKey(Object methodMetadata) {
        try {
            Method method = methodMetadata.getClass().getMethod("configKey");
            Object result = method.invoke(methodMetadata);
            return String.valueOf(result);
        } catch (Exception e) {
            return String.valueOf(methodMetadata);
        }
    }

}

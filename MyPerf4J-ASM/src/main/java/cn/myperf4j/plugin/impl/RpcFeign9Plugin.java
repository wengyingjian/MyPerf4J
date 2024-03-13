package cn.myperf4j.plugin.impl;

import cn.myperf4j.common.util.StrUtils;
import cn.myperf4j.core.prometheus.MethodObserver;
import cn.myperf4j.plugin.BaseInjectPlugin;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

import static cn.myperf4j.core.prometheus.MethodObserver.RPC_METRIC;

public class RpcFeign9Plugin extends BaseInjectPlugin {

    public static final String RPC_CLASSIFIER = "feign/hystrix/HystrixInvocationHandler#invoke";

    @Override
    public boolean matches(String classifier) {
        return RPC_CLASSIFIER.equals(classifier);
    }

    @Override
    public boolean injectParams(AdviceAdapter adapter, MethodVisitor mv) {
        return injectAllParams(adapter, mv);
    }

    @Override
    public void onMethodExitRecord(long startNanos, String classifier, Object thisObj, Object[] fields, Object[] params) {
        //ClientInsAgentApi2#server()
        String uri = String.valueOf(params[1]);
        uri = StrUtils.formatMethod(uri);
        long endNanos = System.nanoTime();
        MethodObserver.observe(RPC_METRIC, uri, startNanos, endNanos);

    }

}

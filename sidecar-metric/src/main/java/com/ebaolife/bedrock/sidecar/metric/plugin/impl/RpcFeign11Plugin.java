package com.ebaolife.bedrock.sidecar.metric.plugin.impl;

import com.ebaolife.bedrock.sidecar.metric.plugin.BaseInjectPlugin;
import com.ebaolife.bedrock.sidecar.common.util.StrUtils;
import com.ebaolife.bedrock.sidecar.metric.core.prometheus.MethodObserver;
import com.ebaolife.bedrock.sidecar.metric.constant.MetricEnum;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

public class RpcFeign11Plugin extends BaseInjectPlugin {

    public static final String RPC_CLASSIFIER = "feign/AsyncResponseHandler#handleResponse";

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
        long cost = (long) params[4];
        MethodObserver.observe(MetricEnum.RPC.getMetric(), uri, cost);
    }

}

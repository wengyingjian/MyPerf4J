package com.ebaolife.bedrock.sidecar.metric.core.prometheus;

import com.ebaolife.bedrock.sidecar.metric.core.MethodTagMaintainer;
import com.ebaolife.bedrock.sidecar.common.MethodTag;
import com.ebaolife.bedrock.sidecar.common.util.StrUtils;
import com.ebaolife.bedrock.sidecar.metric.constant.MetricEnum;
import io.prometheus.client.Summary;

public class MethodObserver {

    public static void observe(int methodTagId, long startNanoTime, long endNanoTime) {
        MethodTagMaintainer methodTagMaintainer = MethodTagMaintainer.getInstance();
        MethodTag methodTag = methodTagMaintainer.getMethodTag(methodTagId);

        Summary metric = MetricEnum.getByClassLevel(methodTag.getLevel()).getMetric();

        String uri = methodTag.getSimpleDesc();
        uri = StrUtils.formatMethod(uri);
        observe(metric, uri, startNanoTime, endNanoTime);
    }

    public static void observe(Summary metric, String method, long startNanoTime, long endNanoTime) {
        long cost = (endNanoTime - startNanoTime) / 1000_000;
        observe(metric, method, cost);
    }

    public static void observe(Summary metric, String method, long cost) {
        metric.labels(method, "true").observe((double) cost);
    }
}

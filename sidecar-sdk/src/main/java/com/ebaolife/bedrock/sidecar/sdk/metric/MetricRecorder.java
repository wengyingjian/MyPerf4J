package com.ebaolife.bedrock.sidecar.sdk.metric;

import com.ebaolife.bedrock.sidecar.common.metric.MetricEnum;

public class MetricRecorder {

    public static void record(MetricEnum metricEnum, String uri, long cost) {
        metricEnum.getMetric().labels(uri, "true").observe((double) cost);
    }
}

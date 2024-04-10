package com.ebaolife.bedrock.sidecar.sdk.metric;

import com.ebaolife.bedrock.sidecar.common.metric.MetricEnum;
import io.prometheus.client.Summary;

public class MetricRecorder {

    public static void record(MetricEnum metricEnum, String uri, long cost) {
        metricEnum.getMetric().labels(uri, "true").observe((double) cost);
    }

    /**
     * register a Summary
     * user summary.labels(labelValues).observe(cost);
     *
     * @param metricName metric指标名，如 metric_controller
     * @param labelNames 标签名，如"uri", "success"，必须与labels一致
     */
    public static Summary register(String metricName, String... labelNames) {
        return Summary.build()
                .name(metricName)
                .help(metricName)
                .labelNames(labelNames)
                .register();
    }


}

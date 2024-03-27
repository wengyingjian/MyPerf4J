package com.ebaolife.bedrock.sidecar.metric.core.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 该指标无实际含义
 * 通过有无metric_info判断该应用是否接入metric监控
 */
public class MetricTagExport extends Collector {

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily threadStateFamily = new GaugeMetricFamily("metric_info", "metric_info", Collections.emptyList());
        threadStateFamily.addMetric(Collections.emptyList(), 0);
        return Arrays.asList(threadStateFamily);
    }
}

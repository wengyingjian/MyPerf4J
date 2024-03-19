package cn.myperf4j.common.constant;

import cn.hutool.core.util.StrUtil;
import io.prometheus.client.Summary;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MetricEnum {

    /**
     *
     */
    CONTROLLER("Controller",
            Summary.build()
                    .name("metric_controller")
                    .help("metric_controller")
                    .labelNames("uri", "success")
                    .register()),
    DB("DB",
            Summary.build()
                    .name("metric_db")
                    .help("metric_db")
                    .labelNames("uri", "success")
                    .register()),
    CACHE("Cache",
            Summary.build()
                    .name("metric_cache")
                    .help("metric_cache")
                    .labelNames("uri", "success")
                    .register()),
    RPC("Rpc",
            Summary.build()
                    .name("metric_rpc")
                    .help("metric_rpc")
                    .labelNames("uri", "success")
                    .register()),
    OTHER("Other",
            Summary.build()
                    .name("metric_other")
                    .help("metric_other")
                    .labelNames("uri", "success")
                    .register());

    private final String classLevel;
    /**
     * metric指标名称
     */
    private final Summary metric;

    public static MetricEnum getByClassLevel(String classLevel) {
        for (MetricEnum metricEnum : values()) {
            if (StrUtil.equals(metricEnum.getClassLevel(), classLevel)) {
                return metricEnum;
            }
        }
        return MetricEnum.OTHER;
    }
}
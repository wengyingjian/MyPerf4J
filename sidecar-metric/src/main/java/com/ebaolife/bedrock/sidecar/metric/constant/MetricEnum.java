package com.ebaolife.bedrock.sidecar.metric.constant;

import cn.hutool.core.util.StrUtil;
import com.ebaolife.bedrock.sidecar.common.constant.ClassLevelEnum;
import io.prometheus.client.Summary;
import lombok.AllArgsConstructor;
import lombok.Getter;

;

@AllArgsConstructor
@Getter
public enum MetricEnum {

    /**
     *
     */
    CONTROLLER(ClassLevelEnum.CONTROLLER,
            Summary.build()
                    .name("metric_controller")
                    .help("metric_controller")
                    .labelNames("uri", "success")
                    .register()),
    DB(ClassLevelEnum.DB,
            Summary.build()
                    .name("metric_db")
                    .help("metric_db")
                    .labelNames("uri", "success")
                    .register()),
    CACHE(ClassLevelEnum.CACHE,
            Summary.build()
                    .name("metric_cache")
                    .help("metric_cache")
                    .labelNames("uri", "success")
                    .register()),
    RPC(ClassLevelEnum.RPC,
            Summary.build()
                    .name("metric_rpc")
                    .help("metric_rpc")
                    .labelNames("uri", "success")
                    .register()),
    OTHER(ClassLevelEnum.OTHER,
            Summary.build()
                    .name("metric_other")
                    .help("metric_other")
                    .labelNames("uri", "success")
                    .register());

    private final ClassLevelEnum classLevel;
    /**
     * metric指标名称
     */
    private final Summary metric;

    public static MetricEnum getByClassLevel(String classLevel) {
        for (MetricEnum metricEnum : values()) {
            if (StrUtil.equals(metricEnum.getClassLevel().getCode(), classLevel)) {
                return metricEnum;
            }
        }
        return MetricEnum.OTHER;
    }
}
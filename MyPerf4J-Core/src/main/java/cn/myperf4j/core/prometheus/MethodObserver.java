package cn.myperf4j.core.prometheus;

import cn.myperf4j.base.MethodTag;
import cn.myperf4j.core.MethodTagMaintainer;
import io.prometheus.client.Summary;

public class MethodObserver {

    /**
     * 对外提供的方法监控
     */
    private static final Summary ENDPOINTS_METRIC =
            Summary.build().name("endpoints_metric").help("client call metric").labelNames("uri", "success").register();
    /**
     * 调用数据库监控
     */
    private static final Summary DB_METRIC =
            Summary.build().name("db_metric").help("client call metric").labelNames("uri", "success").register();

    /**
     * 调用redis监控
     */
    private static final Summary REDIS_METRIC =
            Summary.build().name("redis_metric").help("client call metric").labelNames("uri", "success").register();
    /**
     * 调用外部系统监控
     */
    private static final Summary CLIENT_METRIC =
            Summary.build().name("client_metric").help("client call metric").labelNames("uri", "success").register();

    /**
     * 定时任务监控
     */
    private static final Summary JOB_METRIC =
            Summary.build().name("job_metric").help("client call metric").labelNames("uri", "success").register();

    public static void observe(int methodTagId, long startNanoTime, long endNanoTime) {
        MethodTagMaintainer methodTagMaintainer = MethodTagMaintainer.getInstance();
        MethodTag methodTag = methodTagMaintainer.getMethodTag(methodTagId);

        long cost = (endNanoTime - startNanoTime) / 1000_000;
        ENDPOINTS_METRIC.labels(methodTag.getSimpleDesc(), "true").observe((double) cost);
    }


}

package cn.myperf4j.core.prometheus;

import cn.myperf4j.base.MethodTag;
import cn.myperf4j.base.constant.ClassLevels;
import cn.myperf4j.core.MethodTagMaintainer;
import io.prometheus.client.Summary;

public class MethodObserver {

    /**
     * 对外提供的方法监控
     */
    public static final Summary ENDPOINTS_METRIC =
            Summary.build().name("metric_endpoints").help("client call metric").labelNames("uri", "success").register();
    /**
     * 调用数据库监控
     */
    public static final Summary DB_METRIC =
            Summary.build().name("metric_db").help("client call metric").labelNames("uri", "success").register();

    /**
     * 调用redis监控
     */
    public static final Summary REDIS_METRIC =
            Summary.build().name("metric_redis").help("client call metric").labelNames("uri", "success").register();
    /**
     * 调用外部系统监控
     */
    public static final Summary RPC_METRIC =
            Summary.build().name("metric_rpc").help("client call metric").labelNames("uri", "success").register();

    /**
     * 调用外部系统监控
     */
    public static final Summary OTHER_METRIC =
            Summary.build().name("metric_other").help("other metric").labelNames("uri", "success").register();


    /**
     * 定时任务监控
     */
    public static final Summary JOB_METRIC =
            Summary.build().name("metric_job").help("client call metric").labelNames("uri", "success").register();

    public static void observe(int methodTagId, long startNanoTime, long endNanoTime) {
        MethodTagMaintainer methodTagMaintainer = MethodTagMaintainer.getInstance();
        MethodTag methodTag = methodTagMaintainer.getMethodTag(methodTagId);

        Summary metric = OTHER_METRIC;
        if (methodTag.getSimpleDesc().contains("Redisson")) {
            metric = REDIS_METRIC;
        } else if (ClassLevels.DAO.equals(methodTag.getLevel())) {
            metric = DB_METRIC;
        }
        observe(metric, methodTag.getSimpleDesc(), startNanoTime, endNanoTime);
    }

    public static void observe(Summary metric, String method, long startNanoTime, long endNanoTime) {
        long cost = (endNanoTime - startNanoTime) / 1000_000;
        observe(metric, method, cost);
    }

    public static void observe(Summary metric, String method, long cost) {
        metric.labels(method, "true").observe((double) cost);
    }
}

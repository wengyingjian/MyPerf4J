package com.ebaolife.bedrock.sidecar.metric.plugin.impl;

import com.ebaolife.bedrock.sidecar.common.util.Logger;
import com.ebaolife.bedrock.sidecar.metric.constant.MetricEnum;
import com.ebaolife.bedrock.sidecar.metric.core.prometheus.MethodObserver;
import com.ebaolife.bedrock.sidecar.metric.plugin.BaseInjectPlugin;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

import java.lang.reflect.Method;

public class SpringCloudGatewayPlugin extends BaseInjectPlugin {
    @Override
    public boolean matches(String classifier) {
        return "org/springframework/cloud/gateway/handler/FilteringWebHandler#handle".equals(classifier);
    }

    @Override
    public boolean injectParams(AdviceAdapter adapter, MethodVisitor mv) {
        return injectAllParams(adapter, mv);
    }

    @Override
    public void onMethodExitRecord(long startNanos, String classifier, Object thisObj, Object[] fields, Object[] params) {
        try {
            //org.springframework.web.server.ServerWebExchange
            Object exchange = params[0];
            Method exchangeMethod = exchange.getClass().getMethod("getRequiredAttribute", String.class);
            Object router = exchangeMethod.invoke(exchange, "org.springframework.cloud.gateway.support.ServerWebExchangeUtils.gatewayRoute");

            //org.springframework.cloud.gateway.route.Route
            Method routerMethod = router.getClass().getMethod("getId");
            Object routerName = routerMethod.invoke(router);

            long endNanos = System.nanoTime();
            MethodObserver.observe(MetricEnum.CONTROLLER.getMetric(), String.valueOf(routerName), startNanos, endNanos);
        } catch (Exception e) {
            Logger.error("FilteringWebHandler#handle onMethodExitRecord failed", e);
        }
    }

}

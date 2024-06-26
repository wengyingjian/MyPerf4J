package com.ebaolife.bedrock.sidecar.metric.plugin.impl;

import com.ebaolife.bedrock.sidecar.common.util.Logger;
import com.ebaolife.bedrock.sidecar.common.classloader.ClassLoaderHolder;
import com.ebaolife.bedrock.sidecar.common.metric.MetricEnum;
import com.ebaolife.bedrock.sidecar.metric.core.prometheus.MethodObserver;
import com.ebaolife.bedrock.sidecar.metric.plugin.BaseInjectPlugin;

import java.lang.reflect.Method;

public class ZuulPlugin extends BaseInjectPlugin {
    @Override
    public boolean matches(String classifier) {
        return "com/netflix/zuul/ZuulRunner#route".equals(classifier);
    }

    @Override
    public void onMethodExitRecord(long startNanos, String classifier, Object thisObj, Object[] fields, Object[] params) {
        try {
            ClassLoader classLoader = ClassLoaderHolder.getClassLoader("com/netflix/zuul/context/RequestContext");

//            RequestContext context = RequestContext.getCurrentContext();
            Class requestContextClass = classLoader.loadClass("com.netflix.zuul.context.RequestContext");
            Method getCurrentContextMethod = requestContextClass.getMethod("getCurrentContext");
            Object requestContext = getCurrentContextMethod.invoke(null);

//            ctx.get(ZuulConstant.PROXY_ID_KEY);
            Method getMethod = requestContextClass.getMethod("get", Object.class);
            Object routeId = getMethod.invoke(requestContext, "proxy");

            long endNanos = System.nanoTime();
            MethodObserver.observe(MetricEnum.CONTROLLER.getMetric(), String.valueOf(routeId), startNanos, endNanos);
        } catch (Exception e) {
            Logger.error("ZuulServlet#service onMethodExitRecord failed", e);
        }
    }

}

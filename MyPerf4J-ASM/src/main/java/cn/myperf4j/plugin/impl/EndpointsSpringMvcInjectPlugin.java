package cn.myperf4j.plugin.impl;

import cn.myperf4j.common.util.StrUtils;
import cn.myperf4j.core.prometheus.MethodObserver;
import cn.myperf4j.plugin.BaseInjectPlugin;
import org.objectweb.asm.commons.AdviceAdapter;

import static cn.myperf4j.core.prometheus.MethodObserver.ENDPOINTS_METRIC;

public class EndpointsSpringMvcInjectPlugin extends BaseInjectPlugin {


    public static EndpointsSpringMvcInjectPlugin plugin = new EndpointsSpringMvcInjectPlugin();
    public static final String ENDPOINTS_CLASSIFIER = "org/springframework/web/servlet/mvc/method/AbstractHandlerMethodAdapter#handle";

    @Override
    public boolean matches(String classifier) {
        return ENDPOINTS_CLASSIFIER.equals(classifier);
    }

    @Override
    public boolean injectParams(AdviceAdapter adapter) {
        return injectAllParams(adapter);
    }

    @Override
    public void onMethodExitRecord(long startNanos, String classifier, Object thisObj, Object[] fields, Object[] params) {
        //spring 1x *com.ebaolife.leopard.codeflow.group.interfaces.TcfGroupController.findById(java.lang.Long)
        //spring 2x *org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController#errorHtml(HttpServletRequest, HttpServletResponse)
        String uri = String.valueOf(params[2]);
        uri = StrUtils.formatMethod(uri);
        long endNanos = System.nanoTime();
        MethodObserver.observe(ENDPOINTS_METRIC, uri, startNanos, endNanos);
    }

}
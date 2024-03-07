package cn.myperf4j.plugin.impl;

import cn.myperf4j.common.util.StrUtils;
import cn.myperf4j.core.prometheus.MethodObserver;
import cn.myperf4j.plugin.InjectPlugin;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;

import static cn.myperf4j.core.prometheus.MethodObserver.ENDPOINTS_METRIC;

public class EndpointsSpringMvcInjectPlugin implements InjectPlugin {


    public static EndpointsSpringMvcInjectPlugin plugin = new EndpointsSpringMvcInjectPlugin();
    public static final String ENDPOINTS_CLASSIFIER = "org/springframework/web/servlet/mvc/method/AbstractHandlerMethodAdapter#handle";

    @Override
    public boolean matches(String classifier) {
        return ENDPOINTS_CLASSIFIER.equals(classifier);
    }

    @Override
    public boolean injectFields(MethodVisitor mv) {
        return false;
    }


    /**
     * "onMethodExitRecord"
     */
    protected String getMethodName() {
        return "onMethodExitRecord";

    }

    /**
     * "cn/myperf4j/premain/aop/ProfilingAspect"
     */
    private String getOwner() {
        return Type.getInternalName(this.getClass());
    }

    public static void main(String[] args) {
        System.out.println(Type.getInternalName(Type.class));
    }

    /**
     * "(JLjava/lang/String;[Ljava/lang/Object;)V"
     */
    private String getMethodDescriptor() {
        Method[] methods = this.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals(getMethodName())) {
                return Type.getMethodDescriptor(method);
            }
        }
        throw new RuntimeException("no onMethodExitRecord method found for asm inject");
    }

    public void onMethodExitRecord(long startNanos, Object thisObj, Object fields, Object[] params) {
        //spring 1x *com.ebaolife.leopard.codeflow.group.interfaces.TcfGroupController.findById(java.lang.Long)
        //spring 2x *org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController#errorHtml(HttpServletRequest, HttpServletResponse)
        String uri = String.valueOf(params[2]);
        uri = StrUtils.formatMethod(uri);
        long endNanos = System.nanoTime();
        MethodObserver.observe(ENDPOINTS_METRIC, uri, startNanos, endNanos);
    }

}

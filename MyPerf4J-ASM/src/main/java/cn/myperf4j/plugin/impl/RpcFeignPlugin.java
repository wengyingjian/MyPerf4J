package cn.myperf4j.plugin.impl;

import cn.myperf4j.common.util.StrUtils;
import cn.myperf4j.core.prometheus.MethodObserver;
import cn.myperf4j.plugin.InjectPlugin;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;

import static cn.myperf4j.core.prometheus.MethodObserver.RPC_METRIC;

public class RpcFeignPlugin implements InjectPlugin {


    public static RpcFeignPlugin plugin = new RpcFeignPlugin();
    public static final String RPC_CLASSIFIER = "feign/AsyncResponseHandler#handleResponse";


    @Override
    public boolean matches(String classifier) {
        return RPC_CLASSIFIER.equals(classifier);
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
        //ClientInsAgentApi2#server()
        String uri = String.valueOf(params[1]);
        uri = StrUtils.formatMethod(uri);
        long cost = (long) params[4];
        MethodObserver.observe(RPC_METRIC, uri, cost);
    }




}

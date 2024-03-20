package cn.myperf4j.metric.plugin;

import cn.myperf4j.metric.plugin.impl.DBMybtisPlusPlugin;
import cn.myperf4j.metric.plugin.impl.EndpointsSpringMvcInjectPlugin;
import cn.myperf4j.metric.plugin.impl.RpcFeign11Plugin;
import cn.myperf4j.metric.plugin.impl.RpcFeign9Plugin;
import com.ebaolife.bedrock.sidecar.common.util.Logger;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class PluginAdapter {


    public static final List<InjectPlugin> PLUGIN_LIST = new ArrayList<>();

    static {
        PLUGIN_LIST.add(new EndpointsSpringMvcInjectPlugin());
        PLUGIN_LIST.add(new RpcFeign11Plugin());
        PLUGIN_LIST.add(new RpcFeign9Plugin());
        PLUGIN_LIST.add(new DBMybtisPlusPlugin());
    }

    /**
     * 指定字节码修改逻辑：每次方法调用完成后指定操作
     * 该方法本身只有在字节码加载的时候调用一次
     *
     * @return
     * @see "ProfilingMethodVisitor"
     */
    public static boolean onMethodExitInject(AdviceAdapter adapter, MethodVisitor mv, int startTimeIdentifier, String innerClassName, String methodName) {
        //字节码类描述符，用于匹配插件
        String classifier = innerClassName + "#" + methodName;

        InjectPlugin plugin = PLUGIN_LIST.stream().filter(p -> p.matches(classifier)).findFirst().orElse(null);
        if (plugin == null) {
            return false;
        }

        //第一个参数：固定：开始时间
        mv.visitVarInsn(Opcodes.LLOAD, startTimeIdentifier);

        //第二个参数：固定：classifier
        mv.visitLdcInsn(classifier);

        //第三个参数：固定：当前对象
        mv.visitVarInsn(Opcodes.ALOAD, 0);

        //第四个参数：自定义：可以注入需要的属性
        if (!plugin.injectFields(adapter)) {
            //否则，注入空
            mv.visitInsn(Opcodes.ACONST_NULL);
        }

        //第五个参数：自定义：可以注入需要的方法
        if (!plugin.injectParams(adapter, mv)) {
            //否则，注入空
            mv.visitInsn(Opcodes.ACONST_NULL);
        }

        //调用统计方法
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, getOwner(), getMethodName(), getMethodDescriptor(), false);
        return true;
    }

    /**
     * 该方法运行时每次调用到对应的方法都会被调用
     *
     * @param startNanos 方法调用开始时间，可以根据当前时间计算耗时
     * @param classifier 方法描述信息，如要修改的方法是feign feign/AsyncResponseHandler#handleResponse
     * @param targetObj  方法执行的当前对象，如上即 AsyncResponseHandler 对象
     * @param fields     方法执行当前对象的属性，需要自己指定传入
     * @param params     方法的参数
     */
    public static void onMethodExitRecord(long startNanos, String classifier, Object targetObj, Object[] fields, Object[] params) {
        try {
            for (InjectPlugin plugin : PLUGIN_LIST) {
                if (plugin.matches(classifier)) {
                    plugin.onMethodExitRecord(startNanos, classifier, targetObj, fields, params);
                    return;
                }
            }
        } catch (Exception e) {
            Logger.error("onMethodExitRecord failed", e);
        }
    }


    /**
     * "onMethodExitRecord"
     */
    public static String getMethodName() {
        return "onMethodExitRecord";

    }

    /**
     * "cn/myperf4j/premain/aop/ProfilingAspect"
     */
    public static String getOwner() {
        return Type.getInternalName(PluginAdapter.class);
    }

    public static String getMethodDescriptor() {
        Method[] methods = PluginAdapter.class.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(getMethodName())) {
                return Type.getMethodDescriptor(method);
            }
        }
        throw new RuntimeException("no onMethodExitRecord method found for asm inject");
    }


}

package cn.myperf4j.plugin;

import cn.myperf4j.plugin.impl.EndpointsSpringMvcInjectPlugin;
import cn.myperf4j.plugin.impl.JobXxlPlugin;
import cn.myperf4j.plugin.impl.RpcFeignPlugin;
import cn.myperf4j.premain.aop.ProfilingMethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class PluginAdapter {


    private static final List<InjectPlugin> PLUGIN_LIST = new ArrayList<>();

    static {
        PLUGIN_LIST.add(new EndpointsSpringMvcInjectPlugin());
        PLUGIN_LIST.add(new RpcFeignPlugin());
        PLUGIN_LIST.add(new JobXxlPlugin());
    }

    /**
     * 指定字节码修改逻辑：每次方法调用完成后指定操作
     * 该方法本身只有在字节码加载的时候调用一次
     *
     * @return
     * @see ProfilingMethodVisitor
     */
    public static boolean onMethodExitInject(AdviceAdapter adapter, int startTimeIdentifier, String innerClassName, String methodName) {
        //字节码类描述符，用于匹配插件
        String classifier = innerClassName + "#" + methodName;

        for (InjectPlugin plugin : PLUGIN_LIST) {
            if (plugin.matches(classifier)) {
                //第一个参数：固定：开始时间
                adapter.visitVarInsn(Opcodes.LLOAD, startTimeIdentifier);

                //第二个参数：固定：classifier
                adapter.visitLdcInsn(classifier);

                //第三个参数：固定：当前对象
                adapter.visitVarInsn(Opcodes.ALOAD, 0);

                //第四个参数：自定义：可以注入需要的属性
                boolean fields = plugin.injectFields(adapter);
                if (!fields) {
                    adapter.visitInsn(Opcodes.ACONST_NULL);
                }

                //第五个参数：自定义：可以注入需要的方法

                // 创建一个 Object 数组来存储参数
                Type[] argumentTypes = adapter.getArgumentTypes();
                adapter.visitLdcInsn(argumentTypes.length + 1);
                adapter.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

                for (int i = 0; i < argumentTypes.length; i++) {
                    adapter.visitInsn(Opcodes.DUP);
                    adapter.visitLdcInsn(i);
                    // 将参数加载到数组中
                    adapter.loadArg(i);
                    adapter.box(argumentTypes[i]);
                    adapter.visitInsn(Opcodes.AASTORE);
                }
                adapter.visitMethodInsn(Opcodes.INVOKESTATIC, getOwner(), getMethodName(), getMethodDescriptor(), false);
                return true;
            }
        }

        return false;
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
    public static void onMethodExitRecord(long startNanos, String classifier, Object targetObj, Object fields, Object[] params) {
        for (InjectPlugin plugin : PLUGIN_LIST) {
            if (plugin.matches(classifier)) {
                plugin.onMethodExitRecord(startNanos, targetObj, fields, params);
                return;
            }
        }
    }


    /**
     * "onMethodExitRecord"
     */
    private static String getMethodName() {
        return "onMethodExitRecord";

    }

    /**
     * "cn/myperf4j/premain/aop/ProfilingAspect"
     */
    private static String getOwner() {
        return Type.getInternalName(PluginAdapter.class);
    }

    private static String getMethodDescriptor() {
        Method[] methods = PluginAdapter.class.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(getMethodName())) {
                return Type.getMethodDescriptor(method);
            }
        }
        throw new RuntimeException("no onMethodExitRecord method found for asm inject");
    }

    public static void main(String[] args) {
        System.out.println(getMethodDescriptor());
    }


}

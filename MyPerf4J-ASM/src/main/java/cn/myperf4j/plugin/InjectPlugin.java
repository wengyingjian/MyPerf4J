package cn.myperf4j.plugin;

import org.objectweb.asm.commons.AdviceAdapter;

public interface InjectPlugin {

    boolean matches(String classifier);


    boolean injectFields(AdviceAdapter adapter);

    boolean injectParams(AdviceAdapter adapter);

    /**
     * 该方法运行时每次调用到对应的方法都会被调用
     *
     * @param startNanos 方法调用开始时间，可以根据当前时间计算耗时
     * @param classifier
     * @param thisObj    方法执行的当前对象
     * @param fields     方法执行当前对象的属性，需要自己指定传入
     * @param params     方法的参数
     */
    void onMethodExitRecord(long startNanos, String classifier, Object thisObj, Object[] fields, Object[] params);
}

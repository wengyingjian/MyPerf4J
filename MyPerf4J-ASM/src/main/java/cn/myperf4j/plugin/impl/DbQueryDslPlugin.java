package cn.myperf4j.plugin.impl;

import cn.myperf4j.common.util.StrUtils;
import cn.myperf4j.core.prometheus.MethodObserver;
import cn.myperf4j.plugin.BaseInjectPlugin;

import static cn.myperf4j.core.prometheus.MethodObserver.DB_METRIC;

/**
 * 先不实现，依靠业务系统自定义包路径监控
 */
public class DbQueryDslPlugin extends BaseInjectPlugin {
    @Override
    public boolean matches(String classifier) {
        return false;
//        return classifier.contains("com/ebaolife/bedrock/entity/QueryDslBaseDao");
    }

    public static void dbdslprof(long startNanos, Object thisObj, String methodName) {
        String uri = thisObj.getClass().getSimpleName() + '#' + methodName;
        uri = StrUtils.formatMethod(uri);
        long endNanos = System.nanoTime();
        MethodObserver.observe(DB_METRIC, uri, startNanos, endNanos);
    }

    @Override
    public void onMethodExitRecord(long startNanos, String classifier, Object thisObj, Object[] fields, Object[] params) {
//        String uri = thisObj.getClass().getSimpleName() + '#' + methodName;
//        uri = StrUtils.formatMethod(uri);
//        long endNanos = System.nanoTime();
//        MethodObserver.observe(DB_METRIC, uri, startNanos, endNanos);
    }


//        if ("com/ebaolife/bedrock/entity/QueryDslBaseDao".equals(innerClassName)) {
//        //注入开始时间
//        mv.visitVarInsn(LLOAD, startTimeIdentifier);
//
//        //注入this
//        mv.visitVarInsn(ALOAD, 0);
//
//        mv.visitLdcInsn(methodName);
//
//        mv.visitMethodInsn(INVOKESTATIC, PROFILING_ASPECT_INNER_NAME, "dbdslprof", "(JLjava/lang/Object;Ljava/lang/String;)V", false);
//        return;
//    }
}

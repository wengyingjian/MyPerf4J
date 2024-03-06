package cn.myperf4j.asm.aop;

import cn.myperf4j.asm.ASMRecorderMaintainer;
import cn.myperf4j.base.config.ProfilingParams;
import cn.myperf4j.base.util.Logger;
import cn.myperf4j.core.MethodTagMaintainer;
import cn.myperf4j.core.prometheus.MethodObserver;
import cn.myperf4j.core.recorder.Recorder;

import java.lang.reflect.Method;
import java.util.Objects;

import static cn.myperf4j.core.prometheus.MethodObserver.*;

/**
 * Created by LinShunkang on 2018/4/15
 */
public final class ProfilingAspect {

    private static final MethodTagMaintainer methodTagMaintainer = MethodTagMaintainer.getInstance();

    private static ASMRecorderMaintainer recorderMaintainer;

    private static boolean running;

    private ProfilingAspect() {
        //empty
    }

    public static final String ENDPOINTS_CLASSIFIER = "org/springframework/web/servlet/mvc/method/AbstractHandlerMethodAdapter#handle";
    public static final String RPC_CLASSIFIER = "feign/AsyncResponseHandler#handleResponse";


    public static void dbdslprof(long startNanos, Object thisObj, String methodName) {
        String uri = thisObj.getClass().getSimpleName() + '#' + methodName;
        long endNanos = System.nanoTime();
        MethodObserver.observe(DB_METRIC, uri, startNanos, endNanos);
    }


    public static void dbprof(long startNanos, Object fields, Object[] args) {
        Class clazz = (Class) fields;
        Method method = (Method) args[1];

        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String target = clazz.getSimpleName();
        String uri = Objects.equals(className, target) ?
                String.format("%s#%s", className, methodName)
                : String.format("%s#%s(%s)", className, methodName, target);

        long endNanos = System.nanoTime();
        MethodObserver.observe(DB_METRIC, uri, startNanos, endNanos);
    }

    public static void jobprof(long startNanos, Object args) {
        Method method = ((Method) args);
        String uri = method.getDeclaringClass().getSimpleName() + "#" + method.getName();

        long endNanos = System.nanoTime();
        MethodObserver.observe(JOB_METRIC, uri, startNanos, endNanos);
    }

    public static void executeWithArguments(long startNanos, String method, Object[] args) {
        if (ENDPOINTS_CLASSIFIER.equals(method)) {
            String uri = String.valueOf(args[2]);
            uri = uri.substring(uri.lastIndexOf(".") + 1);
            long endNanos = System.nanoTime();
            MethodObserver.observe(ENDPOINTS_METRIC, uri, startNanos, endNanos);
        }

        if (RPC_CLASSIFIER.equals(method)) {
            String uri = String.valueOf(args[1]);
            long cost = (long) args[4];
            MethodObserver.observe(RPC_METRIC, uri, cost);
        }
    }

    public static void profiling(final long startNanos, final int methodTagId) {
        try {
            if (!running) {
                Logger.warn("ProfilingAspect.profiling(): methodTagId=" + methodTagId
                        + ", methodTag=" + MethodTagMaintainer.getInstance().getMethodTag(methodTagId)
                        + ", startNanos: " + startNanos + ", IGNORED!!!");
                return;
            }

            Recorder recorder = recorderMaintainer.getRecorder(methodTagId);
            if (recorder == null) {
                Logger.warn("ProfilingAspect.profiling(): methodTagId=" + methodTagId
                        + ", methodTag=" + MethodTagMaintainer.getInstance().getMethodTag(methodTagId)
                        + ", startNanos: " + startNanos + ", recorder is null IGNORED!!!");
                return;
            }

            long endNanos = System.nanoTime();
            recorder.recordTime(startNanos, endNanos);
            MethodObserver.observe(methodTagId, startNanos, endNanos);
        } catch (Exception e) {
            Logger.error("ProfilingAspect.profiling(" + startNanos + ", " + methodTagId + ", "
                    + MethodTagMaintainer.getInstance().getMethodTag(methodTagId) + ")", e);
        }
    }

    //InvocationHandler.invoke(Object proxy, Method method, Object[] args)
    public static void profiling(final long startNanos, final Method method) {
        try {
            if (!running) {
                Logger.warn("ProfilingAspect.profiling(): method=" + method + ", startNanos: " + startNanos
                        + ", IGNORED!!!");
                return;
            }

            int methodTagId = methodTagMaintainer.addMethodTag(method);
            if (methodTagId < 0) {
                Logger.warn("ProfilingAspect.profiling(): method=" + method + ", startNanos: " + startNanos
                        + ", methodTagId < 0!!!");
                return;
            }

            ASMRecorderMaintainer recMaintainer = ProfilingAspect.recorderMaintainer;
            Recorder recorder = recMaintainer.getRecorder(methodTagId);
            if (recorder == null) {
                synchronized (ProfilingAspect.class) {
                    recorder = recMaintainer.getRecorder(methodTagId);
                    if (recorder == null) {
                        recMaintainer.addRecorder(methodTagId, ProfilingParams.of(3000, 10));
                        recorder = recMaintainer.getRecorder(methodTagId);
                    }
                }
            }

            recorder.recordTime(startNanos, System.nanoTime());
        } catch (Exception e) {
            Logger.error("ProfilingAspect.profiling(" + startNanos + ", " + method + ")", e);
        }
    }

    public static void setRecorderMaintainer(ASMRecorderMaintainer maintainer) {
        synchronized (ProfilingAspect.class) { //强制把值刷新到主存
            recorderMaintainer = maintainer;
        }
    }

    public static void setRunning(boolean run) {
        synchronized (ProfilingAspect.class) { //强制把值刷新到主存
            running = run;
        }
    }
}

package cn.myperf4j.premain.aop;

import cn.myperf4j.common.config.ProfilingParams;
import cn.myperf4j.common.util.Logger;
import cn.myperf4j.common.util.StrUtils;
import cn.myperf4j.core.MethodTagMaintainer;
import cn.myperf4j.core.prometheus.MethodObserver;
import cn.myperf4j.core.recorder.Recorder;
import cn.myperf4j.premain.ASMRecorderMaintainer;

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

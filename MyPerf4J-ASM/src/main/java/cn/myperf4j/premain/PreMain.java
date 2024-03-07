package cn.myperf4j.premain;

import cn.myperf4j.premain.aop.ProfilingTransformer;

import java.lang.instrument.Instrumentation;

/**
 * Created by LinShunkang on 2018/4/25
 */
public final class PreMain {

    private PreMain() {
        //empty
    }

    public static void premain(String options, Instrumentation ins) {
        if (ASMBootstrap.getInstance().initial()) {
            ins.addTransformer(new ProfilingTransformer());
        }
    }
}

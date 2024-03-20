package cn.myperf4j.premain;

import cn.myperf4j.premain.aop.ProfilingTransformer;
import com.taobao.arthas.agent.attach.ArthasAgent;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;

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

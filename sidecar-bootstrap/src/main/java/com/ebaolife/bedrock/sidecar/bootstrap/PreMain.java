package com.ebaolife.bedrock.sidecar.bootstrap;

import com.ebaolife.bedrock.sidecar.arthas.ArthasBootstrap;
import com.ebaolife.bedrock.sidecar.bootstrap.aop.ProfilingTransformer;

import java.lang.instrument.Instrumentation;

/**
 * Created by LinShunkang on 2018/4/25
 */
public final class PreMain {

    public static void premain(String options, Instrumentation ins) {
        //加载配置文件
        if (ASMBootstrap.getInstance().initial()) {
            //启动类加载重写
            ins.addTransformer(new ProfilingTransformer());

            //启动javaagent
            ArthasBootstrap.getInstance().initial();
        }
    }
}

package com.ebaolife.bedrock.sidecar.bootstrap;

import com.ebaolife.bedrock.sidecar.common.config.ProfilingParams;
import com.ebaolife.bedrock.sidecar.metric.core.recorder.AbstractRecorderMaintainer;

/**
 * Created by LinShunkang on 2018/4/26
 */
public class ASMRecorderMaintainer extends AbstractRecorderMaintainer {

    private static final ASMRecorderMaintainer instance = new ASMRecorderMaintainer();

    public static ASMRecorderMaintainer getInstance() {
        return instance;
    }


    @Override
    public void addRecorder(int methodTagId, ProfilingParams params) {
        recorderArr.set(methodTagId,
                createRecorder(methodTagId, params.getMostTimeThreshold(), params.getOutThresholdCount()));
    }

}

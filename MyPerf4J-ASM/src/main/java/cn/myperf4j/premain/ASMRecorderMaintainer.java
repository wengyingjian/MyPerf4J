package cn.myperf4j.premain;

import com.ebaolife.bedrock.sidecar.common.config.ProfilingParams;
import cn.myperf4j.metric.core.recorder.AbstractRecorderMaintainer;

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

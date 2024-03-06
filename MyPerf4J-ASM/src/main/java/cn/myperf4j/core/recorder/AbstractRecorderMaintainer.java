package cn.myperf4j.core.recorder;

import cn.myperf4j.base.config.ProfilingParams;

import java.util.concurrent.atomic.AtomicReferenceArray;

import static cn.myperf4j.core.MethodTagMaintainer.MAX_NUM;

/**
 * Created by LinShunkang on 2018/4/25
 */
public abstract class AbstractRecorderMaintainer {

    protected AtomicReferenceArray<Recorder> recorderArr;

    private boolean accurateMode;

    public boolean initial(boolean accurateMode) {
        this.accurateMode = accurateMode;

        if (!initRecorders()) {
            return false;
        }
        return true;
    }

    private boolean initRecorders() {
        recorderArr = new AtomicReferenceArray<>(MAX_NUM);
        return true;
    }


    protected Recorder createRecorder(int methodTagId, int mostTimeThreshold, int outThresholdCount) {
        if (accurateMode) {
            return AccurateRecorder.getInstance(methodTagId, mostTimeThreshold, outThresholdCount);
        }
        return RoughRecorder.getInstance(methodTagId, mostTimeThreshold);
    }

    public abstract void addRecorder(int methodTagId, ProfilingParams params);

    public Recorder getRecorder(int methodTagId) {
        return recorderArr.get(methodTagId);
    }


}

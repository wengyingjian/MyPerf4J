package cn.myperf4j.core.recorder;

import cn.myperf4j.common.config.ProfilingParams;

import java.util.concurrent.atomic.AtomicReferenceArray;

import static cn.myperf4j.core.MethodTagMaintainer.MAX_NUM;

/**
 * Created by LinShunkang on 2018/4/25
 */
public abstract class AbstractRecorderMaintainer {

    protected AtomicReferenceArray<Recorder> recorderArr;


    public boolean initial() {
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
        return AccurateRecorder.getInstance(methodTagId, mostTimeThreshold, outThresholdCount);
    }

    public abstract void addRecorder(int methodTagId, ProfilingParams params);

    public Recorder getRecorder(int methodTagId) {
        return recorderArr.get(methodTagId);
    }


}

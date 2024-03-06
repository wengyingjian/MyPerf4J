package cn.myperf4j.core.recorder;

import cn.myperf4j.base.config.ProfilingParams;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static cn.myperf4j.base.constant.PropertyValues.Recorder.MAX_BACKUP_RECORDERS_COUNT;
import static cn.myperf4j.base.constant.PropertyValues.Recorder.MIN_BACKUP_RECORDERS_COUNT;
import static cn.myperf4j.core.MethodTagMaintainer.MAX_NUM;

/**
 * Created by LinShunkang on 2018/4/25
 */
public abstract class AbstractRecorderMaintainer {

    protected List<Recorders> recordersList;

    private int curIndex;

    private volatile Recorders curRecorders;

    private boolean accurateMode;

    public boolean initial(boolean accurateMode, int bakRecordersCnt) {
        this.accurateMode = accurateMode;
        bakRecordersCnt = getFitBakRecordersCnt(bakRecordersCnt);

        if (!initRecorders(bakRecordersCnt)) {
            return false;
        }
        return true;
    }

    private int getFitBakRecordersCnt(int backupRecordersCount) {
        if (backupRecordersCount < MIN_BACKUP_RECORDERS_COUNT) {
            return MIN_BACKUP_RECORDERS_COUNT;
        } else if (backupRecordersCount > MAX_BACKUP_RECORDERS_COUNT) {
            return MAX_BACKUP_RECORDERS_COUNT;
        }
        return backupRecordersCount;
    }

    private boolean initRecorders(int backupRecordersCount) {
        recordersList = new ArrayList<>(backupRecordersCount + 1);
        for (int i = 0; i < backupRecordersCount + 1; ++i) {
            recordersList.add(new Recorders(new AtomicReferenceArray<Recorder>(MAX_NUM)));
        }

        curRecorders = recordersList.get(curIndex);
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
        return curRecorders.getRecorder(methodTagId);
    }


}

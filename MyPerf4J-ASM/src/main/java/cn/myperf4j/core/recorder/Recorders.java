package cn.myperf4j.core.recorder;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Created by LinShunkang on 2018/7/7
 */
public class Recorders {

    private final AtomicReferenceArray<Recorder> recorderArr;

    private final AtomicInteger recorderCount;

    private volatile boolean writing;

    private volatile long startTime;

    private volatile long stopTime;

    public Recorders(AtomicReferenceArray<Recorder> recorderArr) {
        this.recorderArr = recorderArr;
        this.recorderCount = new AtomicInteger(0);
    }

    public Recorder getRecorder(int index) {
        return recorderArr.get(index);
    }

    public void setRecorder(int index, Recorder recorder) {
        recorderArr.set(index, recorder);
        recorderCount.incrementAndGet();
    }

    public int size() {
        return recorderArr.length();
    }

    public boolean isWriting() {
        return writing;
    }

    public void setWriting(boolean writing) {
        this.writing = writing;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStopTime() {
        return stopTime;
    }

    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }

    public void resetRecorder() {
        final int count = recorderCount.get();
        final AtomicReferenceArray<Recorder> recorderArr = this.recorderArr;
        for (int i = 0; i < count; ++i) {
            final Recorder recorder = recorderArr.get(i);
            if (recorder != null) {
                recorder.resetRecord();
            }
        }
    }

    @Override
    public String toString() {
        return "Recorders{" +
                "recorderArr=" + recorderArr +
                ", writing=" + writing +
                ", startTime=" + startTime +
                ", stopTime=" + stopTime +
                '}';
    }
}

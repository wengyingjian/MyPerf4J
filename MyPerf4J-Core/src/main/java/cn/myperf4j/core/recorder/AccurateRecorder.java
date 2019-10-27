package cn.myperf4j.core.recorder;

import cn.myperf4j.base.buffer.IntBuf;
import cn.myperf4j.base.util.ArrayUtils;
import cn.myperf4j.base.util.MapUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Created by LinShunkang on 2018/3/13
 */

/**
 * 默认使用该类作为 MyPerf4J 的 Recorder
 * <p>
 * 该类用于精确存储某一个方法在指定时间片内的响应时间
 * 为了减小内存占用，利用 数组+Map 的方式:
 * 1、将小于等于 mostTimeThreshold 的响应时间记录在数组中；
 * 2、将大于 mostTimeThreshold 的响应时间记录到 Map 中。
 */
public class AccurateRecorder extends Recorder {

    private final AtomicIntegerArray zeroTimingArr;//存储响应时间低于 1ms 的记录

    private final AtomicIntegerArray timingArr;//存储响应时间大于等于 1ms 并且小于 mostTimeThreshold 的记录

    private final ConcurrentHashMap<Integer, AtomicInteger> timingMap;//存储响应时间大于等于 mostTimeThreshold 的记录

    private final AtomicInteger diffCount;

    private AccurateRecorder(int methodTagId, int mostTimeThreshold, int outThresholdCount) {
        super(methodTagId);
        this.zeroTimingArr = new AtomicIntegerArray(10);
        this.timingArr = new AtomicIntegerArray(mostTimeThreshold + 1);
        this.timingMap = new ConcurrentHashMap<>(MapUtils.getFitCapacity(outThresholdCount));
        this.diffCount = new AtomicInteger(0);
    }

    @Override
    public void recordTime(long startNanoTime, long endNanoTime) {
        if (startNanoTime > endNanoTime) {
            return;
        }

        int elapsedMicros = (int) ((endNanoTime - startNanoTime) / 1000);
        if (elapsedMicros < 1000) {// < 1ms
            if (zeroTimingArr.getAndIncrement(elapsedMicros / 100) <= 0) {
                diffCount.incrementAndGet();
            }
            return;
        }

        int elapsedMills = elapsedMicros / 1000;
        if (elapsedMills < timingArr.length()) {
            if (timingArr.getAndIncrement(elapsedMills) <= 0) {
                diffCount.incrementAndGet();
            }
            return;
        }

        AtomicInteger count = timingMap.get(elapsedMills);
        if (count != null) {
            count.incrementAndGet();
            return;
        }

        AtomicInteger oldCounter = timingMap.putIfAbsent(elapsedMills, new AtomicInteger(1));
        if (oldCounter != null) {
            oldCounter.incrementAndGet();
        } else {
            diffCount.incrementAndGet();
        }
    }

    @Override
    public int fillSortedRecords(IntBuf intBuf) {
        int totalCount = 0;
        AtomicIntegerArray zeroTimingArr = this.zeroTimingArr;
        for (int i = 0; i < zeroTimingArr.length(); ++i) {
            int count = zeroTimingArr.get(i);
            if (count > 0) {
                intBuf.write(i, count);
                totalCount += count;
            }
        }

        AtomicIntegerArray timingArr = this.timingArr;
        for (int i = 1; i < timingArr.length(); ++i) {
            int count = timingArr.get(i);
            if (count > 0) {
                intBuf.write(i * 10, count);
                totalCount += count;
            }
        }
        return totalCount + fillMapRecord(intBuf);
    }

    private int fillMapRecord(IntBuf intBuf) {
        int totalCount = 0;
        int offset = intBuf.writerIndex();
        ConcurrentHashMap<Integer, AtomicInteger> timingMap = this.timingMap;
        for (Map.Entry<Integer, AtomicInteger> entry : timingMap.entrySet()) {
            int count = entry.getValue().get();
            if (count > 0) {
                intBuf.write(entry.getKey() * 10);
                totalCount += count;
            }
        }

        if (offset == intBuf.writerIndex()) {
            return 0;
        }

        int writerIndex = intBuf.writerIndex();
        Arrays.sort(intBuf._buf(), offset, writerIndex);

        for (int i = writerIndex - 1; i >= offset; --i) {
            int count = intBuf.getInt(i);
            int keyIdx = (i << 1) - offset;//2 * (i - offset) + offset
            intBuf.setInt(keyIdx, count);
            intBuf.setInt(keyIdx + 1, timingMap.get(count / 10).get());
        }
        intBuf.writerIndex((writerIndex << 1) - offset);//writerIndex + (writerIndex - offset)
        return totalCount;
    }

    @Override
    public int getDiffCount() {
        return diffCount.get();
    }

    @Override
    public synchronized void resetRecord() {
        ArrayUtils.reset(zeroTimingArr);
        ArrayUtils.reset(timingArr);

        Iterator<Map.Entry<Integer, AtomicInteger>> iterator = timingMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, AtomicInteger> entry = iterator.next();
            if ((entry.getKey() > 2 * timingArr.length())
                    || entry.getValue().get() <= 0) {
                iterator.remove();
            } else {
                entry.getValue().set(0);
            }
        }

        diffCount.set(0);
    }

    @Override
    public boolean hasRecord() {
        return diffCount.get() > 0;
    }

    public static AccurateRecorder getInstance(int methodTagId, int mostTimeThreshold, int outThresholdCount) {
        return new AccurateRecorder(methodTagId, mostTimeThreshold, outThresholdCount);
    }
}

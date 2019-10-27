package MyPerf4J;

import cn.myperf4j.base.MethodTag;
import cn.myperf4j.base.metric.MethodMetrics;
import cn.myperf4j.core.*;
import cn.myperf4j.core.recorder.AccurateRecorder;
import cn.myperf4j.core.recorder.Recorder;
import cn.myperf4j.core.recorder.Recorders;
import cn.myperf4j.core.recorder.RoughRecorder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Created by LinShunkang on 2018/10/17
 */
public class MethodMetricsTest {

    private Recorders recorders = new Recorders(new AtomicReferenceArray<Recorder>(10));

    private MethodTagMaintainer methodTagMaintainer = MethodTagMaintainer.getInstance();

    private Recorder accurateRecorder;

    private Recorder roughRecorder;

    private Recorder emptyAccurateRecorder;

    private Recorder emptyRoughRecorder;


    @Before
    public void init() {
        int methodId1 = methodTagMaintainer.addMethodTag(MethodTag.getGeneralInstance("", "Test", "Api", "m1", ""));
        accurateRecorder = AccurateRecorder.getInstance(methodId1, 9000, 10);
        recorders.setRecorder(methodId1, accurateRecorder);

        int methodId2 = methodTagMaintainer.addMethodTag(MethodTag.getGeneralInstance("", "Test", "Api", "m2", ""));
        roughRecorder = RoughRecorder.getInstance(methodId2, 9999);
        recorders.setRecorder(methodId2, roughRecorder);

        //
        int methodId3 = methodTagMaintainer.addMethodTag(MethodTag.getGeneralInstance("", "Test", "Api", "m3", ""));
        emptyAccurateRecorder = RoughRecorder.getInstance(methodId3, 10000);
        recorders.setRecorder(methodId3, emptyAccurateRecorder);

        int methodId4 = methodTagMaintainer.addMethodTag(MethodTag.getGeneralInstance("", "Test", "Api", "m4", ""));
        emptyRoughRecorder = RoughRecorder.getInstance(methodId4, 10000);
        recorders.setRecorder(methodId4, emptyRoughRecorder);

        //
        recordTimes(accurateRecorder);
        recordTimes(roughRecorder);
    }

    private void recordTimes(Recorder recorder) {
        recorders.setStartTime(System.currentTimeMillis());
        long start = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            recorder.recordTime(start, start + i * 1000 * 100);
        }

        for (long i = 1; i < 10000; ++i) {
            for (int j = 0; j < 10; j++) {
                recorder.recordTime(start, start + i * 1000 * 1000);
            }
        }
        recorders.setStopTime(System.currentTimeMillis());
    }

    @Test
    public void test() {
        testNormalRecord(accurateRecorder);
        testNormalRecord(roughRecorder);

        testEmptyRecord(emptyAccurateRecorder);
        testEmptyRecord(emptyRoughRecorder);
    }

    private void testNormalRecord(Recorder recorder) {
        MethodTag methodTag = methodTagMaintainer.getMethodTag(recorder.getMethodTagId());
        MethodMetrics methodMetrics = MethodMetricsCalculator.calPerfStats(recorder, methodTag, recorders.getStartTime(), recorders.getStopTime());
        System.out.println(methodMetrics);
        recorder.resetRecord();

        Assert.assertEquals(0, methodMetrics.getMinTime());
        Assert.assertEquals("4999.500045", String.valueOf(methodMetrics.getAvgTime()));
        Assert.assertEquals(4999, methodMetrics.getTP50());
        Assert.assertEquals(8999, methodMetrics.getTP90());
        Assert.assertEquals(9499, methodMetrics.getTP95());
        Assert.assertEquals(9899, methodMetrics.getTP99());
        Assert.assertEquals(9989, methodMetrics.getTP999());
        Assert.assertEquals(9998, methodMetrics.getTP9999());
        Assert.assertEquals(9999, methodMetrics.getTP100());
        Assert.assertEquals(methodMetrics.getMaxTime(), methodMetrics.getTP100());
        Assert.assertEquals("2886.751253584815", String.valueOf(methodMetrics.getStdDev()));
    }

    private void testEmptyRecord(Recorder recorder) {
        recorders.setStartTime(System.currentTimeMillis());
        recorders.setStopTime(System.currentTimeMillis() + 1000);

        MethodTag methodTag = methodTagMaintainer.getMethodTag(recorder.getMethodTagId());
        MethodMetrics methodMetrics = MethodMetricsCalculator.calPerfStats(recorder, methodTag, recorders.getStartTime(), recorders.getStopTime());
        System.out.println(methodMetrics);
        recorder.resetRecord();

        Assert.assertEquals(-1, methodMetrics.getMinTime());
        Assert.assertEquals("-1.0", String.valueOf(methodMetrics.getAvgTime()));
        Assert.assertEquals(-1, methodMetrics.getTP50());
        Assert.assertEquals(-1, methodMetrics.getTP90());
        Assert.assertEquals(-1, methodMetrics.getTP95());
        Assert.assertEquals(-1, methodMetrics.getTP99());
        Assert.assertEquals(-1, methodMetrics.getTP999());
        Assert.assertEquals(-1, methodMetrics.getTP9999());
        Assert.assertEquals(-1, methodMetrics.getTP100());
        Assert.assertEquals(-1, methodMetrics.getTP100(), methodMetrics.getMaxTime());
        Assert.assertEquals("0.0", String.valueOf(methodMetrics.getStdDev()));
    }
}

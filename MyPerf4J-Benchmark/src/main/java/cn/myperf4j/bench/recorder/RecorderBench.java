package cn.myperf4j.bench.recorder;

import cn.myperf4j.core.recorder.AccurateRecorder;
import cn.myperf4j.core.recorder.Recorder;
import cn.myperf4j.core.recorder.RoughRecorder;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Created by LinShunkang on 2019/10/19
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class RecorderBench {

    private Recorder roughRecorder;

    private Recorder accurateRecorder;

    private long startNanos;

    private long endNanos;

    @Setup
    public void setup() {
        roughRecorder = RoughRecorder.getInstance(0, 1024);
        accurateRecorder = AccurateRecorder.getInstance(1, 1024, 64);
        startNanos = System.nanoTime();
        endNanos = System.nanoTime();
    }

    @Benchmark
    public void roughRecorderBench() {
        roughRecorder.recordTime(startNanos, endNanos);
    }

    @Benchmark
    public void accurateRecorderBench() {
        accurateRecorder.recordTime(startNanos, endNanos);
    }

    public static void main(String[] args) throws RunnerException {
        // 使用一个单独进程执行测试，执行3遍warmup，然后执行5遍测试
        Options opt = new OptionsBuilder()
                .include(RecorderBench.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .build();
        new Runner(opt).run();
    }
}

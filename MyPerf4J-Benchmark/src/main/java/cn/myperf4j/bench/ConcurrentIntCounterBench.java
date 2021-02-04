package cn.myperf4j.bench;

import cn.myperf4j.base.util.concurrent.AtomicIntArray;
import cn.myperf4j.base.util.concurrent.ConcurrentIntCounter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by LinShunkang on 2021/02/01
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class ConcurrentIntCounterBench {

    private ConcurrentIntCounter intMap;

    private AtomicIntArray intArray;

    private ConcurrentMap<Integer, AtomicInteger> integerMap;

    private final int[] keys = {0, 8, 16, 24, 32, 40, 48, 56};

    @Setup
    public void setup() {
        intMap = new ConcurrentIntCounter(128);
        intArray = new AtomicIntArray(128);
        integerMap = new ConcurrentHashMap<>(128);
    }

    @Benchmark
    public int intMapSingleBench() {
        return intMap.incrementAndGet(1, 1);
    }

    @Benchmark
    public int intArraySingleBench() {
        return intArray.incrementAndGet(1);
    }

    @Benchmark
    public int integerMapSingleBench() {
        return increase(integerMap, 1);
    }

//    @Benchmark
    public int intMapMultiBench() {
        int tmp = 0;
        for (int i = 0; i < keys.length; i++) {
            tmp += intMap.incrementAndGet(keys[i], 1);
        }
        return tmp;
    }

//    @Benchmark
    public int intArrayMultiBench() {
        int tmp = 0;
        for (int i = 0; i < keys.length; i++) {
            tmp += intArray.incrementAndGet(keys[i]);
        }
        return tmp;
    }

//    @Benchmark
    public int integerMapMultiBench() {
        int tmp = 0;
        for (int i = 0; i < keys.length; i++) {
            tmp += increase(integerMap, keys[i]);
        }
        return tmp;
    }

    private int increase(ConcurrentMap<Integer, AtomicInteger> integerHashMap, int k) {
        final AtomicInteger count = integerHashMap.get(k);
        if (count != null) {
            return count.incrementAndGet();
        }

        final AtomicInteger oldCounter = integerHashMap.putIfAbsent(k, new AtomicInteger(1));
        if (oldCounter != null) {
            return oldCounter.incrementAndGet();
        }
        return 0;
    }

    public static void main(String[] args) throws RunnerException {
        // 使用一个单独进程执行测试，执行3遍warmup，然后执行5遍测试
        Options opt = new OptionsBuilder()
                .include(ConcurrentIntCounterBench.class.getSimpleName())
                .forks(2)
                .threads(6)
                .warmupIterations(3)
                .measurementIterations(5)
                .build();
        new Runner(opt).run();
    }
}

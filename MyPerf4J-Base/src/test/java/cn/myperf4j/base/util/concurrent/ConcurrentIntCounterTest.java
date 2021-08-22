package cn.myperf4j.base.util.concurrent;

import cn.myperf4j.base.util.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created by LinShunkang on 2021/01/31
 */
public class ConcurrentIntCounterTest {

    @Test
    public void testIncrease() {
        final ConcurrentIntCounter intMap = new ConcurrentIntCounter(1);
        Assert.assertEquals(1, intMap.incrementAndGet(1));
        Assert.assertEquals(1, intMap.get(1));
        Assert.assertEquals(3, intMap.addAndGet(1, 2));
        Assert.assertEquals(3, intMap.get(1));

        intMap.reset();

        final int testTimes = 10240;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < testTimes; j++) {
                intMap.addAndGet(j, j + 1);
            }
        }

        Assert.assertEquals(testTimes, intMap.size());

        for (int i = 0; i < testTimes; i++) {
            Assert.assertEquals((i + 1) * 2, intMap.get(i));
        }
    }

    @Test
    public void testDecrease() {
        final ConcurrentIntCounter intMap = new ConcurrentIntCounter(1);
        Assert.assertEquals(-1, intMap.addAndGet(0, -1));
        Assert.assertEquals(-1, intMap.get(0));

        Assert.assertEquals(-3, intMap.addAndGet(0, -2));
        Assert.assertEquals(-3, intMap.get(0));

        Assert.assertEquals(1, intMap.addAndGet(0, 4));
        Assert.assertEquals(1, intMap.get(0));
    }

    @Test
    public void testSize() {
        final ConcurrentIntCounter intMap = new ConcurrentIntCounter(1);
        Assert.assertEquals(intMap.size(), 0);

        intMap.addAndGet(1, 2);
        Assert.assertEquals(intMap.size(), 1);

        intMap.addAndGet(2, 2);
        Assert.assertEquals(intMap.size(), 2);

        for (int i = 1; i < 5; i++) {
            intMap.addAndGet(i, i);
        }
        Assert.assertEquals(4, intMap.size());
    }

    @Test
    public void testReset() {
        final ConcurrentIntCounter intMap = new ConcurrentIntCounter(1);
        Assert.assertEquals(intMap.size(), 0);

        for (int i = 1; i < 10240; i++) {
            Assert.assertEquals(i, intMap.addAndGet(i, i));
        }
        Assert.assertEquals(10239, intMap.size());

        intMap.reset();
        Assert.assertEquals(intMap.size(), 0);

        for (int i = 1; i < 10240; i++) {
            Assert.assertEquals(0, intMap.get(i));
        }
    }

    @Test
    public void testConflict() {
        final ConcurrentIntCounter intMap = new ConcurrentIntCounter(8);
        for (int i = 0; i < 2; i++) {
            intMap.incrementAndGet(0);
            intMap.incrementAndGet(8);
            intMap.incrementAndGet(16);
        }

        for (int i = 0; i < 2; i++) {
            intMap.get(0);
            intMap.get(8);
            intMap.get(16);
        }
    }

    @Test
    public void testSingleThread() {
        final ConcurrentIntCounter intMap = new ConcurrentIntCounter(1);
        final AtomicIntegerArray intArray = new AtomicIntegerArray(128 * 1024);
        final ConcurrentMap<Integer, AtomicInteger> integerMap = new ConcurrentHashMap<>(128 * 1024);
        mode1(intMap, intArray, integerMap, 1024, 64);
        mode1(intMap, intArray, integerMap, 256, 256);
        mode1(intMap, intArray, integerMap, 64, 1024);
        mode1(intMap, intArray, integerMap, 16, 4 * 1024);
        mode1(intMap, intArray, integerMap, 4, 16 * 1024);
        mode1(intMap, intArray, integerMap, 1, 64 * 1024);

        for (Map.Entry<Integer, AtomicInteger> entry : integerMap.entrySet()) {
            final Integer key = entry.getKey();
            final AtomicInteger value = entry.getValue();
            Assert.assertEquals("intArray", value.intValue(), intArray.get(key));
            Assert.assertEquals("intMap", value.intValue(), intMap.get(key));
        }
    }

    private void mode1(ConcurrentIntCounter intMap,
                       AtomicIntegerArray intArray,
                       ConcurrentMap<Integer, AtomicInteger> integerMap,
                       int x, int y) {
        final long start = System.nanoTime();
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                intMap.incrementAndGet(j);
                intArray.incrementAndGet(j);
                increase(integerMap, j, 1);
            }
        }
        Logger.info("x=" + x + ", y=" + y + ", cost=" + (System.nanoTime() - start) / 1000_000 + "ms");
    }

    @Test
    public void testMultiThread() throws InterruptedException, BrokenBarrierException {
        final int testTimes = 1024;
        for (int i = 0; i < testTimes; i++) {
            Logger.info("\n--------------------- Round " + i + " ---------------------\n");
            testMultiThread0();
        }
    }

    private static void testMultiThread0() throws InterruptedException, BrokenBarrierException {
        final int testTimes = 1024;
        final ConcurrentIntCounter intMap = new ConcurrentIntCounter(1);
        final AtomicIntArray intArray = new AtomicIntArray(testTimes);
        final ConcurrentMap<Integer, AtomicInteger> integerMap = new ConcurrentHashMap<>(testTimes);
        final int threadCnt = 12;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCnt);
        final CyclicBarrier barrier = new CyclicBarrier(threadCnt + 1);
        for (int i = 0; i < threadCnt; i++) {
            final int delta = i + 1;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        barrier.await();
                    } catch (InterruptedException | BrokenBarrierException e) {
                        e.printStackTrace();
                    }
                    Logger.info("starting...");

                    try {
                        for (int k = 0; k < testTimes; k++) {
                            intMap.addAndGet(k, delta);
                            intArray.addAndGet(k, delta);
                            increase(integerMap, k, delta);
                        }
                    } finally {
                        try {
                            Logger.info("stopping...");
                            barrier.await();
                        } catch (InterruptedException | BrokenBarrierException e) {
                            e.printStackTrace();
                        } finally {
                            Logger.info("stopped.");
                        }
                    }
                }
            });
        }
        barrier.await();
        long start = System.nanoTime();
        Logger.info("M starting...");
        barrier.await();
        Logger.info("Cost " + (System.nanoTime() - start) / 1000_000 + "ms");

        executor.shutdown();
        final boolean termination = executor.awaitTermination(1, SECONDS);
        Logger.info("termination=" + termination);

        Assert.assertEquals(integerMap.toString(), testTimes, integerMap.size());
        Assert.assertEquals(intMap.toString(), testTimes, intMap.size());

        for (Map.Entry<Integer, AtomicInteger> entry : integerMap.entrySet()) {
            final Integer key = entry.getKey();
            final int expectedVal = entry.getValue().intValue();
            Assert.assertEquals("intArray " + key + ", " + intArray, expectedVal, intArray.get(key));
            Assert.assertEquals("intMap " + key + ", " + intMap, expectedVal, intMap.get(key));
        }
        Logger.info("Congratulation!");
        SECONDS.sleep(1);
    }

    private static void increase(ConcurrentMap<Integer, AtomicInteger> integerHashMap, int k, int delta) {
        final AtomicInteger count = integerHashMap.get(k);
        if (count != null) {
            count.addAndGet(delta);
            return;
        }

        final AtomicInteger oldCounter = integerHashMap.putIfAbsent(k, new AtomicInteger(delta));
        if (oldCounter != null) {
            oldCounter.addAndGet(delta);
        }
    }
}

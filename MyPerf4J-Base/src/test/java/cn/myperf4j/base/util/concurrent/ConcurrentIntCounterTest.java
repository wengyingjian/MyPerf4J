package cn.myperf4j.base.util.concurrent;

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

/**
 * Created by LinShunkang on 2021/01/31
 */
public class ConcurrentIntCounterTest {

    @Test
    public void testIncrease() {
        final ConcurrentIntCounter intMap = new ConcurrentIntCounter(1);
        Assert.assertEquals(0, intMap.incrementAndGet(1, 1));
        Assert.assertEquals(1, intMap.get(1));
        Assert.assertEquals(3, intMap.incrementAndGet(1, 2));
        Assert.assertEquals(3, intMap.get(1));

        intMap.reset();

        for (int i = 1; i < 10240; i++) {
            intMap.incrementAndGet(i, i);
        }

        for (int i = 1; i < 10240; i++) {
            Assert.assertEquals(i, intMap.get(i));
        }
    }

    @Test
    public void testSize() {
        final ConcurrentIntCounter intMap = new ConcurrentIntCounter(1);
        Assert.assertEquals(intMap.size(), 0);

        intMap.incrementAndGet(1, 2);
        Assert.assertEquals(intMap.size(), 1);

        intMap.incrementAndGet(2, 2);
        Assert.assertEquals(intMap.size(), 2);

        for (int i = 1; i < 5; i++) {
            intMap.incrementAndGet(i, i);
        }
        Assert.assertEquals(4, intMap.size());
    }

    @Test
    public void testReset() {
        final ConcurrentIntCounter intMap = new ConcurrentIntCounter(1);
        Assert.assertEquals(intMap.size(), 0);

        for (int i = 1; i < 10240; i++) {
            Assert.assertEquals(0, intMap.incrementAndGet(i, i));
        }
        Assert.assertEquals(10239, intMap.size());

        intMap.reset();
        Assert.assertEquals(intMap.size(), 0);

        for (int i = 1; i < 10240; i++) {
            Assert.assertEquals(0, intMap.get(i));
        }
    }

    @Test
    public void testSingleThread() {
        final ConcurrentIntCounter intMap = new ConcurrentIntCounter(128 * 1024);
        final AtomicIntegerArray intArray = new AtomicIntegerArray(128 * 1024);
        final ConcurrentMap<Integer, AtomicInteger> integerMap = new ConcurrentHashMap<>(128 * 1024);
        mode1(intMap, intArray, integerMap, 1024, 64);
        mode1(intMap, intArray, integerMap, 256, 256);
        mode1(intMap, intArray, integerMap, 64, 1024);
        mode1(intMap, intArray, integerMap, 16, 4 * 1024);
        mode1(intMap, intArray, integerMap, 4, 16 * 1024);
        mode1(intMap, intArray, integerMap, 1, 64 * 1024);
    }

    private void mode1(ConcurrentIntCounter intMap,
                       AtomicIntegerArray intArray,
                       ConcurrentMap<Integer, AtomicInteger> integerMap,
                       int x, int y) {
        final long start = System.nanoTime();
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                intMap.incrementAndGet(j, 1);
                intArray.incrementAndGet(j);
                increase(integerMap, j);
            }
        }
        System.out.println("x=" + x + ", y=" + y + ", cost=" + (System.nanoTime() - start) / 1000_000 + "ms");
    }

    @Test
    public void testMultiThread() throws InterruptedException, BrokenBarrierException {
        final ConcurrentIntCounter intMap = new ConcurrentIntCounter(128 * 1024);
        final AtomicIntArray intArray = new AtomicIntArray(128 * 1024);
        final ConcurrentMap<Integer, AtomicInteger> integerMap = new ConcurrentHashMap<>(128 * 1024);
        final int threadCnt = 4;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCnt);
        final CyclicBarrier barrier = new CyclicBarrier(threadCnt + 1);
        for (int i = 0; i < threadCnt; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        barrier.await();
                    } catch (InterruptedException | BrokenBarrierException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Thread: " + Thread.currentThread().getId() + " starting...");

                    try {
                        for (int k = 0; k < 64 * 1024; k++) {
                            intMap.incrementAndGet(k, 1);
                            intArray.incrementAndGet(k);
                            increase(integerMap, k);
                        }
                    } finally {
                        try {
                            barrier.await();
                        } catch (InterruptedException | BrokenBarrierException e) {
                            e.printStackTrace();
                        } finally {
                            System.out.println("Thread: " + Thread.currentThread().getId() + " stopped.");
                        }
                    }
                }
            });
        }
        barrier.await();
        long start = System.nanoTime();
        System.out.println("Thread: M" + Thread.currentThread().getId() + " starting...");
        barrier.await();
        System.out.println("Cost " + (System.nanoTime() - start) / 1000_000 + "ms");
        executor.shutdownNow();

        for (Map.Entry<Integer, AtomicInteger> entry : integerMap.entrySet()) {
            final Integer key = entry.getKey();
            final AtomicInteger value = entry.getValue();
            Assert.assertEquals(threadCnt, value.intValue());
            Assert.assertEquals(threadCnt, intMap.get(key));
            Assert.assertEquals(threadCnt, intArray.get(key));
        }
    }

    private static void increase(ConcurrentMap<Integer, AtomicInteger> integerHashMap, int k) {
        final AtomicInteger count = integerHashMap.get(k);
        if (count != null) {
            count.incrementAndGet();
            return;
        }

        final AtomicInteger oldCounter = integerHashMap.putIfAbsent(k, new AtomicInteger(1));
        if (oldCounter != null) {
            oldCounter.incrementAndGet();
        }
    }
}

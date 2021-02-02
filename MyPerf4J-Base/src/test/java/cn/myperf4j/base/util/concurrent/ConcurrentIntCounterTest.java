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

/**
 * Created by LinShunkang on 2021/01/31
 */
public class ConcurrentIntCounterTest {

    @Test
    public void testIncrease() {
        final ConcurrentIntCounter intMap = new ConcurrentIntCounter(1);
        Assert.assertEquals(0, intMap.increase(1, 1));
        Assert.assertEquals(1, intMap.get(1));
        Assert.assertEquals(1, intMap.increase(1, 2));
        Assert.assertEquals(3, intMap.get(1));

        intMap.reset();

        for (int i = 1; i < 10240; i++) {
            intMap.increase(i, i);
        }

        for (int i = 1; i < 10240; i++) {
            Assert.assertEquals(i, intMap.get(i));
        }
    }

    @Test
    public void testSize() {
        final ConcurrentIntCounter intMap = new ConcurrentIntCounter(1);
        Assert.assertEquals(intMap.size(), 0);

        intMap.increase(1, 2);
        Assert.assertEquals(intMap.size(), 1);

        intMap.increase(2, 2);
        Assert.assertEquals(intMap.size(), 2);

        for (int i = 1; i < 5; i++) {
            intMap.increase(i, i);
        }
        Assert.assertEquals(4, intMap.size());
    }

    @Test
    public void testReset() {
        final ConcurrentIntCounter intMap = new ConcurrentIntCounter(1);
        Assert.assertEquals(intMap.size(), 0);

        for (int i = 1; i < 10240; i++) {
            Assert.assertEquals(0, intMap.increase(i, i));
        }
        Assert.assertEquals(10239, intMap.size());

        intMap.reset();
        Assert.assertEquals(intMap.size(), 0);

        for (int i = 1; i < 10240; i++) {
            Assert.assertEquals(0, intMap.get(i));
        }
    }

    @Test
    public void test() throws InterruptedException, BrokenBarrierException {
        final ConcurrentIntCounter intMap = new ConcurrentIntCounter(128);
        final AtomicIntArray intArray = new AtomicIntArray(128 * 1024 * 1024);
        final ConcurrentMap<Integer, AtomicInteger> integerHashMap = new ConcurrentHashMap<>(128);
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
                            intMap.increase(k, 1);
                            intArray.incrementAndGet(k);
                            increase(integerHashMap, k);
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
        System.out.println("Thread: M" + Thread.currentThread().getId() + " starting...");

        barrier.await();

        for (Map.Entry<Integer, AtomicInteger> entry : integerHashMap.entrySet()) {
            Assert.assertEquals(entry.getValue().intValue(), intMap.get(entry.getKey()));
//            Assert.assertEquals(threadCnt, entry.getValue().intValue());
        }
    }

    private void increase(ConcurrentMap<Integer, AtomicInteger> integerHashMap, int k) {
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

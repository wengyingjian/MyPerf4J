package cn.myperf4j.base.util.concurrent;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by LinShunkang on 2021/01/31
 */
public class ConcurrentIntMapTest {

    @Test
    public void testPutAndGet() {
        final ConcurrentIntMap intMap = new ConcurrentIntMap(1);
        intMap.put(1, 1);
        Assert.assertEquals(1, intMap.get(1));
        intMap.put(1, 2);
        Assert.assertEquals(2, intMap.get(1));

        for (int i = 1; i < 10240; i++) {
            intMap.put(i, i);
        }

        for (int i = 1; i < 10240; i++) {
            Assert.assertEquals(i, intMap.get(i));
        }
    }

    @Test
    public void testSize() {
        final ConcurrentIntMap intMap = new ConcurrentIntMap(1);
        Assert.assertEquals(intMap.size(), 0);

        intMap.put(1, 1);
        Assert.assertEquals(intMap.size(), 1);

        intMap.put(2, 2);
        Assert.assertEquals(intMap.size(), 2);

        for (int i = 1; i < 16; i++) {
            intMap.put(i, i);
        }
        Assert.assertEquals(15, intMap.size());
    }

    @Test
    public void testReset() {
        final ConcurrentIntMap intMap = new ConcurrentIntMap(1);
        Assert.assertEquals(intMap.size(), 0);

        for (int i = 1; i < 10240; i++) {
            intMap.put(i, i);
        }
        Assert.assertEquals(10239, intMap.size());

        intMap.reset();
        Assert.assertEquals(intMap.size(), 0);

        for (int i = 1; i < 10240; i++) {
            Assert.assertEquals(0, intMap.get(i));
        }
    }
}

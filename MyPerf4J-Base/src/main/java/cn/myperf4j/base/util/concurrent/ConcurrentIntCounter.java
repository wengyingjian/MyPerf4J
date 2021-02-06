package cn.myperf4j.base.util.concurrent;

import cn.myperf4j.base.util.UnsafeUtils;
import sun.misc.Unsafe;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static java.lang.Integer.MAX_VALUE;

/**
 * Created by LinShunkang on 2021/01/30
 */
public final class ConcurrentIntCounter implements Serializable {

    private static final long serialVersionUID = -23407935555046178L;

    private static final Unsafe unsafe = UnsafeUtils.getUnsafe();
    private static final int base = Unsafe.ARRAY_INT_BASE_OFFSET;
    private static final int scale = Unsafe.ARRAY_INT_INDEX_SCALE;
    private static final int shift = 31 - Integer.numberOfLeadingZeros(scale);

    private static final float DEFAULT_LOAD_FACTOR = 0.5F;

    private static final int MAX_CAPACITY = MAX_VALUE >> 1;

    private static final AtomicIntegerFieldUpdater<ConcurrentIntCounter> SIZE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(ConcurrentIntCounter.class, "size");

    private volatile int[] array;

    private volatile int size;

    private volatile int maxSize;

    static {
        if ((scale & (scale - 1)) != 0) {
            throw new Error("data type scale not a power of two");
        }
    }

    private static long byteOffset(int idx, int maxIdx) {
        if (idx < 0 || idx > maxIdx) {
            throw new IndexOutOfBoundsException("index " + idx);
        }
        return ((long) idx << shift) + base;
    }

    private static int tableSizeFor(int capacity) {
        int number = 1;
        while (number < capacity) {
            number = number << 1;
        }
        return number <= 8 ? 16 : number;
    }

    private static int hashIdx(int key, int mask) {
        return ((key & mask) << 1) & mask;
    }

    private static int probeNext(int idx, int mask) {
        return (idx + 2) & mask;
    }

    private static int getIntRaw(int[] array, long offset) {
        return unsafe.getIntVolatile(array, offset);
    }

    public ConcurrentIntCounter(int initialCapacity) {
        if (initialCapacity >= MAX_CAPACITY) {
            throw new IllegalArgumentException("Max initialCapacity need low than " + MAX_CAPACITY);
        }

        final int tableSize = tableSizeFor(initialCapacity << 1);
        this.array = new int[tableSize];
        this.size = 0;
        this.maxSize = calcMaxSize(tableSize >> 1);
    }

    private int calcMaxSize(int keySize) {
        final int upperBound = keySize - 1;
        return Math.min(upperBound, (int) (keySize * DEFAULT_LOAD_FACTOR));
    }

    public int get(final int key) {
        final int[] array = this.array;
        final int mask = array.length - 1;
        final int keyIdx = findKeyIdx(array, mask, key);
        return keyIdx >= 0 ? getIntRaw(array, byteOffset(keyIdx + 1, mask)) : 0;
    }

    private int findKeyIdx(final int[] array, final int mask, final int key) {
        final int startIdx = hashIdx(key, mask);
        int idx = startIdx;
        while (true) {
            if (getIntRaw(array, byteOffset(idx + 1, mask)) == 0) {
                return -1;
            }

            if (key == getIntRaw(array, byteOffset(idx, mask))) {
                return idx;
            }

            // Conflict, keep probing ...
            if ((idx = probeNext(idx, mask)) == startIdx) {
                return -1;
            }
        }
    }

    public int incrementAndGet(final int key) {
        return addAndGet(key, 1);
    }

    public int addAndGet(final int key, final int delta) {
        final int[] array = this.array;
        final int mask = array.length - 1;
        final int startIdx = hashIdx(key, mask);
        int idx = startIdx;

        int k;
        long kOffset;
        while (true) {
            kOffset = byteOffset(idx, mask);
            k = getIntRaw(array, kOffset);
            if (k == key) { //increase
                return addAndGet(array, byteOffset(idx + 1, mask), delta);
            } else if (k == 0) { //try set
                final long kvLong = ((long) delta) << 32 | key;
                if (unsafe.compareAndSwapLong(array, kOffset, 0L, kvLong)) {
                    growSize();
                    return 0;
                } else if (key == getIntRaw(array, kOffset)) {
                    return addAndGet(array, byteOffset(idx + 1, mask), delta);
                }
            }

            if ((idx = probeNext(idx, mask)) == startIdx) {
                throw new IllegalStateException("Unable to insert");
            }
        }
    }

    private int addAndGet(int[] array, long byteOffset, int delta) {
        while (true) {
            final int current = getIntRaw(array, byteOffset);
            final int next = current + delta;
            if (unsafe.compareAndSwapInt(array, byteOffset, current, next)) {
                return next;
            }
        }
    }

    private void growSize() {
        if (SIZE_UPDATER.incrementAndGet(this) < maxSize) {
            return;
        }

        final int[] oldArray = this.array;
        if (oldArray.length >= MAX_CAPACITY) {
            throw new IllegalStateException("Max capacity reached at size=" + size);
        }

        boolean needRehash;
        synchronized (this) {
            if (needRehash = size >= maxSize) {
                final int newCapacity = oldArray.length << 1;
                this.array = new int[newCapacity];
                this.size = 0;
                this.maxSize = calcMaxSize(newCapacity >> 1);
            }
        }

        if (needRehash) {
            transfer(oldArray);
        }
    }

    private void transfer(final int[] oldArray) {
        final int mask = oldArray.length - 1;
        for (int i = 0; i < oldArray.length; i += 2) {
            final int value = getIntRaw(oldArray, byteOffset(i + 1, mask));
            if (value > 0) {
                final int key = getIntRaw(oldArray, byteOffset(i, mask));
                addAndGet(key, value);
            }
        }
    }

    public int size() {
        return size;
    }

    public void reset() {
        final int[] array = this.array;
        final int mask = array.length - 1;
        this.size = 0;
        unsafe.setMemory(array, byteOffset(0, mask), (long) array.length * scale, (byte) 0);
    }
}

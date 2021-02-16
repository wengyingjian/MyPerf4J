package cn.myperf4j.base.util.concurrent;

import cn.myperf4j.base.util.UnsafeUtils;
import sun.misc.Unsafe;

import java.io.Serializable;
import java.util.Arrays;
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

    private static long getLongRaw(int[] array, long offset) {
        return unsafe.getLongVolatile(array, offset);
    }

    private static long getKvLong(int key, int value) {
        return ((long) value) << 32 | key;
    }

    private static int getKey(long kvLong) {
        return (int) kvLong;
    }

    private static int getValue(long kvLong) {
        return (int) (kvLong >> 32);
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
        int[] array = this.array;
        int mask = array.length - 1;
        int startIdx = hashIdx(key, mask);
        int idx = startIdx;

        long kv, kOffset;
        while (true) {
            kOffset = byteOffset(idx, mask);
            if ((int) (kv = getLongRaw(array, kOffset)) == key) { //increase
                if (tryAddLong(array, kOffset, key, delta)) {
                    return getValue(kv) + delta;
                } else { //说明正在 rehash
                    array = this.array;
                    mask = array.length - 1;
                    idx = startIdx = hashIdx(key, mask);
                    continue;
                }
            } else if (kv == 0L) { //try set
                if (unsafe.compareAndSwapLong(array, kOffset, 0L, getKvLong(key, delta))) {
                    growSize();
                    return delta;
                } else if ((int) (kv = getLongRaw(array, kOffset)) == key) {
                    if (tryAddLong(array, kOffset, key, delta)) {
                        return getValue(kv) + delta;
                    } else { //说明正在 rehash
                        array = this.array;
                        mask = array.length - 1;
                        idx = startIdx = hashIdx(key, mask);
                        continue;
                    }
                }
            }

            if ((idx = probeNext(idx, mask)) == startIdx) {
                throw new IllegalStateException("Unable to insert");
            }
        }
    }

    private boolean tryAddLong(final int[] array, final long byteOffset, final int key, final int delta) {
        while (true) {
            final long current = getLongRaw(array, byteOffset);
            if (key != getKey(current)) {
//                System.err.println("tryAddLong(): key=" + key + ", but curKey=" + getKey(current));
                return false;
            }

            final long next = getKvLong(key, getValue(current) + delta);
            if (unsafe.compareAndSwapLong(array, byteOffset, current, next)) {
                return true;
            }
        }
    }

    private void growSize() {
        if (SIZE_UPDATER.incrementAndGet(this) < maxSize) {
            return;
        }

        if (array.length >= MAX_CAPACITY) {
            throw new IllegalStateException("Max capacity reached at size=" + size);
        }

        boolean needRehash;
        int[] oldArray = null;
        synchronized (this) {
            if (needRehash = size >= maxSize) {
                oldArray = this.array;
                final int newCapacity = array.length << 1;
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
//        System.err.println("Thread " + Thread.currentThread().getId() + " begin rehash...");
        for (int i = 0; i < 2; i++) {
            for (int k = 0; k < oldArray.length; k += 2) {
                final long offset = byteOffset(k, mask);
                final long kvLong = getLongRaw(oldArray, offset);
                if (kvLong != 0L && unsafe.compareAndSwapLong(oldArray, offset, kvLong, 0L)) {
                    addAndGet(getKey(kvLong), getValue(kvLong));
                }
            }
        }
//        System.err.println("Thread " + Thread.currentThread().getId() + " rehashed.");
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

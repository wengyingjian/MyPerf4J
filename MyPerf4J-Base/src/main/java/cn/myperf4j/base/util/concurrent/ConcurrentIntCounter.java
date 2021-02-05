package cn.myperf4j.base.util.concurrent;

import cn.myperf4j.base.util.UnsafeUtils;
import sun.misc.Unsafe;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

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

    private long checkedByteOffset(int i) {
        if (i < 0 || i >= array.length) {
            throw new IndexOutOfBoundsException("index " + i);
        }
        return ((long) i << shift) + base;
    }

    private static int tableSizeFor(int capacity) {
        int number = 1;
        while (number < capacity) {
            number = number << 1;
        }
        return number <= 8 ? 16 : number;
    }

    public ConcurrentIntCounter(int initialCapacity) {
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
        return keyIdx >= 0 ? unsafe.getIntVolatile(array, checkedByteOffset(keyIdx + 1)) : 0;
    }

    private int findKeyIdx(final int[] array, final int mask, final int key) {
        final int startIdx = hashIdx(key, mask);
        int idx = startIdx;
        while (true) {
            if (unsafe.getIntVolatile(array, checkedByteOffset(idx + 1)) == 0) {
                return -1;
            }

            if (key == unsafe.getIntVolatile(array, checkedByteOffset(idx))) {
                return idx;
            }

            // Conflict, keep probing ...
            if ((idx = probeNext(idx, mask)) == startIdx) {
                return -1;
            }
        }
    }

    private static int hashIdx(int key, int mask) {
        return ((key & mask) << 1) & mask;
    }

    private static int probeNext(int idx, int mask) {
        return (idx + 2) & mask;
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
            kOffset = checkedByteOffset(idx);
            k = unsafe.getIntVolatile(array, kOffset);
            if (k == key) { //increase
                return addAndGet(array, checkedByteOffset(idx + 1), delta);
            } else if (k == 0) { //try set
                final long kvLong = ((long) delta) << 32 | key;
                if (unsafe.compareAndSwapLong(array, kOffset, 0, kvLong)) {
                    growSize();
                    return 0;
                } else if (key == unsafe.getIntVolatile(array, kOffset)) {
                    return addAndGet(array, checkedByteOffset(idx + 1), delta);
                }
            }

            if ((idx = probeNext(idx, mask)) == startIdx) {
                throw new IllegalStateException("Unable to insert");
            }
        }
    }

    private int addAndGet(int[] array, long byteOffset, int delta) {
        while (true) {
            final int current = unsafe.getIntVolatile(array, byteOffset);
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
        if (oldArray.length == Integer.MAX_VALUE) {
            throw new IllegalStateException("Max capacity reached at size=" + size);
        }

        synchronized (this) {
            if (size >= maxSize) {
                final int newCapacity = oldArray.length << 1;
                this.array = new int[newCapacity];
                this.size = 0;
                this.maxSize = calcMaxSize(newCapacity >> 1);

                for (int i = 0; i < oldArray.length; i += 2) {
                    final int oldKey = oldArray[i];
                    final int oldVal = oldArray[i + 1];
                    if (oldKey >= 0 && oldVal != 0) {
                        addAndGet(oldKey, oldVal);
                    }
                }
            }
        }
    }

    public int size() {
        return size;
    }

    public void reset() {
        final int[] array = this.array;
        this.size = 0;
        unsafe.setMemory(array, checkedByteOffset(0), (long) array.length * scale, (byte) 0);
    }
}

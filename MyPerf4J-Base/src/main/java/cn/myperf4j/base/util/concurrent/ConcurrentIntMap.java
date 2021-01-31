package cn.myperf4j.base.util.concurrent;

import cn.myperf4j.base.util.UnsafeUtils;
import sun.misc.Unsafe;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Created by LinShunkang on 2021/01/30
 */
public final class ConcurrentIntMap implements Serializable {

    private static final long serialVersionUID = -23407935555046178L;

    private static final Unsafe unsafe = UnsafeUtils.getUnsafe();
    private static final int base = Unsafe.ARRAY_INT_BASE_OFFSET;
    private static final int scale = Unsafe.ARRAY_INT_INDEX_SCALE;
    private static final int shift = 31 - Integer.numberOfLeadingZeros(scale);

    private static final int HASH_BITS = 0x7FFFFFFF; // usable bits of normal node hash

    private static final AtomicIntegerFieldUpdater<ConcurrentIntMap> SIZE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(ConcurrentIntMap.class, "size");

    private volatile int[] array;

    private volatile int size;

    private volatile int mask;

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
        return byteOffset(i);
    }

    private static long byteOffset(int i) {
        return ((long) i << shift) + base;
    }

    private static int hash(int i) {
        return (i ^ (i >>> 16)) & HASH_BITS;
    }

    private static int tableSizeFor(int capacity) {
        int number = 1;
        while (number < capacity) {
            number = number << 1;
        }
        return number <= 8 ? 16 : number;
    }

    public ConcurrentIntMap(int initialCapacity) {
        final int tableSize = tableSizeFor(initialCapacity);
        this.array = new int[tableSize];
        this.size = 0;
        this.mask = array.length - 1;
        this.maxSize = calcMaxSize(tableSize);
    }

    private int calcMaxSize(int capacity) {
        // Clip the upper bound so that there will always be at least one available slot.
        int upperBound = capacity - 1;
        return Math.min(upperBound, capacity >> 1);
    }

    public int get(final int key) {
        final int keyIdx = findKeyIdx(key);
        return keyIdx >= 0 ? getRaw(checkedByteOffset(keyIdx + 1)) : 0;
    }

    private int findKeyIdx(final int key) {
        final int startIdx = hashIdx(key);
        int idx = startIdx;
        while (true) {
            if (getRaw(checkedByteOffset(idx + 1)) == 0) {
                return -1;
            }

            if (key == getRaw(checkedByteOffset(idx))) {
                return idx;
            }

            // Conflict, keep probing ...
            if ((idx = probeNext(idx)) == startIdx) {
                return -1;
            }
        }
    }

    private int hashIdx(int key) {
        return keyIdx(hash(key) & mask, mask);
    }

    private int probeNext(int idx) {
        return keyIdx((idx + 2) & mask, mask);
    }

    private int keyIdx(int idx, int maxIdx) {
        if ((idx & 0x01) == 0) {
            return idx;
        } else {
            return idx + 1 <= maxIdx ? idx + 1 : idx - 1;
        }
    }

    private int getRaw(long offset) {
        return unsafe.getIntVolatile(array, offset);
    }

    public int put(int key, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Invalid value[" + value + "]");
        }

        final int startIdx = hashIdx(key);
        int idx = startIdx;
        while (true) {
            if (compareAndSetRaw(checkedByteOffset(idx + 1), 0, value)) {
                setRaw(idx, key);
                growSize();
                return 0;
            } else if (key == getRaw(checkedByteOffset(idx))) { // replace
                final int oldValue = getRaw(checkedByteOffset(idx + 1));
                setRaw(idx + 1, value);
                return oldValue;
            }

            if ((idx = probeNext(idx)) == startIdx) {
                throw new IllegalStateException("Unable to insert");
            }
        }
    }

    private boolean compareAndSetRaw(long offset, int expect, int update) {
        return unsafe.compareAndSwapInt(array, offset, expect, update);
    }

    private void setRaw(int idx, int newValue) {
        unsafe.putIntVolatile(array, checkedByteOffset(idx), newValue);
    }

    private void growSize() {
        if (SIZE_UPDATER.incrementAndGet(this) >= maxSize) {
            if (array.length == Integer.MAX_VALUE) {
                throw new IllegalStateException("Max capacity reached at size=" + size);
            }

            synchronized (this) {
                if (size >= maxSize) {
                    // Double the capacity.
                    rehash(array.length << 1);
                }
            }
        }
    }

    private void rehash(int newCapacity) {
        final int[] oldArray = this.array;
        final int[] newArray = new int[newCapacity];
        this.array = newArray;
        this.maxSize = calcMaxSize(newCapacity);
        this.mask = newCapacity - 1;

        // Insert to the new arrays.
        for (int i = 0; i < oldArray.length; i += 2) {
            final int oldKey = oldArray[i];
            final int oldVal = oldArray[i + 1];
            if (oldKey >= 0 && oldVal != 0) {
                // Inlined put(), but much simpler: we don't need to worry about
                // duplicated keys, growing/rehashing, or failing to insert.
                int index = hashIdx(oldKey);
                while (true) {
                    if (newArray[index] == 0 && newArray[index + 1] <= 0) {
                        newArray[index] = oldKey;
                        newArray[index + 1] = oldVal;
                        break;
                    }

                    // Conflict, keep probing. Can wrap around, but never reaches startIndex again.
                    index = probeNext(index);
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
        unsafe.setMemory(array, byteOffset(0), (long) array.length * scale, (byte) 0);
    }
}

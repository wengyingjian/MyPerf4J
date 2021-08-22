package cn.myperf4j.base.util.concurrent;

import cn.myperf4j.base.util.Logger;
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

    private static final int SIZE_CTL_RACING = -1;
    private static final int SIZE_CTL_REHASHING = -2;

    private static final float DEFAULT_LOAD_FACTOR = 0.5F;

    private static final int MAX_CAPACITY = MAX_VALUE >> 1;

    private static final AtomicIntegerFieldUpdater<ConcurrentIntCounter> SIZE_UPDATER;

    private static final long SIZE_CTL;

    static {
        try {
            SIZE_CTL = unsafe.objectFieldOffset(ConcurrentIntCounter.class.getDeclaredField("sizeCtl"));
            SIZE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(ConcurrentIntCounter.class, "size");
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private volatile int[] array;

    private volatile int[] nextArray;

    private volatile int size;

    private volatile int sizeCtl;

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
        this.sizeCtl = 0;
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
        Logger.info("addAndGet（" + key + ", " + delta + ")...");
        if (delta == 0) {
            return get(key);
        }

        final int[] array = getArray();
        final int mask = array.length - 1;
        final int startIdx = hashIdx(key, mask);
        int idx = startIdx;

        long kv, kOffset;
        while (true) {
            kOffset = byteOffset(idx, mask);
            if ((kv = getLongRaw(array, kOffset)) == 0L) { //try set
                if (unsafe.compareAndSwapLong(array, kOffset, 0L, getKvLong(key, delta))) {
                    growSize(key, delta);
                    return delta;
                } else if ((int) (kv = getLongRaw(array, kOffset)) == key) {
                    if (tryAddLong(array, kOffset, key, delta)) {
                        return getValue(kv) + delta;
                    } else { //说明正在 rehash
                        waitingTransfer(key, delta);
                        return addAndGet(key, delta);
                    }
                }
            } else if (getKey(kv) == key) { //increase
                if (tryAddLong(array, kOffset, key, delta)) {
                    return getValue(kv) + delta;
                } else { //说明正在 rehash
                    waitingTransfer(key, delta);
                    return addAndGet(key, delta);
                }
            }

            if ((idx = probeNext(idx, mask)) == startIdx) {
                Logger.warn("Unable to insert1, so try to growArray(" + key + ", " + delta + ")");
                growArray(key, delta);
                return addAndGet(key, delta);
            }
        }
    }

    private int[] getArray() {
        return this.sizeCtl != SIZE_CTL_REHASHING ? this.array : this.nextArray;
    }

    private boolean tryAddLong(final int[] array, final long byteOffset, final int key, final int delta) {
        while (true) {
            final long current = getLongRaw(array, byteOffset);
            if (key != getKey(current)) {
                return false;
            }

            final long next = getKvLong(key, getValue(current) + delta);
            if (unsafe.compareAndSwapLong(array, byteOffset, current, next)) {
                return true;
            }
        }
    }

    private void waitingTransfer(final int key, final int delta) {
        while (sizeCtl < 0) {
            Logger.info("waiting, key=" + key + ", delta=" + delta);
            synchronized (this) {
                while (sizeCtl < 0) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        Logger.info("wait finished! key=" + key + ", " + "delta=" + delta);
                    }
                }
            }
        }
    }

    private void growSize(final int key, final int delta) {
        Logger.info("growSize(): key=" + key + ", " + "delta=" + delta);
        if (SIZE_UPDATER.incrementAndGet(this) < maxSize) {
            return;
        }

        growArray(key, delta);
    }

    private void growArray(final int key, final int delta) {
        final int length = array.length;
        if (length >= MAX_CAPACITY) {
            throw new IllegalStateException("Max capacity reached at size=" + size);
        }

        final int newCapacity = length << 1;
        Logger.info("trying to transfer, key=" + key + ", delta=" + delta + ", newCapacity=" + newCapacity);

        int sc;
        while (true) {
            if ((sc = sizeCtl) < 0) {
                Logger.info("begin yield, sc=" + sc + ", " + "newCapacity=" + newCapacity);
                waitingTransfer(key, delta);
                return;
            } else if (this.array.length >= newCapacity) { //不应该拿 sc 和 newCapacity 做对比！！！
                Logger.info("sizeCtl=" + sc + ", array.length[" + array.length + "] >= newCapacity[" + newCapacity +
                        "], so break!");
                return;
            } else if (unsafe.compareAndSwapInt(this, SIZE_CTL, sc, SIZE_CTL_RACING)) {
                Logger.info("transferring, key=" + key + ", delta=" + delta + " sc=" + sc + ", newCapacity=" + newCapacity);
                synchronized (this) {
                    try {
                        this.nextArray = new int[newCapacity];
                        this.sizeCtl = SIZE_CTL_REHASHING;
                        transfer(this.array, this.nextArray);
//                        Logger.error("after transfer, key=" + key + ", " + "delta=" + delta + " sc=" + sc +
//                                ", newCapacity=" + newCapacity + ", array=" + Arrays.toString(this.array) + ", " +
//                                "nextArray=" + Arrays.toString(nextArray));

                        this.array = this.nextArray;
                        this.maxSize = calcMaxSize(newCapacity >> 1);
                    } finally {
                        this.sizeCtl = sc;
                        this.nextArray = null;
                        notifyAll();
                        Logger.info("transferred, key=" + key + ", " + "delta=" + delta);
                    }
                }
                break;
            }
        }
    }

    private void transfer(final int[] fromArray, final int[] toArray) {
        final int fromMask = fromArray.length - 1;
        final int toMask = toArray.length - 1;

        Logger.info("transfer(): begin rehash, fromArray=" + Arrays.toString(fromArray));
        for (int k = 0; k < fromArray.length; k += 2) {
            final long offset = byteOffset(k, fromMask);
            final long kvLong = getAndReset(fromArray, offset);
            if (kvLong == 0L) {
                continue;
            }

            final int key = getKey(kvLong);
            final int value = getValue(kvLong);
            final int startIdx = hashIdx(key, toMask);
            int idx = startIdx;
            long kOffset, kv;
            while (true) {
                kOffset = byteOffset(idx, toMask);
                if ((kv = getLongRaw(toArray, kOffset)) == 0L) { //try set
                    Logger.info("transfer(): try CAS: key=" + key + ", value=" + value);
                    if (unsafe.compareAndSwapLong(toArray, kOffset, 0L, kvLong)) {
                        Logger.info("transfer(): try CAS SUCCESS");
                        break;
                    } else if (getIntRaw(toArray, kOffset) == key) {
                        Logger.info("transfer(): try CAS FAILURE but getIntRaw(toArray, kOffset)=" + getIntRaw(toArray, kOffset));
                        if (tryAddLong(toArray, kOffset, key, value)) {
                            SIZE_UPDATER.decrementAndGet(this);
                            break;
                        }
                        Logger.error("transfer(): It should not be arrive here2!");
                    } else {
                        Logger.warn("transfer(): try CAS FAILURE and getIntRaw(toArray, kOffset)=" + getIntRaw(toArray, kOffset));
                    }
                } else if (getKey(kv) == key) { //increase
                    Logger.info("transfer(): tryAddLong: key=" + key + ", value=" + value);
                    if (tryAddLong(toArray, kOffset, key, value)) {
                        SIZE_UPDATER.decrementAndGet(this);
                        break;
                    }
                    Logger.error("transfer(): It should not be arrive here1!");
                }

                if ((idx = probeNext(idx, toMask)) == startIdx) {
                    throw new IllegalStateException("Unable to insert2");
                }
            }
        }
        Logger.info("transfer(): rehashed！");
    }

    private long getAndReset(final int[] array, final long offset) {
        while (true) {
            final long current = getLongRaw(array, offset);
            if (unsafe.compareAndSwapLong(array, offset, current, 0L)) {
                return current;
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
        this.sizeCtl = 0;
        this.nextArray = null;
        unsafe.setMemory(array, byteOffset(0, mask), (long) array.length * scale, (byte) 0);
    }

    @Override
    public String toString() {
        return "ConcurrentIntCounter{" +
                "array=" + Arrays.toString(array) +
                ", nextArray=" + Arrays.toString(nextArray) +
                ", size=" + size +
                ", sizeCtl=" + sizeCtl +
                ", maxSize=" + maxSize +
                '}';
    }
}

package com.ebaolife.bedrock.sidecar.common.buffer;

import com.ebaolife.bedrock.sidecar.common.util.NumUtils;

import java.util.Arrays;

/**
 * Created by LinShunkang on 2022/08/14
 */
public class LongBuf implements AutoCloseable {

    private final long[] buf;

    private int writerIndex;

    private final LongBufPool pool;

    public LongBuf(int capacity, LongBufPool pool) {
        this.buf = new long[capacity];
        this.writerIndex = 0;
        this.pool = pool;
    }

    public LongBuf(int capacity) {
        this.buf = new long[capacity];
        this.writerIndex = 0;
        this.pool = null;
    }

    public void write(long kv) {
        ensureWritable(1);
        this.buf[writerIndex++] = kv;
    }

    public void write(int key, int value) {
        ensureWritable(1);
        this.buf[writerIndex++] = NumUtils.composeKv(key, value);
    }

    private void ensureWritable(int minWritableSize) {
        if (minWritableSize > buf.length - writerIndex) {
            throw new IndexOutOfBoundsException("LongBuf minWritableSize(" + minWritableSize +
                    ") + writerIndex(" + writerIndex + ") exceed buf.length(" + buf.length + ")");
        }
    }

    private void checkBounds(int index) {
        if (index >= buf.length) {
            throw new IndexOutOfBoundsException("LongBuf index(" + index + ") exceed buf.length(" + buf.length + ")");
        }
    }

    public int capacity() {
        return buf.length;
    }

    public int writerIndex() {
        return writerIndex;
    }

    public long getLong(int index) {
        checkBounds(index);
        return buf[index];
    }

    public long[] _buf() {
        return buf;
    }

    public LongBuf reset() {
        this.writerIndex = 0;
        return this;
    }

    public LongBufPool pool() {
        return pool;
    }

    @Override
    public void close() {
        if (pool != null) {
            pool.release(reset());
        }
    }

    @Override
    public String toString() {
        return "LongBuf{" +
                "buf=" + Arrays.toString(buf) +
                ", writerIndex=" + writerIndex +
                ", pool=" + pool +
                '}';
    }
}

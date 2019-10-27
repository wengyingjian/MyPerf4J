package cn.myperf4j.base.util;

import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Created by LinShunkang on 2019/10/20
 */
public final class ArrayUtils {

    public static void reset(AtomicIntegerArray array) {
        for (int i = 0; i < array.length(); ++i) {
            array.set(i, 0);
        }
    }

}

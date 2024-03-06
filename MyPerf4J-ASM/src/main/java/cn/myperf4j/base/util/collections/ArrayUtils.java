package cn.myperf4j.base.util.collections;

/**
 * Created by LinShunkang on 2020/05/16
 */
public final class ArrayUtils {

    private ArrayUtils() {
        //empty
    }

    public static boolean isEmpty(byte[] arr) {
        return arr == null || arr.length <= 0;
    }

    public static boolean isNotEmpty(byte[] arr) {
        return !isEmpty(arr);
    }
}

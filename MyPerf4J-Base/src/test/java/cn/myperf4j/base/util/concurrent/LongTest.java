package cn.myperf4j.base.util.concurrent;

/**
 * Created by LinShunkang on 2021/08/24
 */
public class LongTest {

    public static void main(String[] args) {
        final long l = 4294967296L;
        System.out.println(l + ": key=" + getKey(l) + ", value=" + getValue(l));
    }

    private static int getKey(long kvLong) {
        return (int) kvLong;
    }

    private static int getValue(long kvLong) {
        return (int) (kvLong >> 32);
    }
}

package cn.myperf4j.common.config;

import cn.myperf4j.common.util.Logger;

import java.util.Properties;

import static cn.myperf4j.common.util.StrUtils.trimToEmpty;

/**
 * Created by LinShunkang on 2018/4/9
 */
public final class MyProperties {

    /**
     * 本地配置文件，提供基础配置及兜底配置
     */
    private static Properties properties;

    /**
     * apollo默认配置，提供默认配置
     */
    private static Properties apolloDefaultProperties;

    /**
     * apollo应用配置，提供自定义配置
     */
    private static Properties apolloAppProperties;

    private MyProperties() {
        //empty
    }

    public static synchronized boolean initial(Properties prop) {
        if (properties != null || prop == null) {
            return false;
        }

        properties = prop;
        return true;
    }

    public static synchronized boolean initialAppApollo(Properties prop) {
        if (apolloAppProperties != null || prop == null) {
            return false;
        }

        apolloAppProperties = prop;
        return true;
    }

    public static synchronized boolean initialDefaultApollo(Properties prop) {
        if (apolloDefaultProperties != null || prop == null) {
            return false;
        }

        apolloDefaultProperties = prop;
        return true;
    }

    public static String getStr(ConfigKey confKey) {
        final String str = getStr0(confKey.key());
        if (str != null) {
            return trimToEmpty(str);
        }
        return trimToEmpty(getStr0(confKey.legacyKey()));
    }

    public static String getStr(String key) {
        return trimToEmpty(getStr0(key));
    }

    private static String getStr0(String key) {
        checkState();

        //环境变量优先级最高
        final String value = System.getProperty(key);
        if (value != null) {
            return value;
        }

        //apollo应用配置优先级第二
        if (apolloAppProperties != null) {
            final String apolloValue = apolloAppProperties.getProperty(key);
            if (apolloValue != null) {
                return apolloValue;
            }
        }

        //apollo默认配置优先级第三
        if (apolloDefaultProperties != null) {
            final String apolloValue = apolloDefaultProperties.getProperty(key);
            if (apolloValue != null) {
                return apolloValue;
            }
        }

        //配置文件优先级第四
        return properties.getProperty(key);
    }

    private static void checkState() {
        if (properties == null) {
            throw new IllegalStateException("MyProperties is not initial yet!!!");
        }
    }

    public static void setStr(String key, String value) {
        checkState();

        System.setProperty(key, value);
        properties.setProperty(key, value);
    }

    public static String getStr(ConfigKey confKey, String defaultValue) {
        final String result = getStr0(confKey.key());
        if (result != null) {
            return trimToEmpty(result);
        }
        return defaultValue;
    }

    public static String getStr(String key, String defaultValue) {
        final String result = getStr0(key);
        if (result != null) {
            return trimToEmpty(result);
        }
        return defaultValue;
    }

    public static int getInt(ConfigKey confKey, int defaultValue) {
        final Integer result = getInt(confKey.key());
        if (result != null) {
            return result;
        }
        return getInt(confKey.legacyKey(), defaultValue);
    }

    public static Integer getInt(ConfigKey confKey) {
        final Integer result = getInt(confKey.key());
        if (result != null) {
            return result;
        }
        return getInt(confKey.legacyKey());
    }

    public static Integer getInt(String key) {
        final String result = getStr0(key);
        if (result == null) {
            return null;
        }

        try {
            return Integer.valueOf(result);
        } catch (Exception e) {
            Logger.error("MyProperties.getInt(" + key + ")", e);
        }
        return null;
    }

    public static int getInt(String key, int defaultValue) {
        final String result = getStr0(key);
        if (result == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(result);
        } catch (Exception e) {
            Logger.error("MyProperties.getInt(" + key + ", " + defaultValue + ")", e);
        }
        return defaultValue;
    }

    public static long getLong(ConfigKey confKey, long defaultValue) {
        final Long l = getLong(confKey.key());
        if (l != null) {
            return l;
        }
        return getLong(confKey.legacyKey(), defaultValue);
    }

    public static Long getLong(String key) {
        final String result = getStr0(key);
        if (result == null) {
            return null;
        }

        try {
            return Long.valueOf(result);
        } catch (Exception e) {
            Logger.error("MyProperties.getLong(" + key + ")", e);
        }
        return null;
    }

    public static long getLong(String key, long defaultValue) {
        final String result = getStr0(key);
        if (result == null) {
            return defaultValue;
        }

        try {
            return Long.parseLong(result);
        } catch (Exception e) {
            Logger.error("MyProperties.getLong(" + key + ", " + defaultValue + ")", e);
        }
        return defaultValue;
    }

    public static long getLong(String key, long defaultValue, long minValue) {
        final long result = getLong(key, defaultValue);
        if (result <= minValue) {
            return minValue;
        }
        return result;
    }

    public static boolean isSame(String key, String expectValue) {
        if (expectValue == null) {
            throw new IllegalArgumentException("isSame(" + key + ", null): expectValue must not null!!!");
        }
        return expectValue.equals(getStr0(key));
    }

    public static boolean getBoolean(ConfigKey confKey, boolean defaultValue) {
        final Boolean result = getBoolean(confKey.key());
        if (result != null) {
            return result;
        }
        return getBoolean(confKey.legacyKey(), defaultValue);
    }

    public static Boolean getBoolean(String key) {
        final String result = getStr0(key);
        if (result != null) {
            return result.equalsIgnoreCase("true");
        }
        return null;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        final String result = getStr0(key);
        if (result != null) {
            return result.equalsIgnoreCase("true");
        }
        return defaultValue;
    }
}

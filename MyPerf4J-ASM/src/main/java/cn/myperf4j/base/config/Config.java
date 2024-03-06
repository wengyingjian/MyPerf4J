package cn.myperf4j.base.config;

import cn.myperf4j.base.util.Logger;
import cn.myperf4j.base.util.StrUtils;
import cn.myperf4j.base.util.collections.MapUtils;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static cn.myperf4j.base.config.MyProperties.*;
import static cn.myperf4j.base.constant.PropertyKeys.Basic.*;
import static cn.myperf4j.base.constant.PropertyKeys.Filter.*;
import static cn.myperf4j.base.constant.PropertyKeys.HttpServer.*;
import static cn.myperf4j.base.constant.PropertyKeys.Metrics.CLASS_LEVEL_MAPPINGS;
import static cn.myperf4j.base.constant.PropertyKeys.Recorder.*;
import static cn.myperf4j.base.constant.PropertyValues.Recorder.MODE_ACCURATE;
import static cn.myperf4j.base.util.NumUtils.parseInt;
import static cn.myperf4j.base.util.StrUtils.isBlank;

public class Config {

    @Data
    public static class BasicConfig {

        private String appName;

        private String configFileDir;

        private boolean debug;

        public static BasicConfig loadBasicConfig() {
            String appName = getStr(APP_NAME);
            if (isBlank(appName)) {
                throw new IllegalArgumentException(APP_NAME.key() + "|" + APP_NAME.legacyKey() + " is required!!!");
            }

            BasicConfig config = new BasicConfig();
            config.setAppName(appName);
            config.setDebug(getBoolean(DEBUG, false));
            config.setConfigFileDir(getStr(PROPERTIES_FILE_DIR));
            return config;
        }

        public String sysProfilingParamsFile() {
            return configFileDir + "." + appName + "_SysGenProfilingFile";
        }

    }

    @Data
    public static class FilterConfig {

        private String excludeClassLoaders;

        private String includePackages;

        private String excludePackages;

        private String excludeMethods;

        private boolean excludePrivateMethod;

        public static FilterConfig loadFilterConfig() {
            final String includePackages = getStr(PACKAGES_INCLUDE);
            if (StrUtils.isBlank(includePackages)) {
                throw new IllegalArgumentException(PACKAGES_INCLUDE.key() + " or " + PACKAGES_INCLUDE.legacyKey() +
                        " is required!!!");
            }

            final FilterConfig config = new FilterConfig();
            config.setIncludePackages(includePackages);
            config.setExcludeClassLoaders(getStr(CLASS_LOADERS_EXCLUDE));
            config.setExcludePackages(getStr(PACKAGES_EXCLUDE));
            config.setExcludeMethods(getStr(METHODS_EXCLUDE));
            config.setExcludePrivateMethod(getBoolean(METHODS_EXCLUDE_PRIVATE, true));
            return config;
        }
    }

    @Data
    public static class HttpServerConfig {

        private int preferencePort;

        private int minPort;

        private int maxPort;

        private int minWorkers;

        private int maxWorkers;

        private int acceptCount;

        public static HttpServerConfig loadHttpServerConfig() {
            String portStr = getStr(PORT);
            if (isBlank(portStr)) {
                portStr = "2048,2000,2040";
                Logger.info(PORT.key() + " is not configured, so use '" + portStr + "' as default.");
            }

            final HttpServerConfig config = new HttpServerConfig();
            completePorts(config, portStr);
            config.setMinWorkers(getInt(MIN_WORKERS, 1));
            config.setMaxWorkers(getInt(MAX_WORKERS, 2));
            config.setAcceptCount(getInt(ACCEPT_COUNT, 1024));
            return config;
        }

        private static void completePorts(final HttpServerConfig config, final String portStr) {
            final List<String> ports = StrUtils.splitAsList(portStr, ',');
            if (ports.size() != 3) {
                config.setPreferencePort(parseInt(ports.get(0), 2048));
                config.setMinPort(2000);
                config.setMaxPort(2040);
                return;
            }

            config.setPreferencePort(parseInt(ports.get(0), 2048));
            config.setMinPort(parseInt(ports.get(1), 2000));
            config.setMaxPort(parseInt(ports.get(2), 2040));
        }
    }

    @Data
    public static class MetricsConfig {

        private String classLevelMapping;

        public static MetricsConfig loadMetricsConfig() {
            MetricsConfig config = new MetricsConfig();
            config.setClassLevelMapping(getStr(CLASS_LEVEL_MAPPINGS));
            return config;
        }


    }

    @Data
    public static class RecorderConfig {

        private String mode;

        private int backupCount;

        private int timingArrSize;

        private int timingMapSize;

        private ProfilingParams commonProfilingParams;

        private final Map<String, ProfilingParams> profilingParamsMap = MapUtils.createHashMap(1024);

        public static RecorderConfig loadRecorderConfig() {
            final RecorderConfig config = new RecorderConfig();
            config.setMode(getStr(MODE, MODE_ACCURATE));
            config.setBackupCount(getInt(BACKUP_COUNT, 1));
            config.setTimingArrSize(getInt(SIZE_TIMING_ARR, 1024));
            config.setTimingMapSize(getInt(SIZE_TIMING_MAP, 32));
            config.setCommonProfilingParams(ProfilingParams.of(config.getTimingArrSize(), config.getTimingMapSize()));
            return config;
        }

        public boolean accurateMode() {
            return MODE_ACCURATE.equalsIgnoreCase(mode);
        }

        public void addProfilingParam(String methodName, int timeThreshold, int outThresholdCount) {
            profilingParamsMap.put(methodName, ProfilingParams.of(timeThreshold, outThresholdCount));
        }

        public ProfilingParams getProfilingParam(String methodName) {
            ProfilingParams params = profilingParamsMap.get(methodName);
            if (params != null) {
                return params;
            }
            return commonProfilingParams;
        }


    }


}

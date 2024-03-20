package com.ebaolife.bedrock.sidecar.common.config;

import com.ebaolife.bedrock.sidecar.common.constant.PropertyKeys;
import com.ebaolife.bedrock.sidecar.common.util.Logger;
import com.ebaolife.bedrock.sidecar.common.util.NumUtils;
import com.ebaolife.bedrock.sidecar.common.util.StrUtils;
import com.ebaolife.bedrock.sidecar.common.util.collections.MapUtils;
import lombok.Data;

import java.util.List;
import java.util.Map;

public class Config {

    @Data
    public static class BasicConfig {

        private String appName;

        private String configFileDir;

        private boolean debug;

        private String apolloConfigServiceUrl;

        public static BasicConfig loadBasicConfig() {
            String appName = MyProperties.getStr(PropertyKeys.Basic.APP_NAME);
            if (StrUtils.isBlank(appName)) {
                throw new IllegalArgumentException(PropertyKeys.Basic.APP_NAME.key() + "|" + PropertyKeys.Basic.APP_NAME.legacyKey() + " is required!!!");
            }

            BasicConfig config = new BasicConfig();
            config.setAppName(appName);
            config.setDebug(MyProperties.getBoolean(PropertyKeys.Basic.DEBUG, false));
            config.setConfigFileDir(MyProperties.getStr(PropertyKeys.Basic.PROPERTIES_FILE_DIR));
            config.setApolloConfigServiceUrl(MyProperties.getStr(PropertyKeys.Basic.APOLLO_CONFIG_SERVICE_URL));
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
            final String includePackages = MyProperties.getStr(PropertyKeys.Filter.PACKAGES_INCLUDE);
            if (StrUtils.isBlank(includePackages)) {
                throw new IllegalArgumentException(PropertyKeys.Filter.PACKAGES_INCLUDE.key() + " or " + PropertyKeys.Filter.PACKAGES_INCLUDE.legacyKey() +
                        " is required!!!");
            }

            final FilterConfig config = new FilterConfig();
            config.setIncludePackages(includePackages);
            config.setExcludeClassLoaders(MyProperties.getStr(PropertyKeys.Filter.CLASS_LOADERS_EXCLUDE));
            config.setExcludePackages(MyProperties.getStr(PropertyKeys.Filter.PACKAGES_EXCLUDE));
            config.setExcludeMethods(MyProperties.getStr(PropertyKeys.Filter.METHODS_EXCLUDE));
            config.setExcludePrivateMethod(MyProperties.getBoolean(PropertyKeys.Filter.METHODS_EXCLUDE_PRIVATE, true));
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
            String portStr = MyProperties.getStr(PropertyKeys.HttpServer.PORT);
            if (StrUtils.isBlank(portStr)) {
                portStr = "2048,2000,2040";
                Logger.info(PropertyKeys.HttpServer.PORT.key() + " is not configured, so use '" + portStr + "' as default.");
            }

            final HttpServerConfig config = new HttpServerConfig();
            completePorts(config, portStr);
            config.setMinWorkers(MyProperties.getInt(PropertyKeys.HttpServer.MIN_WORKERS, 1));
            config.setMaxWorkers(MyProperties.getInt(PropertyKeys.HttpServer.MAX_WORKERS, 2));
            config.setAcceptCount(MyProperties.getInt(PropertyKeys.HttpServer.ACCEPT_COUNT, 1024));
            return config;
        }

        private static void completePorts(final HttpServerConfig config, final String portStr) {
            final List<String> ports = StrUtils.splitAsList(portStr, ',');
            if (ports.size() != 3) {
                config.setPreferencePort(NumUtils.parseInt(ports.get(0), 2048));
                config.setMinPort(2000);
                config.setMaxPort(2040);
                return;
            }

            config.setPreferencePort(NumUtils.parseInt(ports.get(0), 2048));
            config.setMinPort(NumUtils.parseInt(ports.get(1), 2000));
            config.setMaxPort(NumUtils.parseInt(ports.get(2), 2040));
        }
    }

    @Data
    public static class MetricsConfig {

        private String classLevelMapping;

        public static MetricsConfig loadMetricsConfig() {
            MetricsConfig config = new MetricsConfig();
            config.setClassLevelMapping(MyProperties.getStr(PropertyKeys.Metrics.CLASS_LEVEL_MAPPINGS));
            return config;
        }


    }

    @Data
    public static class RecorderConfig {

        private int timingArrSize;

        private int timingMapSize;

        private ProfilingParams commonProfilingParams;

        private final Map<String, ProfilingParams> profilingParamsMap = MapUtils.createHashMap(1024);

        public static RecorderConfig loadRecorderConfig() {
            final RecorderConfig config = new RecorderConfig();
            config.setTimingArrSize(MyProperties.getInt(PropertyKeys.Recorder.SIZE_TIMING_ARR, 1024));
            config.setTimingMapSize(MyProperties.getInt(PropertyKeys.Recorder.SIZE_TIMING_MAP, 32));
            config.setCommonProfilingParams(ProfilingParams.of(config.getTimingArrSize(), config.getTimingMapSize()));
            return config;
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

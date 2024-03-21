package com.ebaolife.bedrock.sidecar.common.config;

/**
 * Created by LinShunkang on 2018/5/12
 */
public final class ProfilingConfig {

    private static Config.BasicConfig BASIC_CONFIG;

    private static Config.HttpServerConfig HTTP_SERVER_CONFIG;

    private static Config.MetricsConfig METRICS_CONFIG;
    private static Config.ArthasConfig ARTHAS_CONFIG;

    private static Config.FilterConfig FILTER_CONFIG;

    private static Config.RecorderConfig RECORDER_CONFIG;

    private ProfilingConfig() {
        //empty
    }

    public static Config.BasicConfig basicConfig() {
        return BASIC_CONFIG;
    }

    public static void basicConfig(Config.BasicConfig basicConfig) {
        BASIC_CONFIG = basicConfig;
    }

    public static Config.HttpServerConfig httpServerConfig() {
        return HTTP_SERVER_CONFIG;
    }

    public static void httpServerConfig(Config.HttpServerConfig httpServerConfig) {
        HTTP_SERVER_CONFIG = httpServerConfig;
    }

    public static Config.MetricsConfig metricsConfig() {
        return METRICS_CONFIG;
    }

    public static void metricsConfig(Config.MetricsConfig metricsConfig) {
        ProfilingConfig.METRICS_CONFIG = metricsConfig;
    }

    public static Config.FilterConfig filterConfig() {
        return FILTER_CONFIG;
    }

    public static void filterConfig(Config.FilterConfig filterConfig) {
        ProfilingConfig.FILTER_CONFIG = filterConfig;
    }

    public static Config.RecorderConfig recorderConfig() {
        return RECORDER_CONFIG;
    }

    public static void recorderConfig(Config.RecorderConfig recorderConfig) {
        RECORDER_CONFIG = recorderConfig;
    }

    public static Config.ArthasConfig getArthasConfig() {
        return ARTHAS_CONFIG;
    }

    public static void setArthasConfig(Config.ArthasConfig arthasConfig) {
        ARTHAS_CONFIG = arthasConfig;
    }
}

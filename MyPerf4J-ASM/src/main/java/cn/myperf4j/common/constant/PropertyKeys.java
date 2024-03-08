package cn.myperf4j.common.constant;

import cn.myperf4j.common.config.ConfigKey;

/**
 * Created by LinShunkang on 2018/4/27
 */
public interface PropertyKeys {

    String PRO_FILE_NAME = "MyPerf4JPropFile";

    interface Basic {

        ConfigKey APP_NAME = ConfigKey.of("app_name", "AppName");

        ConfigKey DEBUG = ConfigKey.of("debug", "Debug.PrintDebugLog");

        ConfigKey PROPERTIES_FILE_DIR = ConfigKey.of("properties.dir", "MyPerf4JPropDIR");

        ConfigKey APOLLO_CONFIG_SERVICE_URL = ConfigKey.of("apollo.config.service.url", "apollo.config.service.url");
    }

    interface HttpServer {

        ConfigKey PORT = ConfigKey.of("http.server.port", "http.server.port");

        ConfigKey MIN_WORKERS = ConfigKey.of("http.server.min_workers", "http.server.min_workers");

        ConfigKey MAX_WORKERS = ConfigKey.of("http.server.max_workers", "http.server.max_workers");

        ConfigKey ACCEPT_COUNT = ConfigKey.of("http.server.accept_count", "http.server.accept_count");
    }

    interface Metrics {
        ConfigKey CLASS_LEVEL_MAPPINGS = ConfigKey.of("metrics.method.class_level_mappings", "ClassLevelMapping");

    }


    interface Filter {

        ConfigKey PACKAGES_INCLUDE = ConfigKey.of("filter.packages.include", "IncludePackages");

        ConfigKey PACKAGES_EXCLUDE = ConfigKey.of("filter.packages.exclude", "ExcludePackages");

        ConfigKey METHODS_EXCLUDE = ConfigKey.of("filter.methods.exclude", "ExcludeMethods");

        ConfigKey METHODS_EXCLUDE_PRIVATE = ConfigKey.of("filter.methods.exclude_private", "ExcludePrivateMethod");

        ConfigKey CLASS_LOADERS_EXCLUDE = ConfigKey.of("filter.class_loaders.exclude", "ExcludeClassLoaders");
    }

    interface Recorder {

        ConfigKey BACKUP_COUNT = ConfigKey.of("recorders.backup_count", "BackupRecordersCount");

        ConfigKey MODE = ConfigKey.of("recorder.mode", "RecorderMode");

        ConfigKey SIZE_TIMING_ARR = ConfigKey.of("recorder.size.timing_arr", "ProfilingTimeThreshold");

        ConfigKey SIZE_TIMING_MAP = ConfigKey.of("recorder.size.timing_map", "ProfilingOutThresholdCount");
    }
}

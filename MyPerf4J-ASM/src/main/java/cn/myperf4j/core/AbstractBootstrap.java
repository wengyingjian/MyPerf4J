package cn.myperf4j.core;

import cn.hutool.json.JSONUtil;
import cn.myperf4j.common.Version;
import cn.myperf4j.common.apollo.ApolloClient;
import cn.myperf4j.common.config.*;
import cn.myperf4j.common.constant.PropertyValues.Separator;
import cn.myperf4j.common.http.HttpHeaders;
import cn.myperf4j.common.http.HttpRequest;
import cn.myperf4j.common.http.HttpResponse;
import cn.myperf4j.common.http.server.Dispatcher;
import cn.myperf4j.common.http.server.SimpleHttpServer;
import cn.myperf4j.common.util.Logger;
import cn.myperf4j.common.util.NumUtils;
import cn.myperf4j.common.util.StrUtils;
import cn.myperf4j.core.prometheus.MemoryExport;
import cn.myperf4j.core.prometheus.WebContainerExport;
import cn.myperf4j.core.prometheus.format.ApplicationTextFormat;
import cn.myperf4j.core.recorder.AbstractRecorderMaintainer;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.hotspot.*;

import java.io.*;
import java.util.*;

import static cn.myperf4j.common.config.Config.BasicConfig.loadBasicConfig;
import static cn.myperf4j.common.config.Config.FilterConfig.loadFilterConfig;
import static cn.myperf4j.common.config.Config.HttpServerConfig.loadHttpServerConfig;
import static cn.myperf4j.common.config.Config.MetricsConfig.loadMetricsConfig;
import static cn.myperf4j.common.config.Config.RecorderConfig.loadRecorderConfig;
import static cn.myperf4j.common.constant.PropertyKeys.Basic.PROPERTIES_FILE_DIR;
import static cn.myperf4j.common.constant.PropertyKeys.PRO_FILE_NAME;
import static cn.myperf4j.common.constant.PropertyValues.DEFAULT_PRO_FILE;
import static cn.myperf4j.common.constant.PropertyValues.Separator.ELE;
import static cn.myperf4j.common.constant.PropertyValues.Separator.ELE_KV;
import static cn.myperf4j.common.http.HttpRespStatus.NOT_FOUND;
import static cn.myperf4j.common.http.HttpRespStatus.OK;
import static cn.myperf4j.common.util.StrUtils.splitAsList;
import static cn.myperf4j.common.util.SysProperties.LINE_SEPARATOR;
import static cn.myperf4j.common.util.net.NetUtils.isPortAvailable;

/**
 * Created by LinShunkang on 2018/4/11
 */
public abstract class AbstractBootstrap {

    private volatile boolean initStatus;

    protected AbstractRecorderMaintainer maintainer;

    public final boolean initial() {
        try {
            if (initStatus) {
                Logger.warn("AbstractBootstrap is already init.");
                return true;
            }

            Logger.info("Thanks sincerely for using MyPerf4J.");
            if (!(initStatus = doInitial())) {
                Logger.error("AbstractBootstrap doInitial() FAILURE!!!");
                return false;
            }

            printBannerText();
            Logger.info("AbstractBootstrap doInitial() SUCCESS!!!");
            return true;
        } catch (Exception e) {
            Logger.error("AbstractBootstrap.initial()", e);
        }
        return false;
    }

    private boolean doInitial() {
        if (!initProperties()) {
            Logger.error("AbstractBootstrap initProperties() FAILURE!!!");
            return false;
        }

        if (!initBasicConfig()) {
            Logger.error("AbstractBootstrap initProfilingConfig() FAILURE!!!");
            return false;
        }

        if (!initApolloProperties()) {
            Logger.error("AbstractBootstrap initApolloProperties() FAILURE!!!");
            return false;
        }

        if (!initProfilingConfig()) {
            Logger.error("AbstractBootstrap initProfilingConfig() FAILURE!!!");
            return false;
        }

        if (!initLogger()) {
            Logger.error("AbstractBootstrap initLogger() FAILURE!!!");
            return false;
        }

        if (!initPackageFilter()) {
            Logger.error("AbstractBootstrap initPackageFilter() FAILURE!!!");
            return false;
        }

        if (!initClassLoaderFilter()) {
            Logger.error("AbstractBootstrap initClassLoaderFilter() FAILURE!!!");
            return false;
        }

        if (!initMethodFilter()) {
            Logger.error("AbstractBootstrap initMethodFilter() FAILURE!!!");
            return false;
        }

        if (!initClassLevelMapping()) {
            Logger.error("AbstractBootstrap initClassLevelMapping() FAILURE!!!");
            return false;
        }

        if (!initProfilingParams()) {
            Logger.error("AbstractBootstrap initProfilingParams() FAILURE!!!");
            return false;
        }

        if (!initRecorderMaintainer()) {
            Logger.error("AbstractBootstrap initRecorderMaintainer() FAILURE!!!");
            return false;
        }

        if (!initHttpServer()) {
            Logger.error("AbstractBootstrap initHttpServer() FAILURE!!!");
            return false;
        }

        if (!initPrometheus()) {
            Logger.error("AbstractBootstrap initPrometheus() FAILURE!!!");
            return false;
        }

        if (!initOther()) {
            Logger.error("AbstractBootstrap initOther() FAILURE!!!");
            return false;
        }
        return true;
    }

    private boolean initBasicConfig() {
        try {
            ProfilingConfig.basicConfig(loadBasicConfig());
            return true;
        } catch (Exception e) {
            Logger.error("AbstractBootstrap.initProfilingConfig()", e);
        }
        return false;
    }

    private boolean initApolloProperties() {
        try {
            //从apollo加载
            String json = ApolloClient.fetchApolloConfig(ProfilingConfig.basicConfig().getApolloConfigServiceUrl());
            Map<String, String> map = JSONUtil.toBean(json, Map.class);

            String appName = ProfilingConfig.basicConfig().getAppName();
            Properties properties = new Properties();

            Logger.info("loading apollo config: appName=" + appName);

            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                int prefixIndex = key.indexOf(".");
                if (prefixIndex == -1) {
                    continue;
                }
                String keyPrefix = key.substring(0, prefixIndex);
                if (Objects.equals(appName, keyPrefix)) {
                    key = key.substring(prefixIndex + 1);
                    properties.setProperty(key, entry.getValue());
                    Logger.info("loading apollo config:" + key + "=" + entry.getValue());
                }
            }
            return MyProperties.initialApollo(properties);
        } catch (Exception e) {
            Logger.error("AbstractBootstrap.initApolloProperties()", e);
        }
        return false;
    }

    public boolean initPrometheus() {
        CollectorRegistry registry = CollectorRegistry.defaultRegistry;

        (new StandardExports()).register(registry);
        (new MemoryPoolsExports()).register(registry);
        (new MemoryAllocationExports()).register(registry);
        (new BufferPoolsExports()).register(registry);
        (new GarbageCollectorExports()).register(registry);
        (new ThreadExports()).register(registry);
        (new ClassLoadingExports()).register(registry);

        (new WebContainerExport()).register(registry);
        (new MemoryExport()).register(registry);
        return true;
    }

    private boolean initProperties() {
        final String configFilePath = System.getProperty(PRO_FILE_NAME, DEFAULT_PRO_FILE);
        try (InputStream in = new FileInputStream(configFilePath)) {
            Properties properties = new Properties();
            properties.load(in);

            properties.put(PROPERTIES_FILE_DIR.key(), parseConfigFileDir(configFilePath));
            return MyProperties.initial(properties);
        } catch (IOException e) {
            Logger.error("AbstractBootstrap.initProperties()", e);
        }
        return false;
    }

    private String parseConfigFileDir(String configFilePath) {
        final int idx = configFilePath.lastIndexOf(File.separatorChar);
        return configFilePath.substring(0, idx + 1);
    }

    private boolean initProfilingConfig() {
        try {
            ProfilingConfig.metricsConfig(loadMetricsConfig());
            ProfilingConfig.filterConfig(loadFilterConfig());
            ProfilingConfig.recorderConfig(loadRecorderConfig());
            ProfilingConfig.httpServerConfig(loadHttpServerConfig());
            return true;
        } catch (Exception e) {
            Logger.error("AbstractBootstrap.initProfilingConfig()", e);
        }
        return false;
    }

    private boolean initLogger() {
        try {
            Logger.setDebugEnable(ProfilingConfig.basicConfig().isDebug());
            return true;
        } catch (Exception e) {
            Logger.error("AbstractBootstrap.initLogger()", e);
        }
        return false;
    }

    private boolean initPackageFilter() {
        try {
            final Config.FilterConfig filterConfig = ProfilingConfig.filterConfig();
            final String includePackages = filterConfig.getIncludePackages();
            final List<String> includeList = splitAsList(includePackages, ELE);
            for (int i = 0; i < includeList.size(); i++) {
                ProfilingFilter.addIncludePackage(includeList.get(i));
            }

            final String excludePackages = filterConfig.getExcludePackages();
            final List<String> excludeList = splitAsList(excludePackages, ELE);
            for (int i = 0; i < excludeList.size(); i++) {
                ProfilingFilter.addExcludePackage(excludeList.get(i));
            }
            return true;
        } catch (Exception e) {
            Logger.error("AbstractBootstrap.initPackageFilter()", e);
        }
        return false;
    }

    private boolean initClassLoaderFilter() {
        try {
            final Config.FilterConfig filterConfig = ProfilingConfig.filterConfig();
            final String excludeClassLoaders = filterConfig.getExcludeClassLoaders();
            final List<String> excludeList = splitAsList(excludeClassLoaders, ELE);
            for (int i = 0; i < excludeList.size(); i++) {
                ProfilingFilter.addExcludeClassLoader(excludeList.get(i));
            }
            return true;
        } catch (Exception e) {
            Logger.error("AbstractBootstrap.initClassLoaderFilter()", e);
        }
        return false;
    }

    private boolean initMethodFilter() {
        try {
            final Config.FilterConfig filterConfig = ProfilingConfig.filterConfig();
            final String excludeMethods = filterConfig.getExcludeMethods();
            final List<String> excludeList = splitAsList(excludeMethods, ELE);
            for (int i = 0; i < excludeList.size(); i++) {
                ProfilingFilter.addExcludeMethods(excludeList.get(i));
            }
            return true;
        } catch (Exception e) {
            Logger.error("AbstractBootstrap.initMethodFilter()", e);
        }
        return false;
    }

    //MethodLevelMapping=Controller:[*Controller];Api:[*Api,*ApiImpl];
    private boolean initClassLevelMapping() {
        try {
            final Config.MetricsConfig metricsConfig = ProfilingConfig.metricsConfig();
            final String levelMappings = metricsConfig.getClassLevelMapping();
            if (StrUtils.isBlank(levelMappings)) {
                Logger.info("ClassLevelMapping is blank, so use default mappings.");
                return true;
            }

            final List<String> mappingPairs = splitAsList(levelMappings, ELE);
            for (int i = 0; i < mappingPairs.size(); ++i) {
                final String mappingPair = mappingPairs.get(i);
                final List<String> pairs = splitAsList(mappingPair, ELE_KV);
                if (pairs.size() != 2) {
                    Logger.warn("MethodLevelMapping is not correct: " + mappingPair);
                    continue;
                }

                LevelMappingFilter.putLevelMapping(pairs.get(0), getMappingExpList(pairs.get(1)));
            }
            return true;
        } catch (Exception e) {
            Logger.error("AbstractBootstrap.initClassLevelMapping()", e);
        }
        return false;
    }

    //Api:[*Api,*ApiImpl]
    private List<String> getMappingExpList(String expStr) {
        expStr = expStr.substring(1, expStr.length() - 1);
        return splitAsList(expStr, Separator.ARR_ELE);
    }

    private boolean initProfilingParams() {
        try {
            final Config.RecorderConfig recorderConf = ProfilingConfig.recorderConfig();
            if (recorderConf.accurateMode()) {
                addProfilingParams(recorderConf, ProfilingConfig.basicConfig().sysProfilingParamsFile());
            }
            return true;
        } catch (Exception e) {
            Logger.error("AbstractBootstrap.initProfilingParams()", e);
        }
        return false;
    }

    private void addProfilingParams(Config.RecorderConfig recorderConf, String filePath) {
        final File sysFile = new File(filePath);
        if (sysFile.exists() && sysFile.isFile()) {
            Logger.info("Loading " + sysFile.getAbsolutePath() + " to init profiling params.");
            addProfilingParams0(recorderConf, filePath);
        }
    }

    private void addProfilingParams0(Config.RecorderConfig recorderConf, String profilingParamFile) {
        try (InputStream in = new FileInputStream(profilingParamFile)) {
            Properties properties = new Properties();
            properties.load(in);

            Set<String> keys = properties.stringPropertyNames();
            for (String key : keys) {
                String value = properties.getProperty(key);
                if (value == null) {
                    continue;
                }

                List<String> strList = splitAsList(value, ':');
                if (strList.size() != 2) {
                    continue;
                }

                int timeThreshold = NumUtils.parseInt(strList.get(0).trim(), 1000);
                int outThresholdCount = NumUtils.parseInt(strList.get(1).trim(), 64);
                recorderConf.addProfilingParam(key.replace('.', '/'), timeThreshold, outThresholdCount);
            }
        } catch (Exception e) {
            Logger.error("AbstractBootstrap.addProfilingParams(" + profilingParamFile + ")", e);
        }
    }

    private boolean initRecorderMaintainer() {
        return (maintainer = doInitRecorderMaintainer()) != null;
    }

    public abstract AbstractRecorderMaintainer doInitRecorderMaintainer();


    private boolean initHttpServer() {
        try {
            final Config.HttpServerConfig config = ProfilingConfig.httpServerConfig();
            final SimpleHttpServer server = new SimpleHttpServer.Builder().port(choseHttpServerPort(config)).minWorkers(config.getMinWorkers()).maxWorkers(config.getMaxWorkers()).acceptCnt(config.getAcceptCount()).dispatcher(getHttpServerDispatch()).build();
            server.startAsync();
            return true;
        } catch (Exception e) {
            Logger.error("AbstractBootstrap.initHttpServer()", e);
        }
        return false;
    }

    private int choseHttpServerPort(final Config.HttpServerConfig config) {
        final int preferencePort = config.getPreferencePort();
        if (isPortAvailable(preferencePort)) {
            Logger.info("Use " + preferencePort + " as HttpServer port.");
            return preferencePort;
        }

        for (int port = config.getMinPort(); port < config.getMaxPort(); port++) {
            if (isPortAvailable(port)) {
                Logger.info("Use " + port + " as HttpServer port.");
                return port;
            }
        }
        throw new IllegalStateException("Has no available port for HttpServer!");
    }

    private Dispatcher getHttpServerDispatch() {
        return new Dispatcher() {
            @Override
            public HttpResponse dispatch(HttpRequest request) {
                switch (request.getPath()) {
                    case "/switch/debugMode":
                        Logger.setDebugEnable(request.getBoolParam("enable"));
                        break;
                    case "/actuator/prometheus":
                        Logger.info("get prometheus");
                        return new HttpResponse(OK, new HttpHeaders(0), prometheus());
                    default:
                        return new HttpResponse(NOT_FOUND, new HttpHeaders(0), "");
                }
                return new HttpResponse(OK, new HttpHeaders(0), "Success");
            }
        };
    }


    public String prometheus() {
        // 创建一个StringWriter来接收指标输出
        Writer writer = new StringWriter();
        try {
            // 将指标输出到StringWriter中
            ApplicationTextFormat.writeOpenMetrics100(writer, CollectorRegistry.defaultRegistry.metricFamilySamples(), ProfilingConfig.basicConfig().getAppName());
            // 将StringWriter中的内容打印到控制台
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException("get prometheus failed", e);
        }
    }

    public abstract boolean initOther();

    private void printBannerText() {
        Logger.info(LINE_SEPARATOR + "    __  ___      ____            ______ __      __" + LINE_SEPARATOR + "   /  |/  /_  __/ __ \\___  _____/ __/ // /     / /" + LINE_SEPARATOR + "  / /|_/ / / / / /_/ / _ \\/ ___/ /_/ // /___  / / " + LINE_SEPARATOR + " / /  / / /_/ / ____/  __/ /  / __/__  __/ /_/ /  " + LINE_SEPARATOR + "/_/  /_/\\__, /_/    \\___/_/  /_/    /_/  \\____/   " + LINE_SEPARATOR + "       /____/                                     v" + Version.getVersion() + LINE_SEPARATOR);
    }
}

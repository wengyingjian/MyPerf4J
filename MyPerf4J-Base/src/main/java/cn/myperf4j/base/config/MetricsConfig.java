package cn.myperf4j.base.config;

import static cn.myperf4j.base.config.MyProperties.getStr;
import static cn.myperf4j.base.constant.PropertyKeys.Metrics.CLASS_LEVEL_MAPPINGS;

/**
 * Created by LinShunkang on 2020/05/24
 */
public class MetricsConfig {

    private String classLevelMapping;


    public String classLevelMapping() {
        return classLevelMapping;
    }

    public void classLevelMapping(String classLevelMapping) {
        this.classLevelMapping = classLevelMapping;
    }


    public static MetricsConfig loadMetricsConfig() {
        MetricsConfig config = new MetricsConfig();
        config.classLevelMapping(getStr(CLASS_LEVEL_MAPPINGS));
        return config;
    }


}

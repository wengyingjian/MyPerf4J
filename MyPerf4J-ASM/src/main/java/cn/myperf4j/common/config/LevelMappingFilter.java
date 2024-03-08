package cn.myperf4j.common.config;

import cn.myperf4j.common.util.StrMatchUtils;
import cn.myperf4j.common.util.collections.MapUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.myperf4j.common.constant.ClassLevels.*;

/**
 * Created by LinShunkang on 2019/05/04
 * <p>
 * MethodLevelMapping=Controller:[*Controller];Api:[*Api*];
 */
public final class LevelMappingFilter {

    private static final Map<String, List<String>> LEVEL_EXPS_MAP = MapUtils.createLinkedHashMap(20);

    static {
        //Initialize the default level mappings
        LEVEL_EXPS_MAP.put(CONTROLLER, Collections.singletonList("*Controller"));
        LEVEL_EXPS_MAP.put(INTERCEPTOR, Collections.singletonList("*Interceptor"));
        LEVEL_EXPS_MAP.put(PRODUCER, Collections.singletonList("*Producer"));
        LEVEL_EXPS_MAP.put(CONSUMER, Collections.singletonList("*Consumer"));
        LEVEL_EXPS_MAP.put(LISTENER, Collections.singletonList("*Listener"));
        LEVEL_EXPS_MAP.put(FILTER, Collections.singletonList("*Filter"));
        LEVEL_EXPS_MAP.put(HANDLER, Collections.singletonList("*Handler"));
        LEVEL_EXPS_MAP.put(PROCESSOR, Collections.singletonList("*Processor"));
        LEVEL_EXPS_MAP.put(DISPATCHER, Collections.singletonList("*Dispatcher"));
        LEVEL_EXPS_MAP.put(API, Arrays.asList("*Api", "*ApiImpl"));
        LEVEL_EXPS_MAP.put(SERVICE, Arrays.asList("*Service", "*ServiceImpl"));
        LEVEL_EXPS_MAP.put(CACHE, Arrays.asList("*Cache", "*CacheImpl"));
        LEVEL_EXPS_MAP.put(DAO, Arrays.asList("*DAO", "*Dao"));
        LEVEL_EXPS_MAP.put(UTILS, Collections.singletonList("*Utils"));
    }

    private LevelMappingFilter() {
        //empty
    }

    /**
     * 根据 simpleClassName 返回 ClassLevel
     */
    public static String getClassLevel(String simpleClassName) {
        for (Map.Entry<String, List<String>> entry : LEVEL_EXPS_MAP.entrySet()) {
            String level = entry.getKey();
            List<String> mappingExps = entry.getValue();
            for (int i = 0; i < mappingExps.size(); ++i) {
                if (StrMatchUtils.isMatch(simpleClassName, mappingExps.get(i))) {
                    return level;
                }
            }
        }
        return OTHERS;
    }

    public static void putLevelMapping(String classLevel, List<String> expList) {
        LEVEL_EXPS_MAP.put(classLevel, expList);
    }
}

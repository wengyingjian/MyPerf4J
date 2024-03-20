package com.ebaolife.bedrock.sidecar.common.config;

import com.ebaolife.bedrock.sidecar.common.constant.ClassLevelEnum;
import com.ebaolife.bedrock.sidecar.common.util.StrMatchUtils;
import com.ebaolife.bedrock.sidecar.common.util.collections.MapUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by LinShunkang on 2019/05/04
 * <p>
 * MethodLevelMapping=Controller:[*Controller];Api:[*Api*];
 */
public final class LevelMappingFilter {

    private static final Map<String, List<String>> LEVEL_EXPS_MAP = MapUtils.createLinkedHashMap(4);

    static {
        //Initialize the default level mappings
        LEVEL_EXPS_MAP.put(ClassLevelEnum.CONTROLLER.getCode(), Collections.singletonList("*Controller"));
        LEVEL_EXPS_MAP.put(ClassLevelEnum.DB.getCode(), Arrays.asList("*DAO", "*Dao"));
        LEVEL_EXPS_MAP.put(ClassLevelEnum.CACHE.getCode(), Arrays.asList("*Cache", "*CacheImpl"));
        LEVEL_EXPS_MAP.put(ClassLevelEnum.RPC.getCode(), Arrays.asList("*Client", "*ClientImpl"));
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
        return ClassLevelEnum.OTHER.getCode();
    }

    public static void putLevelMapping(String classLevel, List<String> expList) {
        LEVEL_EXPS_MAP.put(classLevel, expList);
    }
}

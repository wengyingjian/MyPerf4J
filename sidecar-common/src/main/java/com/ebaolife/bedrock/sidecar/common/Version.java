package com.ebaolife.bedrock.sidecar.common;

import com.ebaolife.bedrock.sidecar.common.util.Logger;
import com.ebaolife.bedrock.sidecar.common.util.StrUtils;

/**
 * Created by LinShunkang on 2020/08/09
 */
public final class Version {

    private static final String VERSION = getVersion(Version.class, "");

    public static String getVersion() {
        return VERSION;
    }

    public static String getVersion(Class<?> cls, String defaultVersion) {
        try {
            String version = cls.getPackage().getImplementationVersion();
            if (StrUtils.isBlank(version)) {
                version = cls.getPackage().getSpecificationVersion();
            }
            return StrUtils.isBlank(version) ? defaultVersion : version;
        } catch (Throwable e) {
            Logger.error("Version.getVersion(" + cls + ", " + defaultVersion + "): catch Exception ", e);
            return defaultVersion;
        }
    }

    private Version() {
        //empty
    }
}

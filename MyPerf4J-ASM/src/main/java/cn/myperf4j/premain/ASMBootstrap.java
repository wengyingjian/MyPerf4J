package cn.myperf4j.premain;

import cn.myperf4j.premain.aop.ProfilingAspect;
import cn.myperf4j.common.config.Config;
import cn.myperf4j.common.config.ProfilingConfig;
import cn.myperf4j.common.util.Logger;
import cn.myperf4j.core.AbstractBootstrap;
import cn.myperf4j.core.recorder.AbstractRecorderMaintainer;


/**
 * Created by LinShunkang on 2018/4/19
 */
public final class ASMBootstrap extends AbstractBootstrap {

    private static final ASMBootstrap instance = new ASMBootstrap();

    private ASMBootstrap() {
        //empty
    }

    public static ASMBootstrap getInstance() {
        return instance;
    }

    @Override
    public AbstractRecorderMaintainer doInitRecorderMaintainer() {
        Config.RecorderConfig recorderConf = ProfilingConfig.recorderConfig();
        ASMRecorderMaintainer maintainer = ASMRecorderMaintainer.getInstance();
        if (maintainer.initial(recorderConf.accurateMode())) {
            return maintainer;
        }
        return null;
    }

    @Override
    public boolean initOther() {
        return initProfilerAspect();
    }

    private boolean initProfilerAspect() {
        try {
            ProfilingAspect.setRecorderMaintainer((ASMRecorderMaintainer) maintainer);
            ProfilingAspect.setRunning(true);
            return true;
        } catch (Exception e) {
            Logger.error("ASMBootstrap.initProfilerAspect()", e);
        }
        return false;
    }
}

package com.ebaolife.bedrock.sidecar.arthas;

import com.ebaolife.bedrock.sidecar.common.config.ProfilingConfig;
import com.ebaolife.bedrock.sidecar.common.util.Logger;
import com.taobao.arthas.agent.attach.ArthasAgent;

import java.util.HashMap;
import java.util.UUID;

public class ArthasBootstrap {

    private static ArthasBootstrap instance = new ArthasBootstrap();

    public static ArthasBootstrap getInstance() {
        return instance;
    }

    public void initial() {
        String appName = ProfilingConfig.basicConfig().getAppName();
        String agentId = genAgentId(appName);

        HashMap<String, String> configMap = new HashMap<String, String>();
        configMap.put("arthas.appName", appName);
        configMap.put("arthas.agentId", agentId);
        configMap.put("arthas.tunnelServer", "ws://172.21.4.45:7777/ws");
        ArthasAgent.attach(configMap);

        Logger.info("ArthasBootstrap attached,agentId=" + agentId);
    }

    private String genAgentId(String appName) {
        return appName + "_" + UUID.randomUUID();
    }


}

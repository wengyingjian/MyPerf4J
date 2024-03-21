package com.ebaolife.bedrock.sidecar.arthas;

import cn.hutool.http.HttpUtil;
import com.ebaolife.bedrock.sidecar.common.config.ProfilingConfig;
import com.ebaolife.bedrock.sidecar.common.util.Logger;
import com.ebaolife.bedrock.sidecar.common.util.net.IpUtils;
import com.taobao.arthas.agent.attach.ArthasAgent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArthasBootstrap {

    private static final ArthasBootstrap instance = new ArthasBootstrap();

    public static ArthasBootstrap getInstance() {
        return instance;
    }

    public void initial() {
        if (!ProfilingConfig.getArthasConfig().isEnabled()) {
            return;
        }

        String ip = IpUtils.getLocalIp();
        String appName = ProfilingConfig.basicConfig().getAppName();
        String agentId = genAgentId(appName, ip);
        String tunnerServer = ProfilingConfig.getArthasConfig().getTunnelServer();
        String agentIdHolder = ProfilingConfig.getArthasConfig().getAgentIdHolder();

        try {
            HashMap<String, String> configMap = new HashMap<>();
            configMap.put("arthas.appName", appName);
            configMap.put("arthas.agentId", agentId);
            configMap.put("arthas.tunnelServer", tunnerServer);
            ArthasAgent.attach(configMap);

            save(agentIdHolder, appName, ip, agentId);
            Logger.info("ArthasBootstrap init success,appName=" + appName + ",ip=" + ip);
        } catch (Exception e) {
            Logger.error("ArthasBootstrap init failed", e);
        }
    }

    private void save(String agentIdHolder, String appName, String ip, String agentId) {
        Map<String, Object> params = new HashMap<>();
        params.put("appName", appName);
        params.put("ip", ip);
        params.put("agentId", agentId);

        HttpUtil.post(agentIdHolder, params, 3000);
    }

    private String genAgentId(String appName, String ip) {
        return appName + "-" + ip + "-" + UUID.randomUUID();
    }

}

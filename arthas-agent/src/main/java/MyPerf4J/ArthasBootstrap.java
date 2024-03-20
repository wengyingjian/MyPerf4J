package MyPerf4J;

import com.taobao.arthas.agent.attach.ArthasAgent;

import java.util.HashMap;

public class ArthasBootstrap {

    public static void main(String[] args) {

    }

    public static void attach() {

        HashMap<String, String> configMap = new HashMap<String, String>();
        configMap.put("arthas.appName", "demo");
        configMap.put("arthas.agentId", "demoajdklsnajdlksa");
        configMap.put("arthas.tunnelServer", "ws://127.0.0.1:7777/ws");
        ArthasAgent.attach(configMap);
    }
}

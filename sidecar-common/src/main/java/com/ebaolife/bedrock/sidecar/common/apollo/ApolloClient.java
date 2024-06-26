package com.ebaolife.bedrock.sidecar.common.apollo;

import cn.hutool.http.HttpUtil;
import com.ebaolife.bedrock.sidecar.common.util.net.IpUtils;

public class ApolloClient {

    /**
     * 获取apollo配置
     * https://apollo-configservice.jianbaolife.com/configfiles/json/bedrock-sidecar/default/application?ip=10.8.0.42
     */
    public static String fetchApolloConfig(String apolloConfigServiceUrl) {
        String ip = IpUtils.getLocalIp();
        String url = String.format("%s/configfiles/json/bedrock-sidecar/default/application?ip=%s", apolloConfigServiceUrl, ip);
        return HttpUtil.get(url, 10000);
    }
}

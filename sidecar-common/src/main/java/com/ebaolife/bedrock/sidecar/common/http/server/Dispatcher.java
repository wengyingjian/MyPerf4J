package com.ebaolife.bedrock.sidecar.common.http.server;

import com.ebaolife.bedrock.sidecar.common.http.HttpRequest;
import com.ebaolife.bedrock.sidecar.common.http.HttpResponse;

/**
 * Created by LinShunkang on 2020/07/12
 */
public interface Dispatcher {

    HttpResponse dispatch(HttpRequest request);

}

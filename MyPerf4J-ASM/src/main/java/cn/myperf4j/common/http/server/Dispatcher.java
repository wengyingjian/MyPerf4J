package cn.myperf4j.common.http.server;

import cn.myperf4j.common.http.HttpRequest;
import cn.myperf4j.common.http.HttpResponse;

/**
 * Created by LinShunkang on 2020/07/12
 */
public interface Dispatcher {

    HttpResponse dispatch(HttpRequest request);

}

package com.ebaolife.bedrock.sidecar.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ClassLevelEnum {

    /**
     *
     */
    CONTROLLER("Controller"),
    DB("DB"),
    CACHE("Cache"),
    RPC("Rpc"),
    OTHER("Other");

    private final String code;

}
package cn.myperf4j.core;

import com.ebaolife.bedrock.sidecar.common.MethodTag;

import java.lang.reflect.Method;

/**
 * Created by LinShunkang on 2018/5/20
 */
public abstract class AbstractMethodTagMaintainer {

    public abstract int addMethodTag(MethodTag methodTag);

    public abstract int addMethodTag(Method method);

    public abstract MethodTag getMethodTag(int methodId);

}

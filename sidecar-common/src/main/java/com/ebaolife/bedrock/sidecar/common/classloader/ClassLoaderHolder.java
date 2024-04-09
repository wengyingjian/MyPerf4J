package com.ebaolife.bedrock.sidecar.common.classloader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * classloader记录器
 * <p>
 * 因为使用classforname使用的类加载器与spring的类加载器不同，所以会出现classnotfound报错
 * 为了解决这个问题，在类加载时记录对应的类加载器是哪个，这样在需要获取类对象的时候可以找到对应的类加载器来获取
 * <p>
 * 另外考虑节省内存，只记录需要用到的class，需要在static方法中注册，否则获取时直接抛异常
 */
public class ClassLoaderHolder {
    private static final Map<String, ClassLoader> CLASS_LOADER_MAP = new HashMap<>();

    private static final Set<String> CLASS_LOADER_HOLDER_REGISTRY = new HashSet<>();

    static {
        CLASS_LOADER_HOLDER_REGISTRY.add("com/netflix/zuul/context/RequestContext");
    }

    public static void record(ClassLoader loader, String className) {
        if (CLASS_LOADER_HOLDER_REGISTRY.contains(className)) {
            CLASS_LOADER_MAP.put(className, loader);
        }
    }

    public static ClassLoader getClassLoader(String className) {
        if (CLASS_LOADER_HOLDER_REGISTRY.contains(className)) {
            return CLASS_LOADER_MAP.get(className);
        }
        throw new RuntimeException("classLoader not record for class" + className);
    }

}

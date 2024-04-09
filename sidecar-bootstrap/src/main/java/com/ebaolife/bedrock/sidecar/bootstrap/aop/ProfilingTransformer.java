package com.ebaolife.bedrock.sidecar.bootstrap.aop;

import com.ebaolife.bedrock.sidecar.common.config.ProfilingFilter;
import com.ebaolife.bedrock.sidecar.common.util.Logger;
import com.ebaolife.bedrock.sidecar.common.classloader.ClassLoaderHolder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * Created by LinShunkang on 2018/4/24
 */
public class ProfilingTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classFileBuffer) {
        try {
            ClassLoaderHolder.record(loader,className);

            if (ProfilingFilter.isNotNeedInject(className)) {
                return classFileBuffer;
            }

            if (!ProfilingFilter.isNeedInject(className)) {
                return classFileBuffer;
            }

            if (loader != null && ProfilingFilter.isNotNeedInjectClassLoader(loader.getClass().getName())) {
                return classFileBuffer;
            }

            Logger.info("class transforming [" + getSimpleClassLoaderName(loader) + "]: " + className);
            return getBytes(loader, className, classFileBuffer);
        } catch (Throwable e) {
            Logger.error("class transform failed [" + getSimpleClassLoaderName(loader) + "]: " + className, e);
        }
        return classFileBuffer;
    }

    private byte[] getBytes(ClassLoader loader,
                            String className,
                            byte[] classFileBuffer) {
        final int flags = needComputeMaxs(loader) ? ClassWriter.COMPUTE_MAXS : ClassWriter.COMPUTE_FRAMES;

        ClassReader cr = new ClassReader(classFileBuffer);
        ClassWriter cw = new ClassWriter(cr, flags);
        ClassVisitor cv = new ProfilingClassAdapter(cw, className);
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }

    private boolean needComputeMaxs(ClassLoader classLoader) {
        if (classLoader == null) {
            return false;
        }

        String loaderName = getClassLoaderName(classLoader);
        return loaderName.equals("org.apache.catalina.loader.WebappClassLoader")
                || loaderName.equals("org.apache.catalina.loader.ParallelWebappClassLoader")
                || loaderName.equals("org.springframework.boot.loader.LaunchedURLClassLoader")
                || loaderName.startsWith("org.apache.flink.runtime.execution.librarycache.FlinkUserCodeClassLoaders")
                ;
    }

    private String getClassLoaderName(ClassLoader classLoader) {
        if (classLoader == null) {
            return "null";
        }

        return classLoader.getClass().getName();
    }
    private String getSimpleClassLoaderName(ClassLoader classLoader) {
        if (classLoader == null) {
            return "null";
        }

        return classLoader.getClass().getSimpleName();
    }
}

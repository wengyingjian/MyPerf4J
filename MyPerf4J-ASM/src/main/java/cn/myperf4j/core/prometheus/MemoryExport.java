package cn.myperf4j.core.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.Predicate;
import io.prometheus.client.SampleNameFilter;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MemoryExport extends Collector {

    private final OperatingSystemMXBean operatingSystemMXBean;

    public MemoryExport() {
        this(ManagementFactory.getOperatingSystemMXBean());
    }

    public MemoryExport(OperatingSystemMXBean operatingSystemMXBean) {
        this.operatingSystemMXBean = operatingSystemMXBean;
    }


    @Override
    public List<MetricFamilySamples> collect() {
        return this.collect(null);
    }

    @Override
    public List<MetricFamilySamples> collect(Predicate<String> nameFilter) {
        try {
            List<MetricFamilySamples> mfs = new ArrayList<>();
            this.addServerMetrics(mfs, nameFilter == null ? SampleNameFilter.ALLOW_ALL : nameFilter);
            return mfs;
        } catch (Exception var3) {
            throw new RuntimeException(var3);
        }
    }

    void addServerMetrics(List<MetricFamilySamples> sampleFamilies, Predicate<String> nameFilter) throws Exception {
        doAddLong(sampleFamilies, "os_TotalPhysicalMemory", "getTotalPhysicalMemorySize");
        doAddLong(sampleFamilies, "os_getFreePhysicalMemorySize", "getFreePhysicalMemorySize");
        doAddLong(sampleFamilies, "os_getCommittedVirtualMemorySize", "getCommittedVirtualMemorySize");
        doAddLong(sampleFamilies, "os_getSystemCpuLoad", "getSystemCpuLoad");
        doAddLong(sampleFamilies, "os_getProcessCpuLoad", "getProcessCpuLoad");
    }

    private void doAddLong(List<MetricFamilySamples> sampleFamilies, String metric, String method) throws InvocationTargetException, NoSuchMethodException {
        Object value = callLongGetter(method, operatingSystemMXBean);

        long castValue = -1;
        if (value instanceof Long) {
            castValue = (long) value;
        } else if (value instanceof Double) {
            castValue = Double.valueOf((double) value * 100).longValue();
        }

        GaugeMetricFamily threadStateFamily2 = new GaugeMetricFamily(metric, metric, Collections.emptyList());
        threadStateFamily2.addMetric(Collections.emptyList(), castValue);
        sampleFamilies.add(threadStateFamily2);
    }


    static Object callLongGetter(String getterName, Object obj) throws NoSuchMethodException, InvocationTargetException {
        return callLongGetter(obj.getClass().getMethod(getterName), obj);
    }

    static Object callLongGetter(Method method, Object obj) throws InvocationTargetException {
        try {
            return method.invoke(obj);
        } catch (IllegalAccessException var9) {
            Class[] var2 = method.getDeclaringClass().getInterfaces();
            int var3 = var2.length;

            for (int var4 = 0; var4 < var3; ++var4) {
                Class<?> clazz = var2[var4];

                try {
                    Method interfaceMethod = clazz.getMethod(method.getName(), method.getParameterTypes());
                    Object result = callLongGetter(interfaceMethod, obj);
                    if (result != null) {
                        return result;
                    }
                } catch (NoSuchMethodException var8) {
                }
            }

            return null;
        }
    }
}

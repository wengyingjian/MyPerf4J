package cn.myperf4j.core.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.Predicate;
import io.prometheus.client.SampleNameFilter;

import javax.management.JMException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.function.Function;

public class WebContainerExport extends Collector {

    private final ThreadMXBean threadBean;

    public WebContainerExport() {
        this(ManagementFactory.getThreadMXBean());
    }

    public WebContainerExport(ThreadMXBean threadBean) {
        this.threadBean = threadBean;
    }


    @Override
    public List<MetricFamilySamples> collect() {
        return this.collect(null);
    }

    @Override
    public List<MetricFamilySamples> collect(Predicate<String> nameFilter) {
        collectThread();
        try {
            List<MetricFamilySamples> mfs = new ArrayList<>();
            this.addServerMetrics(mfs, nameFilter == null ? SampleNameFilter.ALLOW_ALL : nameFilter);
            return mfs;
        } catch (Exception var3) {
            throw new RuntimeException(var3);
        }
    }

    void addServerMetrics(List<MetricFamilySamples> sampleFamilies, Predicate<String> nameFilter) throws Exception {
        List<ThreadInfo> threadInfoList = collectThread();

        this.add(sampleFamilies, nameFilter, threadInfoList, "tomcat_threads_busy_threads", threadInfos
                -> (double) (int) threadInfos.stream()
                .filter(t -> t.getThreadName().contains("8080") && t.getThreadState() == Thread.State.RUNNABLE)
                .count());
        this.add(sampleFamilies, nameFilter, threadInfoList, "tomcat_threads_current_threads", threadInfos
                -> (double) (int) threadInfos.stream()
                .filter(t -> t.getThreadName().contains("8080"))
                .count());
        this.add(sampleFamilies, nameFilter, threadInfoList, "tomcat_threads_blocked_threads", threadInfos
                -> (double) (int) threadInfos.stream()
                .filter(t -> t.getThreadName().contains("8080") && t.getThreadState() == Thread.State.BLOCKED)
                .count());
    }

    private void add(List<MetricFamilySamples> sampleFamilies, Predicate<String> nameFilter, List<ThreadInfo> threadInfoList, String metricName, Function<List<ThreadInfo>, Double> f) throws JMException {
        if (nameFilter.test(metricName)) {
            GaugeMetricFamily threadStateFamily = new GaugeMetricFamily(metricName, metricName, Collections.singletonList("application"));
            threadStateFamily.addMetric(Arrays.asList("appnamehere"), f.apply(threadInfoList));

            sampleFamilies.add(threadStateFamily);
        }
    }

    private List<ThreadInfo> collectThread() {
        long[] ids = threadBean.getAllThreadIds();
        List<ThreadInfo> threadInfos = Arrays.asList(threadBean.getThreadInfo(ids));

        threadInfos.sort(new Comparator<ThreadInfo>() {
            @Override
            public int compare(ThreadInfo o1, ThreadInfo o2) {
                int state = o1.getThreadState().compareTo(o2.getThreadState());
                if (state == 0) {
                    return o1.getThreadName().compareTo(o2.getThreadName());
                }
                return state;
            }
        });

        threadInfos.forEach(threadInfo -> {
            System.out.println("thread:" + threadInfo.getThreadState().name() + ":" + threadInfo.getThreadName());
        });
        return threadInfos;
    }
}

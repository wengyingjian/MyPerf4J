package MyPerf4J.prometheus;

import cn.myperf4j.core.prometheus.WebContainerExport;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.hotspot.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class RegistryTest {

    public static void main(String[] args) throws InterruptedException {
        register(CollectorRegistry.defaultRegistry);

        while (true) {
            // 创建一个StringWriter来接收指标输出
            Writer writer = new StringWriter();
            try {
                // 将指标输出到StringWriter中
                TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
                // 将StringWriter中的内容打印到控制台
                System.out.println(writer.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Thread.sleep(30000);
        }
    }

    public static void register(CollectorRegistry registry) {
        (new StandardExports()).register(registry);
        (new MemoryPoolsExports()).register(registry);
        (new MemoryAllocationExports()).register(registry);
        (new BufferPoolsExports()).register(registry);
        (new GarbageCollectorExports()).register(registry);
        (new ThreadExports()).register(registry);
        (new ClassLoadingExports()).register(registry);
        (new VersionInfoExports()).register(registry);
        (new WebContainerExport()).register(registry);
    }

}

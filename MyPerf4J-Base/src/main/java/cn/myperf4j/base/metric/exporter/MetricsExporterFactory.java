package cn.myperf4j.base.metric.exporter;

import cn.myperf4j.base.metric.exporter.discard.*;
import cn.myperf4j.base.metric.exporter.log.influxdb.*;
import cn.myperf4j.base.metric.exporter.log.standard.*;

import static cn.myperf4j.base.constant.PropertyValues.Metrics.*;

public final class MetricsExporterFactory {

    private MetricsExporterFactory() {
        //empty
    }

    public static JvmClassMetricsExporter getClassMetricsExporter(String exporter) {
        switch (exporter) {
            case EXPORTER_LOG_STANDARD:
            case EXPORTER_LOG_STDOUT:
                return new StdLogJvmClassMetricsExporter();
            case EXPORTER_LOG_INFLUX_DB:
                return new InfluxLogJvmClassMetricsExporter();
            default:
                return new DiscardJvmClassMetricsExporter();
        }
    }

    public static JvmGcMetricsExporter getGcMetricsExporter(String exporter) {
        switch (exporter) {
            case EXPORTER_LOG_STANDARD:
            case EXPORTER_LOG_STDOUT:
                return new StdLogJvmGcMetricsExporter();
            case EXPORTER_LOG_INFLUX_DB:
                return new InfluxLogJvmGcMetricsExporter();
            default:
                return new DiscardJvmGcMetricsExporter();
        }
    }

    public static JvmMemoryMetricsExporter getMemoryMetricsExporter(String exporter) {
        switch (exporter) {
            case EXPORTER_LOG_STANDARD:
            case EXPORTER_LOG_STDOUT:
                return new StdLogJvmMemoryMetricsExporter();
            case EXPORTER_LOG_INFLUX_DB:
                return new InfluxLogJvmMemoryMetricsExporter();
            default:
                return new DiscardJvmMemoryMetricsExporter();
        }
    }

    public static JvmBufferPoolMetricsExporter getBufferPoolMetricsExporter(String exporter) {
        switch (exporter) {
            case EXPORTER_LOG_STANDARD:
            case EXPORTER_LOG_STDOUT:
                return new StdLogJvmBufferPoolMetricsExporter();
            case EXPORTER_LOG_INFLUX_DB:
                return new InfluxLogJvmBufferPoolMetricsExporter();
            default:
                return new DiscardJvmBufferPoolMetricsExporter();
        }
    }

    public static JvmThreadMetricsExporter getThreadMetricsExporter(String exporter) {
        switch (exporter) {
            case EXPORTER_LOG_STANDARD:
            case EXPORTER_LOG_STDOUT:
                return new StdLogJvmThreadMetricsExporter();
            case EXPORTER_LOG_INFLUX_DB:
                return new InfluxLogJvmThreadMetricsExporter();
            default:
                return new DiscardJvmThreadMetricsExporter();
        }
    }

    public static MethodMetricsExporter getMethodMetricsExporter(String exporter) {
        switch (exporter) {
            case EXPORTER_LOG_INFLUX_DB:
                return new InfluxLogMethodMetricsExporter();
            default:
                return new StdLogMethodMetricsExporter();
        }
    }

    public static JvmFileDescMetricsExporter getFileDescExporter(String exporter) {
        switch (exporter) {
            case EXPORTER_LOG_STANDARD:
            case EXPORTER_LOG_STDOUT:
                return new StdLogJvmFileDescMetricsExporter();
            case EXPORTER_LOG_INFLUX_DB:
                return new InfluxLogJvmFileDescMetricsExporter();
            default:
                return new DiscardJvmFileDescMetricsExporter();
        }
    }

    public static JvmCompilationMetricsExporter getCompilationExporter(String exporter) {
        switch (exporter) {
            case EXPORTER_LOG_STANDARD:
            case EXPORTER_LOG_STDOUT:
                return new StdLogJvmCompilationMetricsExporter();
            case EXPORTER_LOG_INFLUX_DB:
                return new InfluxLogJvmCompilationMetricsExporter();
            default:
                return new DiscardJvmCompilationMetricsExporter();
        }
    }
}

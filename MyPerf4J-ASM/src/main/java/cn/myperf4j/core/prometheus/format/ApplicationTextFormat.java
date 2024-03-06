package cn.myperf4j.core.prometheus.format;

import io.prometheus.client.Collector;
import io.prometheus.client.exporter.common.TextFormat;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;

/**
 * 代码从TextFormat拷贝
 * 在write004、writeOpenMetrics100中，分别增加了writeApplicationLabel004、writeApplicationLabel100方法
 * 实现了在label中新增application的功能
 */
public class ApplicationTextFormat extends TextFormat {

    private static final String LABEL_NAME_APPLICATION = "application";


    private static void writeApplicationLabel100(Writer writer, int size, String applicationName) throws IOException {
        if (size > 0) {
            writer.write(",");
        }
        writer.write(LABEL_NAME_APPLICATION);
        writer.write("=\"");
        writeEscapedLabelValue(writer, applicationName);
        writer.write("\"");
    }


    /**
     * Write out the OpenMetrics text version 1.0.0 of the given MetricFamilySamples.
     *
     * @since 0.10.0
     */
    public static void writeOpenMetrics100(Writer writer, Enumeration<Collector.MetricFamilySamples> mfs, String applicationName) throws IOException {
        while (mfs.hasMoreElements()) {
            Collector.MetricFamilySamples metricFamilySamples = mfs.nextElement();
            String name = metricFamilySamples.name;

            writer.write("# TYPE ");
            writer.write(name);
            writer.write(' ');
            writer.write(omTypeString(metricFamilySamples.type));
            writer.write('\n');

            if (!metricFamilySamples.unit.isEmpty()) {
                writer.write("# UNIT ");
                writer.write(name);
                writer.write(' ');
                writer.write(metricFamilySamples.unit);
                writer.write('\n');
            }

            writer.write("# HELP ");
            writer.write(name);
            writer.write(' ');
            writeEscapedLabelValue(writer, metricFamilySamples.help);
            writer.write('\n');

            for (Collector.MetricFamilySamples.Sample sample : metricFamilySamples.samples) {
                writer.write(sample.name);
                //labelNames start
                writer.write('{');
                for (int i = 0; i < sample.labelNames.size(); ++i) {
                    if (i > 0) {
                        writer.write(",");
                    }
                    writer.write(sample.labelNames.get(i));
                    writer.write("=\"");
                    writeEscapedLabelValue(writer, sample.labelValues.get(i));
                    writer.write("\"");
                }

                //==== add application  label start
                writeApplicationLabel100(writer, sample.labelNames.size(), applicationName);
                //==== add application  label end

                writer.write('}');
                //labelNames end

                writer.write(' ');
                writer.write(Collector.doubleToGoString(sample.value));
                if (sample.timestampMs != null) {
                    writer.write(' ');
                    omWriteTimestamp(writer, sample.timestampMs);
                }
                if (sample.exemplar != null) {
                    writer.write(" # {");
                    for (int i = 0; i < sample.exemplar.getNumberOfLabels(); i++) {
                        if (i > 0) {
                            writer.write(",");
                        }
                        writer.write(sample.exemplar.getLabelName(i));
                        writer.write("=\"");
                        writeEscapedLabelValue(writer, sample.exemplar.getLabelValue(i));
                        writer.write("\"");
                    }
                    writer.write("} ");
                    writer.write(Collector.doubleToGoString(sample.exemplar.getValue()));
                    if (sample.exemplar.getTimestampMs() != null) {
                        writer.write(' ');
                        omWriteTimestamp(writer, sample.exemplar.getTimestampMs());
                    }
                }
                writer.write('\n');
            }
        }
        writer.write("# EOF\n");
    }

    static void omWriteTimestamp(Writer writer, long timestampMs) throws IOException {
        writer.write(Long.toString(timestampMs / 1000L));
        writer.write(".");
        long ms = timestampMs % 1000;
        if (ms < 100) {
            writer.write("0");
        }
        if (ms < 10) {
            writer.write("0");
        }
        writer.write(Long.toString(timestampMs % 1000));
    }

    private static void writeEscapedLabelValue(Writer writer, String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    writer.append("\\\\");
                    break;
                case '\"':
                    writer.append("\\\"");
                    break;
                case '\n':
                    writer.append("\\n");
                    break;
                default:
                    writer.append(c);
            }
        }
    }


    private static String omTypeString(Collector.Type t) {
        switch (t) {
            case GAUGE:
                return "gauge";
            case COUNTER:
                return "counter";
            case SUMMARY:
                return "summary";
            case HISTOGRAM:
                return "histogram";
            case GAUGE_HISTOGRAM:
                return "gaugehistogram";
            case STATE_SET:
                return "stateset";
            case INFO:
                return "info";
            default:
                return "unknown";
        }
    }
}


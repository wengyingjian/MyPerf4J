package com.ebaolife.bedrock.sidecar.metric.core.prometheus.format;

import io.prometheus.client.Collector;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;

/**
 * 代码从TextFormat拷贝
 * 在write004、writeOpenMetrics100中，分别增加了writeApplicationLabel004、writeApplicationLabel100方法
 * 实现了在label中新增application的功能
 */
public class ApplicationText004Format {
    /**
     * Content-type for text version 0.0.4.
     */
    public final static String CONTENT_TYPE_004 = "text/plain; version=0.0.4; charset=utf-8";
    private static final String LABEL_NAME_APPLICATION = "application";

    private static void writeApplicationLabel(Writer writer, String applicationName) throws IOException {
        writer.write(LABEL_NAME_APPLICATION);
        writer.write("=\"");
        writeEscapedLabelValue(writer, applicationName);
        writer.write("\"");
    }

    /**
     * Write out the text version 0.0.4 of the given MetricFamilySamples.
     */
    public static void write004(Writer writer, Enumeration<Collector.MetricFamilySamples> mfs, String appName) throws IOException {
        /* See http://prometheus.io/docs/instrumenting/exposition_formats/
         * for the output format specification. */
        while (mfs.hasMoreElements()) {
            Collector.MetricFamilySamples metricFamilySamples = mfs.nextElement();
            writer.write("# HELP ");
            writer.write(metricFamilySamples.name);
            writer.write(' ');
            writeEscapedHelp(writer, metricFamilySamples.help);
            writer.write('\n');

            writer.write("# TYPE ");
            writer.write(metricFamilySamples.name);
            writer.write(' ');
            writer.write(typeString(metricFamilySamples.type));
            writer.write('\n');

            for (Collector.MetricFamilySamples.Sample sample : metricFamilySamples.samples) {
                writer.write(sample.name);

                //==标签内容开始
                writer.write('{');
                for (int i = 0; i < sample.labelNames.size(); ++i) {
                    writer.write(sample.labelNames.get(i));
                    writer.write("=\"");
                    writeEscapedLabelValue(writer, sample.labelValues.get(i));
                    writer.write("\",");
                }
                //固定的应用名称标签
                writeApplicationLabel(writer, appName);
                writer.write('}');
                //==标签内容结束

                writer.write(' ');
                writer.write(Collector.doubleToGoString(sample.value));
                if (sample.timestampMs != null) {
                    writer.write(' ');
                    writer.write(sample.timestampMs.toString());
                }
                writer.write('\n');
            }
        }
    }

    private static void writeEscapedHelp(Writer writer, String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    writer.append("\\\\");
                    break;
                case '\n':
                    writer.append("\\n");
                    break;
                default:
                    writer.append(c);
            }
        }
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

    private static String typeString(Collector.Type t) {
        switch (t) {
            case GAUGE:
                return "gauge";
            case COUNTER:
                return "counter";
            case SUMMARY:
                return "summary";
            case HISTOGRAM:
                return "histogram";
            default:
                return "untyped";
        }
    }
}


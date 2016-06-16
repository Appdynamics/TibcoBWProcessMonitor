package com.appdynamics.tibco.jmx;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Created by trader on 4/25/16.
 */
public class MetricHandler {

    private AManagedMonitor monitor;
    private String metricPathPrefix;
    private Map<String, String> opsAndMetricNames;

    private static final Logger logger = Logger.getLogger("com.singularity.TibcoJMXMonitor.metrics");

    public MetricHandler(AManagedMonitor monitor, String metricPathPrefix, Map<String, String> opsAndMetricNames) {
        this.monitor = monitor;
        this.metricPathPrefix = metricPathPrefix;
        this.opsAndMetricNames = opsAndMetricNames;
    }

    public void printCollective(String key, String value) {
        printMetric(key, value, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
    }

    public void printIndividual(String key, String value) {
        printMetric(key, value, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
    }

    private void printMetric(String key, String value, String clusterRollup) {
        monitor.getMetricWriter(metricPathPrefix + key, MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, clusterRollup).printMetric(value);

        if (logger.isTraceEnabled()) {
            logger.trace(metricPathPrefix + key + ":" + value + " (" +
                    MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE + "/" + MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE +
                    "/" + clusterRollup + ")");
        }
    }

    public String forKey(String key) {
        return opsAndMetricNames.get(key);
    }
}

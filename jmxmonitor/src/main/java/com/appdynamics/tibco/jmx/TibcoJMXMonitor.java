package com.appdynamics.tibco.jmx;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.*;

import org.apache.log4j.Logger;

/**
 * Created by trader on 1/17/16.
 */
public class TibcoJMXMonitor extends AManagedMonitor {

    protected String metricPathPrefix;
    private String mbeanPattern;
    int retries = -1;
    int port = -1;
    private boolean isInitialized;
    private boolean isDisabled;
    private JMXConnector jmxConnector;
    private MBeanServerConnection mbeanConn = null;

    public static final Logger logger = Logger.getLogger("com.singularity.TibcoJMXMonitor");

    public TibcoJMXMonitor() {
        isInitialized = false;
        isDisabled = false;
        jmxConnector = null;

        logger.info("Created monitor");
    }

    public TaskOutput execute(Map<String, String> arguments, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {

        logger.info("Execute");

        if (isDisabled) {
            return new TaskOutput("Tibco BW JMX extension has been disabled");
        }

        if (!isInitialized) {

            logger.info("Initializing");

            metricPathPrefix = arguments.get("metric-path-prefix");
            if (metricPathPrefix == null) {
                isDisabled = true;
                logger.error("No metric path prefix in configuration!");
                throw new TaskExecutionException(new IllegalArgumentException("Missing required argument method-path-prefix in monitor.xml"));
            } else {
                logger.info("metricPathPrefix set to " + metricPathPrefix);
            }

            if (!isDisabled) {
                mbeanPattern = arguments.get("object-name-pattern");
                if (mbeanPattern == null) {
                    isDisabled = true;
                    throw new TaskExecutionException(new IllegalArgumentException("Missing required argument object-name-pattern in monitor.xml"));
                }
            }

            if (!isDisabled) {
                if (port == -1) {
                    String portStr = arguments.get("port");
                    if (portStr == null) {
                        throw new IllegalArgumentException("TibcoJMXMonitor can't start: no value for port in monitor.xml");
                    } else {
                        try {
                            port = Integer.parseInt(arguments.get("port"));
                        } catch (Exception e) {
                            isDisabled = true;
                            throw new IllegalArgumentException("TibcoJMXMonitor can't start: illegal value for port in monitor.xml: " + portStr);
                        }
                    }
                }
            }

            if (!isDisabled) {
                if (retries == -1) {
                    String retriesString = arguments.get("retries");
                    try {
                        retries = Integer.parseInt(retriesString);
                    } catch (Exception e) {
                        isDisabled = true;
                        throw new IllegalArgumentException("TibcoJMXMonitor can't start: illegal value for retries in monitor.xml: " + retriesString);
                    }
                }
            }

            if (!isDisabled) {
                if (!connect()) {
                    retries--;
                    return new TaskOutput("Couldn't connect, will retry");
                } else {
                    logger.info("Successfully connected to JMX port");
                }
            }

            logger.info("Configuration: mbeanPattern=" + mbeanPattern + ", port=" + port + ", retries=" + retries + ", prefix=" + metricPathPrefix);
        }

        TaskOutput result = new TaskOutput("Success");

        if (!execOnce()) {
            result = new TaskOutput("Execution failure, will retry");
        }

        if (retries == 0) {
            isDisabled = true;
        }

        return result;
    }

    private boolean execOnce() {

        boolean result = false;

        if (mbeanConn != null) {
            try {
                ObjectName mbeanName = null;
                Set<ObjectName> names = new TreeSet<ObjectName>(mbeanConn.queryNames(null, null));
                for (ObjectName name : names) {
                    String cName = name.getCanonicalName();
                    logger.trace("Found canonical object name " + cName);
                    if(name.getCanonicalName().contains(mbeanPattern)) {
                        mbeanName = name;
                        logger.debug("Found ObjectName for Tibco");
                    }
                }

                if(mbeanName != null) {

                    doExecInfo(mbeanName);
                    doProcessInfo(mbeanName);

                    result = true;
                } else {
                    logger.error("Failed to find ObjectName for Tibco");
                }

            } catch (Exception e) {
                retries--;
                mbeanConn = null;
                logger.error("Exec error, will retry", e);
            }
        }

        return result;
    }

    protected boolean connect() {

        boolean result = true;

        connectByRMI(port);
        if (mbeanConn == null) {
            retries--;
            result = false;
        } else {
            logger.info("Successfully connected to JMX");
        }

        return result;
    }

    protected void setMbeanConnection(MBeanServerConnection mbeanConn) {
        this.mbeanConn = mbeanConn;
    }

    private void doExecInfo(ObjectName mbeanName) throws Exception {
        CompositeData obj = (CompositeData) mbeanConn.invoke(mbeanName, "GetExecInfo", new Object[] {}, new String[] {});
        Object status = obj.get("Status");
        if (status != null && !status.equals("FAILURE")) {
            printCollectiveMetric("ExecInfo|Status", "1");
        } else {
            printCollectiveMetric("ExecInfo|Status", "0");
        }

        printCollectiveMetric("ExecInfo|Uptime", obj.get("Uptime").toString());
        printCollectiveMetric("ExecInfo|Threads", obj.get("Threads").toString());
    }

    private void doProcessInfo(ObjectName mbeanName) throws Exception {
        // No-args should give us info for all processes...
        Object[] params = { null, null, null, null, null };
        // ...but of course we still have to give the reflection info for the args :-(
        String[] types = { Long.class.getName(), String.class.getName(), Integer.class.getName(), Integer.class.getName(), String.class.getName() };

        TabularData dataObj = (TabularData) mbeanConn.invoke(mbeanName, "GetProcesses", params, types);
        ProcessInfoAggregator agg = new ProcessInfoAggregator();
        if (dataObj != null) {
            Collection<?> vals = dataObj.values();
            if (vals != null) {

                if (logger.isDebugEnabled()) {
                    logger.debug("Process data object count: " + vals.size());
                }
                // Aggregate on process names
                for (Object val : vals) {
                    CompositeData cd = (CompositeData) val;
                    ProcessInfo info = new ProcessInfo(cd);
                    agg.handle(info);
                }
            }
        }

        logger.info("Aggregated " + agg.getAllProcessStats().size() + " trees");
        printProcessMetricTree(agg);
    }

    protected void printCollectiveMetric(String key, String value) {
        getMetricWriter(metricPathPrefix + key, MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE).printMetric(value);
    }

    protected void printIndividualMetric(String key, long duration, int observationCount) {
        if (logger.isDebugEnabled()) {
            logger.debug("Metric print: key=" + key + ", duration=" + duration + ", count=" + observationCount);
        }
        if (observationCount > 0) {
            // Only print the duration if there is a positive count
            getMetricWriter(metricPathPrefix + key + "|duration", MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL).printMetric(String.valueOf(duration));
        }

        // Always print the count, even if 0
        getMetricWriter(metricPathPrefix + key + "|activeCount", MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL).printMetric(String.valueOf(observationCount));
    }

    protected void printSubProcessInfo(String key, Map<String, int[]> subProcessInfo) {
        for (Map.Entry<String, int[]> entry : subProcessInfo.entrySet()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Metric print for sub-process: key=" + entry.getKey());
            }
            
            getMetricWriter(metricPathPrefix + key + "|SubProcessInvocations|" + entry.getKey() + "|count", MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                    MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL).printMetric(String.valueOf(entry.getValue()[0]));
        }
    }

    protected void printProcessMetricTree(ProcessInfoAggregator agg) {
        for (ProcessStats tree : agg.getAllProcessStats().values()) {
            printProcessMetricTree(tree.getProcessName(), tree);
        }
    }

    protected void printProcessMetricTree(String keyRoot, ProcessStats tree) {
        int count = tree.getCount();
        if (logger.isDebugEnabled()) {
            logger.debug("Printing tree, metric prefix is " + keyRoot + ", count is " + count);
        }
        printIndividualMetric(keyRoot, tree.getAverageDuration(), tree.getCount());
        printSubProcessInfo(keyRoot, tree.getSubProcessInvocations());
    }

    private void connectByRMI(int port) {
        String host = "localhost";
        String url = "service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi";

        try {
            JMXServiceURL serviceUrl = new JMXServiceURL(url);
            jmxConnector = JMXConnectorFactory.connect(serviceUrl, null);
            MBeanServerConnection newConn = jmxConnector.getMBeanServerConnection();
            setMbeanConnection(newConn);
            // now query to get the beans or whatever
        } catch (Exception e) {
            logger.error("Exception while connecting to JMX", e);
            isDisabled = true;
        }
    }
}

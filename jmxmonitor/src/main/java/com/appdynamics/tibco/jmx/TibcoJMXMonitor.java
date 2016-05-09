package com.appdynamics.tibco.jmx;

import com.appdynamics.tibco.jmx.currentinfo.ProcessExecutionTracker;
import com.appdynamics.tibco.jmx.execinfo.BWExecutionTracker;
import com.appdynamics.tibco.jmx.processinfo.ProcessAndActivityTracker;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

import javax.management.*;
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

    private MetricHandler metrics;
    private BWExecutionTracker execTracker;
    private ProcessExecutionTracker oldProcessTracker;
    private ProcessAndActivityTracker newProcessTracker;

    // Metric override capability
    private static final Map<String, String> opsAndMetricNames = new HashMap<String, String>();

    // Keys
    public static final String INVOCATIONS = "Metric-INVOCATIONS";
    public static final String ART = "Metric-ART";
    public static final String CONCURRENT = "Metric-CONCURRENT";
    public static final String TOTAL_ACTIVE = "Metric-ACTIVE-TOTAL";
    public static final String PROCESS_ACTIVE = "Metric-ACTIVE-PROCESS";
    public static final String RUNNING = "Metric-RUNNING";
    public static final String CREATED = "Metric-CREATED";
    public static final String QUEUED = "Metric-QUEUED";
    public static final String SUSPENDED = "Metric-SUSPENDED";
    public static final String SWAPPED = "Metric-SWAPPED";
    public static final String ABORTED = "Metric-ABORTED";
    public static final String COMPLETED = "Metric-COMPLETED";
    public static final String ERRORS = "Metric-ERRORS";

    static {
        opsAndMetricNames.put(INVOCATIONS, "Invocations");
        opsAndMetricNames.put(ART, "Average Response Time (ms)");
        opsAndMetricNames.put(CONCURRENT, "Concurrent Invocations");
        opsAndMetricNames.put(TOTAL_ACTIVE, "Total Active Process Count");
        opsAndMetricNames.put(PROCESS_ACTIVE, "Active Count");
        opsAndMetricNames.put(RUNNING, "Total Running Processes");
        opsAndMetricNames.put(CREATED, "Total Created");
        opsAndMetricNames.put(QUEUED, "Percent Queued");
        opsAndMetricNames.put(SUSPENDED, "Percent Suspended");
        opsAndMetricNames.put(SWAPPED, "Percent Swapped");
        opsAndMetricNames.put(ABORTED, "Percent Aborted");
        opsAndMetricNames.put(COMPLETED, "Percent Completed");
        opsAndMetricNames.put(ERRORS, "Errors");
    }

    private static final Object[] NO_ARGS = {};
    private static final String[] EMPTY_SIG = {};

    /*
     * Logger.  Logging settings for debug or trace logging need to be enabled with this configuration added
     * to conf/logging/log4j.xml in the machineagent folder:
     *
     *     <logger name="com.singularity.TibcoJMXMonitor" additivity="false">
     *         <level value="info"/>         <---------- change "info" to either "debug" or "trace"
     *         <appender-ref ref="FileAppender"/>
     *     </logger>
     */
    private static final Logger logger = Logger.getLogger("com.singularity.TibcoJMXMonitor");
    private static final Logger mbeanLogger = Logger.getLogger("com.singularity.TibcoJMXMonitor.mbeans");

    public TibcoJMXMonitor() {
        isInitialized = false;
        isDisabled = false;
        jmxConnector = null;

        logger.info("Created monitor");
    }

    public TaskOutput execute(Map<String, String> arguments, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {

        logger.debug("Execute");

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

            // Metric name overrides
            for (String key : arguments.keySet()) {
                if (key.startsWith("Metric-")) {
                    opsAndMetricNames.put(key, arguments.get(key));
                }
            }

            metrics = new MetricHandler(this, metricPathPrefix, opsAndMetricNames);
            execTracker = new BWExecutionTracker(metrics);
            oldProcessTracker = new ProcessExecutionTracker(metrics);
            newProcessTracker = new ProcessAndActivityTracker(metrics);


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

        logger.trace("execOnce begin");

        if (mbeanConn != null) {
            try {
                Set<ObjectName> names = new TreeSet<ObjectName>(mbeanConn.queryNames(null, null));
                for (ObjectName name : names) {
                    String cName = name.getCanonicalName();
                    if (cName.contains(mbeanPattern)) {
                        logger.debug("Found ObjectName for Tibco: " + cName);
                        execTracker.getExecutionInfo(mbeanConn, name);
                        newProcessTracker.getProcessStats(mbeanConn, name);
                        result = true;
                    } else if (logger.isTraceEnabled()) {
                        logger.trace("Skipping object name " + cName);
                    }
                }

            } catch (Exception e) {
                retries--;
                mbeanConn = null;
                logger.error("Exec error, will retry", e);
            }
        }

        logger.trace("execOnce end");

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

    private Object[] getProcessExceptions(ObjectName mbeanName) {
        // NOT IMPLEMENTED: is is unclear what the GetProcessesExceptions JMX API uses for its time window.
        // The docs don't say whether all exceptions from the last restart are returned, or exceptions for
        // active process instances, or whatever.  If the answer is all process instances since the last restart,
        // then the size of the return value would be monitonically increasing until a restart is done (not
        // a good thing!).  For now, skip this data.
        return null;
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

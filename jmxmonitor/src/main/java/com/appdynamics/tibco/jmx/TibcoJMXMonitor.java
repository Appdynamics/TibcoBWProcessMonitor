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
    int[] portList = null;
    int[] portRetries = null;
    private boolean isInitialized;
    private boolean isDisabled;
    private boolean activityTrackingEnabled;
    private JMXConnector jmxConnector;

    private MetricHandler metrics;
    private BWExecutionTracker execTracker;
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
            logger.debug("Tibco BW JMX extension has been disabled");
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

            String activityString = arguments.get("enable-activity-tracking");
            if (activityString == null) {
                activityTrackingEnabled = true;
            } else {
                activityTrackingEnabled = Boolean.parseBoolean(activityString);
            }
            logger.info("Activity tracking enabled? " + activityTrackingEnabled);

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
            newProcessTracker = new ProcessAndActivityTracker(metrics, activityTrackingEnabled);

            if (!isDisabled) {
                getPortList(arguments.get("port"), arguments.get("retries"));
            }

            isInitialized = true;
            logger.info("Configuration: mbeanPattern=" + mbeanPattern + ", port=" + arguments.get("port") +
                    ", retries=" + arguments.get("retries") + ", prefix=" + metricPathPrefix);
        }

        TaskOutput result = new TaskOutput("Success");

        if (!isDisabled) {
            boolean oneSuccess = false;
            for (int i = 0; i < portList.length; ++i) {
                if (portRetries[i] > 0) {
                    oneSuccess = true;
                    logger.debug("Polling on port " + portList[i]);
                    MBeanServerConnection conn = connect(portList[i]);
                    if (conn == null) {
                        portRetries[i]--;
                        logger.error("Failed to connect to port " + portList[i] + ", will retry " + portRetries[i] + " more times.");
                        result = new TaskOutput("Couldn't connect, will retry");
                    } else {
                        logger.debug("Successfully connected to JMX port");
                        if (!execOnce(conn)) {
                            portRetries[i]--;
                            result = new TaskOutput("Execution failure, will retry " + portRetries[i] + " more times");
                        }
                    }
                }
            }

            if (!oneSuccess) {
                isDisabled = true;
            }
        }

        return result;
    }

    private boolean execOnce(MBeanServerConnection mbeanConn) {

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
                logger.error("Exec error, will retry", e);
            }
        }

        logger.trace("execOnce end");

        return result;
    }

    protected MBeanServerConnection connect(int port) {
        return connectByRMI(port);
    }

    protected void setPortList(int[] portList) {
        this.portList = portList;
    }

    protected void setRetriesList(int[] retriesList) {
        this.portRetries = retriesList;
    }

    protected void getPortList(String portListArg, String retriesArg) {

        if (portList == null) {
            if (portListArg == null) {
                isDisabled = true;
                throw new IllegalArgumentException("TibcoJMXMonitor can't start: no value for port in monitor.xml");
            } else {

                int retries = -1;
                try {
                    retries = Integer.parseInt(retriesArg);

                    List<Integer> allPorts = new ArrayList<Integer>();
                    StringTokenizer tok = new StringTokenizer(portListArg, ",");
                    while (tok.hasMoreTokens()) {
                        String onePortStr = tok.nextToken();
                        try {
                            int port = Integer.parseInt(onePortStr);
                            if (port > 0) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Will poll JMX on port " + port);
                                }
                                allPorts.add(port);
                            } else {
                                logger.error("Illegal negative value for port: " + onePortStr);
                            }
                        } catch (Exception e) {
                            logger.error("Illegal non-integer port value: " + onePortStr);
                        }
                    }

                    if (allPorts.size() > 0) {
                        int[] ports = new int[allPorts.size()];
                        for (int i = 0; i < allPorts.size(); ++i) {
                            ports[i] = allPorts.get(i).intValue();
                        }

                        setPortList(ports);

                        int[] retriesList = new int[ports.length];
                        for (int i = 0; i < retriesList.length; ++i) {
                            retriesList[i] = retries;
                        }

                        setRetriesList(retriesList);

                    } else {
                        isDisabled = true;
                        logger.error("No valid ports are configured, Tibco monitoring will be disabled.  To fix, correct the value for port in monitor.xml and restart the machine agent");
                    }
                } catch (Exception e) {
                    isDisabled = true;
                    throw new IllegalArgumentException("TibcoJMXMonitor can't start: illegal value for retries in monitor.xml: " + retriesArg);
                }
            }
        }
    }

    private Object[] getProcessExceptions(ObjectName mbeanName) {
        // NOT IMPLEMENTED: is is unclear what the GetProcessesExceptions JMX API uses for its time window.
        // The docs don't say whether all exceptions from the last restart are returned, or exceptions for
        // active process instances, or whatever.  If the answer is all process instances since the last restart,
        // then the size of the return value would be monitonically increasing until a restart is done (not
        // a good thing!).  For now, skip this data.
        return null;
    }

    private MBeanServerConnection connectByRMI(int port) {
        String host = "localhost";
        String url = "service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi";
        MBeanServerConnection result = null;

        try {
            JMXServiceURL serviceUrl = new JMXServiceURL(url);
            jmxConnector = JMXConnectorFactory.connect(serviceUrl, null);
            result = jmxConnector.getMBeanServerConnection();
        } catch (Exception e) {
            logger.error("Exception while connecting to JMX", e);
        }

        return result;
    }
}

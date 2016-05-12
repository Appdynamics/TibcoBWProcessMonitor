package com.appdynamics.tibco.jmx.processinfo;

import com.appdynamics.tibco.jmx.MetricHandler;
import com.appdynamics.tibco.jmx.TibcoJMXMonitor;
import org.apache.log4j.Logger;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by trader on 4/25/16.
 */
public class ProcessAndActivityTracker {

    private MetricHandler metricHandler;
    private Map<String, int[]> activeProcessInfo;
    private boolean trackActivities;

    private static final Logger logger = Logger.getLogger("com.singularity.TibcoJMXMonitor.newProc");

    private static final String[] STRING_SIG = { String.class.getName() };
    private static final Object[] GET_PROCS_ARGS = { null, null, null, null, null };
    private static final String[] GET_PROCS_SIG = { Long.class.getName(),
            String.class.getName(), Integer.class.getName(), Integer.class.getName(), String.class.getName() };

    public ProcessAndActivityTracker(MetricHandler metricHandler, boolean trackActivities) {
        this.metricHandler = metricHandler;
        activeProcessInfo = new HashMap<String, int[]>();
        this.trackActivities = trackActivities;
    }

    public void getProcessStats(MBeanServerConnection mbeanConn, ObjectName mbeanName) {
        if (logger.isTraceEnabled()) {
            // Log all possible operations
            try {
                traceAttributes(logger, mbeanConn, mbeanName);
                traceOperations(logger, mbeanConn, mbeanName);
            } catch (Exception e) {
                logger.error("Error in getting trace log mbean info", e);
            }
        }

        try {
            Map<String, int[]> activeProcs = getActiveProcessCount(mbeanConn, mbeanName);
            getProcessDefinitions(mbeanConn, mbeanName, activeProcs);
        } catch (Exception e) {
            logger.error("Error in getting process statistics", e);
        }
    }

    private String[] getProcessDefinitions(MBeanServerConnection mbeanConn, ObjectName mbeanName, Map<String, int[]> activeProcs) {
        String[] defs = null;

        try {
            TabularData td = (TabularData) mbeanConn.invoke(mbeanName, "GetProcessDefinitions", null, null);
            if (td != null) {
                logger.debug(" --> processDefinitions, type=" + td.getTabularType().toString());
                for (Object value : td.values()) {
                    if (value instanceof CompositeData) {
                        CompositeData cd = (CompositeData) value;
                        String name = (String) cd.get("Name");
                        logger.debug("    ---> Name: " + name);
                        logger.debug("    ---> Created: " + cd.get("Created"));
                        logger.debug("    ---> Suspended: " + cd.get("Suspended"));
                        logger.debug("    ---> Swapped: " + cd.get("Swapped"));
                        logger.debug("    ---> Queued: " + cd.get("Queued"));
                        logger.debug("    ---> Aborted: " + cd.get("Aborted"));
                        logger.debug("    ---> Completed: " + cd.get("Completed"));
                        logger.debug("    ---> AvgEx: " + cd.get("AverageExecution"));
                        logger.debug("    ---> AvgEla: " + cd.get("AverageElapsed"));
                        logger.debug("    ---> Comp: " + cd.get("Completed"));

                        String prefix = "Processes|" + name + "|";

                        // Process-level metrics: Active
                        int[] activeProcInfo = activeProcs.get(name);
                        if (activeProcInfo == null) {
                            activeProcInfo = new int[1];
                            activeProcInfo[0] = 0;
                            activeProcs.put(name, activeProcInfo);
                        }

                        int activeForProcess = activeProcs.get(name)[0];
                        metricHandler.printIndividual(prefix + metricHandler.forKey(TibcoJMXMonitor.TOTAL_ACTIVE),
                                String.valueOf(activeForProcess));

                        // Process-level metrics: Total Created
                        long created = ((Long) cd.get("Created")).longValue();
                        metricHandler.printIndividual(prefix + metricHandler.forKey(TibcoJMXMonitor.CREATED), String.valueOf(created));
                        long pctSuspended = 0;
                        long pctSwapped = 0;
                        long pctQueued = 0;
                        long pctAborted = 0;
                        long pctCompleted = 0;

                        if (created > 0) {
                            long totalSuspended = ((Long) cd.get("Suspended")).longValue();
                            pctSuspended = (long) (((double) totalSuspended / (double) created) * 100);
                            long totalSwapped = ((Long) cd.get("Swapped")).longValue();
                            pctSwapped = (long) (((double) totalSwapped / (double) created) * 100);
                            long totalQueued = ((Long) cd.get("Queued")).longValue();
                            pctQueued = (long) (((double) totalQueued / (double) created) * 100);
                            long totalAborted = ((Long) cd.get("Aborted")).longValue();
                            pctAborted = (long) (((double) totalAborted / (double) created) * 100);
                            long totalCompleted = ((Long) cd.get("Completed")).longValue();
                            pctCompleted = (long) (((double) totalCompleted / (double) created) * 100);
                        }

                        metricHandler.printIndividual(prefix + metricHandler.forKey(TibcoJMXMonitor.SUSPENDED),
                                String.valueOf(pctSuspended));
                        metricHandler.printIndividual(prefix + metricHandler.forKey(TibcoJMXMonitor.SWAPPED),
                                String.valueOf(pctSwapped));
                        metricHandler.printIndividual(prefix + metricHandler.forKey(TibcoJMXMonitor.QUEUED),
                                String.valueOf(pctQueued));
                        metricHandler.printIndividual(prefix + metricHandler.forKey(TibcoJMXMonitor.ABORTED),
                                String.valueOf(pctAborted));
                        metricHandler.printIndividual(prefix + metricHandler.forKey(TibcoJMXMonitor.COMPLETED),
                                String.valueOf(pctCompleted));

                        // Process-level metrics: Average Elapsed Time
                        long avgElapsed = ((Long) cd.get("AverageElapsed")).longValue();
                        metricHandler.printIndividual(prefix + "Average Elapsed Time (ms)",
                                String.valueOf(avgElapsed));

                        // Process-level metrics: Average Elapsed Time
                        long currElapsed = ((Long) cd.get("MostRecentElapsedTime")).longValue();
                        metricHandler.printIndividual(prefix + "Current Elapsed Time (ms)",
                                String.valueOf(currElapsed));

                        // Process-level metrics: Average Elapsed Time
                        long avgExec = ((Long) cd.get("AverageExecution")).longValue();
                        metricHandler.printIndividual(prefix + "Average Execution Time (ms)",
                                String.valueOf(avgExec));

                        // Process-level metrics: Average Elapsed Time
                        long currExec = ((Long) cd.get("MostRecentExecutionTime")).longValue();
                        metricHandler.printIndividual(prefix + "Current Execution Time (ms)",
                                String.valueOf(currExec));

                        if (trackActivities) {
                            getActivities(mbeanConn, mbeanName, name, prefix);
                        }

                    } else {
                        logger.debug("    ---> Value, type=" + value.getClass().getName() + ": " + value);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in getProcessDefinitions", e);
        }

        return defs;
    }

    private void getActivities(MBeanServerConnection mbeanConn, ObjectName mbeanName, String processDefName, String metricPathPrefix) {

        try {
            TabularData td = (TabularData) mbeanConn.invoke(mbeanName, "GetActivities", new Object[] { processDefName }, STRING_SIG);
            if (td != null) {
                logger.debug(" --> activities for process def " + processDefName + ", type=" + td.getTabularType().toString());
                for (Object value : td.values()) {
                    if (value instanceof CompositeData) {
                        CompositeData cd = (CompositeData) value;
                        logger.debug("    ---> CompValue: " + cd);

                        String prefix = metricPathPrefix + "Activities|" + cd.get("Name") + "|";

                        // Process-level metrics: Average Elapsed Time
                        metricHandler.printIndividual(prefix + "Current Elapsed Time (ms)",
                                String.valueOf(cd.get("MostRecentElapsedTime")));

                        // Process-level metrics: Average Elapsed Time
                        metricHandler.printIndividual(prefix + "Current Execution Time (ms)",
                                String.valueOf(cd.get("MostRecentExecutionTime")));

                        // Process-level metrics: Average Elapsed Time
                        metricHandler.printIndividual(prefix + "Count",
                                String.valueOf(cd.get("ExecutionCount")));

                    } else {
                        logger.debug("    ---> Value, type=" + value.getClass().getName() + ": " + value);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in getActivities", e);
        }
    }

    private Map<String, int[]> getActiveProcessCount(MBeanServerConnection mbeanConn, ObjectName mbeanName) {

        logger.debug("getActiveProcessCount");

        Map<String, int[]> processInfo = null;
        int totalCount = 0;
        processInfo = getActiveProcessContainer();

        try {
            TabularData td = (TabularData) mbeanConn.invoke(mbeanName, "GetProcesses", GET_PROCS_ARGS , GET_PROCS_SIG);
            if (td != null) {
                logger.debug(" --> activeProcesses, type=" + td.getTabularType().toString());
                for (Object value : td.values()) {
                    if (value instanceof CompositeData) {
                        CompositeData cd = (CompositeData) value;
                        String name = (String) cd.get("Name");
                        logger.debug("    ---> Name: " + cd.get("Name"));
                        logger.debug("    ---> Id: " + cd.get("Id"));

                        int[] existingCount = processInfo.get(name);
                        if (existingCount == null) {
                            existingCount = new int[1];
                            processInfo.put(name, existingCount);
                        }

                        existingCount[0]++;
                        totalCount++;

                    } else {
                        logger.debug("    ---> Value, type=" + value.getClass().getName() + ": " + value);
                    }
                }
            } else {
                logger.debug("Null return from GetProcesses");
            }

            metricHandler.printIndividual("Processes" + "|" + metricHandler.forKey(TibcoJMXMonitor.TOTAL_ACTIVE),
                    String.valueOf(totalCount));

        } catch (Exception e) {
            logger.error("Error in getActiveProcessCount", e);
        }

        return processInfo;
    }

    private void traceAttributes(Logger localLogger, MBeanServerConnection mbeanConn, ObjectName mbeanName) throws Exception {
        // debugging-level trace logger to spew out all available attributes into TRACE-level logging
        // INVARIANT: trace logging is known to be enabled whenever this method is called
        MBeanInfo info = mbeanConn.getMBeanInfo(mbeanName);
        if (info != null) {
            MBeanAttributeInfo[] attributes = info.getAttributes();
            if (attributes == null || attributes.length == 0) {
                localLogger.trace("No attributes");
            } else {
                for (MBeanAttributeInfo inf : attributes) {
                    localLogger.trace("      -- Attribute name: " + inf.getName());
                    localLogger.trace("                   description: " + inf.getDescription());
                    localLogger.trace("                   type: " + inf.getType());
                    localLogger.trace("                   is getter: " + inf.isIs());
                }
            }
        }
    }

    private void traceOperations(Logger localLogger, MBeanServerConnection mbeanConn, ObjectName mbeanName) throws Exception {
        // debugging-level trace logger to spew out all available operations into TRACE-level logging
        // INVARIANT: trace logging is known to be enabled whenever this method is called
        MBeanInfo info = mbeanConn.getMBeanInfo(mbeanName);
        if (info != null) {
            MBeanOperationInfo[] operations = info.getOperations();
            if (operations == null || operations.length == 0) {
                localLogger.trace("No operations");
            } else {
                for (MBeanOperationInfo inf : operations) {
                    localLogger.trace("      -- Operation name: " + inf.getName() + ", description: " + inf.getDescription());
                    MBeanParameterInfo[] paramInfo = inf.getSignature();
                    if (paramInfo == null || paramInfo.length == 0) {
                        localLogger.trace("           params: none:");
                    } else {
                        localLogger.trace("           params:");
                        for (int i = 0; i < paramInfo.length; ++i) {
                            localLogger.trace("               param " + i + ": name=" + paramInfo[i].getName() +
                                    ", type=" + paramInfo[i].getType() + ", desc=" + paramInfo[i].getDescription());
                        }
                    }
                    localLogger.trace("           return: " + inf.getReturnType());
                    localLogger.trace("           impact: " + inf.getImpact());
                }
            }
        }
    }

    private Map<String, int[]> getActiveProcessContainer() {
        resetActiveProcessStats(activeProcessInfo);
        return activeProcessInfo;
    }

    private void resetActiveProcessStats(Map<String, int[]> procInfo) {
        for (int[] count : procInfo.values()) {
            count[0] = 0;
        }
    }

}

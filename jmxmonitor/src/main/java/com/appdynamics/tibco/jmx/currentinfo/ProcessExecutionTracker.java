package com.appdynamics.tibco.jmx.currentinfo;

import com.appdynamics.tibco.jmx.MetricHandler;
import com.appdynamics.tibco.jmx.ProcessStats;
import com.appdynamics.tibco.jmx.TibcoJMXMonitor;
import org.apache.log4j.Logger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.util.Collection;
import java.util.Map;

/**
 * Created by trader on 4/25/16.
 */
public class ProcessExecutionTracker {

    private MetricHandler metricHandler;

    private static final Logger logger = Logger.getLogger("com.singularity.TibcoJMXMonitor.oldProc");

    public ProcessExecutionTracker(MetricHandler metricHandler) {
        this.metricHandler = metricHandler;
    }

    public void getProcessActivitySnapshot(MBeanServerConnection mbeanConn, ObjectName mbeanName) {

        try {

            // No-args should give us info for all processes...
            Object[] params = {null, null, null, null, null};
            // ...but of course we still have to give the reflection info for the args :-(
            String[] types = {Long.class.getName(), String.class.getName(), Integer.class.getName(), Integer.class.getName(), String.class.getName()};
            TabularData dataObj = (TabularData) mbeanConn.invoke(mbeanName, "GetProcesses", params, types);

            Object[] exParams = {new Long(0)};
            String[] exTypes = {Long.class.getName()};
            TabularData errorsObj = (TabularData) mbeanConn.invoke(mbeanName, "GetProcessesExceptions", exParams, exTypes);

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

                        if (logger.isTraceEnabled()) {
                            logger.trace("Processing info for mainProc " + cd.get("MainProcessName") +
                                    ", proc " + cd.get("Name") + ", subProc " + cd.get("SubProcessName") +
                                    ", id=" + cd.get("Id") + ", status=" + cd.get("Status"));
                        }

                        ProcessInfo info = new ProcessInfo(cd);
                        agg.handleProcessInfo(info);
                    }
                }
            }

            if (errorsObj != null) {
                Collection<?> vals = errorsObj.values();
                if (vals != null) {

                    if (logger.isDebugEnabled()) {
                        logger.debug("Process error object count: " + vals.size());
                    }

                    for (Object val : vals) {
                        CompositeData cd = (CompositeData) val;
                        agg.handleErrorInfo(cd);
                    }
                }
            }

            logger.info("Aggregated " + agg.getAllProcessStats().size() + " trees");
            printProcessMetricTree(agg);

        } catch (Exception e) {

            logger.error("Error in getProcessActivitySnapshot", e);

        }
    }

    private void printProcessMetricTree(ProcessInfoAggregator agg) {
        for (ProcessStats tree : agg.getAllProcessStats().values()) {
            printProcessMetricTree(tree.getProcessName(), tree);
        }
    }

    private void printProcessMetricTree(String keyRoot, ProcessStats tree) {
        int count = tree.getCount();
        printProcessInfo(keyRoot, tree.getAverageDuration(), tree.getCount(), tree.getErrorCount());
        printSubProcessInfo(keyRoot, tree.getSubProcessInvocations());
    }

    private void printProcessInfo(String key, long duration, int observationCount, int errorCount) {
        if (observationCount > 0) {
            // Only print the duration if there is a positive count
            metricHandler.printIndividual(key + "|" + metricHandler.forKey(TibcoJMXMonitor.ART), String.valueOf(duration));
        }

        // Always print the count, even if 0
        metricHandler.printIndividual(key + "|" + metricHandler.forKey(TibcoJMXMonitor.CONCURRENT), String.valueOf(observationCount));

        // Always print the error count, even if 0
        metricHandler.printIndividual(key + "|" + metricHandler.forKey(TibcoJMXMonitor.ERRORS), String.valueOf(errorCount));
    }

    private void printSubProcessInfo(String key, Map<String, int[]> subProcessInfo) {
        for (Map.Entry<String, int[]> entry : subProcessInfo.entrySet()) {
            metricHandler.printIndividual(key + "|SubProcessInvocations|" + entry.getKey() + "|" + metricHandler.forKey(TibcoJMXMonitor.CONCURRENT),
                    String.valueOf(entry.getValue()[0]));
        }
    }
}

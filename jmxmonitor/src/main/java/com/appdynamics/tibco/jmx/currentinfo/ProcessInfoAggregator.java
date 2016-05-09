package com.appdynamics.tibco.jmx.currentinfo;

import com.appdynamics.tibco.jmx.ProcessStats;
import com.appdynamics.tibco.jmx.currentinfo.ProcessInfo;
import org.apache.log4j.Logger;

import javax.management.openmbean.CompositeData;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by trader on 1/23/16.
 */
public class ProcessInfoAggregator {

    private Map<String, ProcessStats> allProcessStats;
    private Map<Long, String> idToName;

    public static final Logger logger = Logger.getLogger("com.singularity.TibcoJMXMonitor.ProcessInfo");

    public ProcessInfoAggregator() {
        allProcessStats = new HashMap<String, ProcessStats>();
        idToName = new HashMap<Long, String>();
    }

    public Map<String, ProcessStats> getAllProcessStats() {
        return allProcessStats;
    }

    public void handleProcessInfo(ProcessInfo info) {
        String procName = info.getProcessName();
        String mainProcName = info.getMainProcessName();
        String subProcessName = info.getSubProcessName();

        if (logger.isDebugEnabled()) {
            logger.debug("handle: procName=[" + procName + "], mainProcName=[" + mainProcName + ", subProcName=[" + subProcessName + "], duration=" + info.getDuration());
        }

        if (procName == null || procName.isEmpty()) {
            logger.error("handling error, no process name");
            handleError(info);
        } else if (mainProcName == null || mainProcName.isEmpty()) {
            // Ignore the sub process, this case only makes sense if the sub-process name is empty
            logger.error("No main process name, error!");
            handleError(info);
        } else if (mainProcName.equals(procName)) {
            // Shouldn't happen
            logger.error("Invalid data returned, main process name and process name are both " + mainProcName);
        } else if (subProcessName == null || subProcessName.isEmpty()) {
            // The procName here is the job name
            handleNoSubProcess(mainProcName, info.getDuration(), info.getId());
        } else if (procName.equals(subProcessName)) {
            // Error.  A process cannot have itself as a sub-process
            logger.error("Invalid data returned, process name is same as sub-process name: " + procName);
            handleError(info);
        } else if (mainProcName.equals(subProcessName)) {
            // Error.  A main process cannot have itself as a sub-process
            logger.error("Invalid data returned, main process name is same as sub-process name: " + mainProcName);
            handleError(info);
        } else {
            // Main non-null, process name non-null, sub-process name non-null
            handleFullInfo(mainProcName, subProcessName, info.getDuration(), info.getId());
        }
    }

    private void handleNoSubProcess(String mainProcessName, long duration, Long id) {
        if (logger.isDebugEnabled()) {
            logger.debug("handle main, main=" + mainProcessName);
        }
        synchronized (allProcessStats) {
            ProcessStats stats = allProcessStats.get(mainProcessName);
            if (stats == null) {
                stats = new ProcessStats(mainProcessName);
                allProcessStats.put(mainProcessName, stats);
            }

            idToName.put(id, mainProcessName);
            stats.putMetrics(duration);
        }
    }

    private void handleFullInfo(String mainProcessName, String subProcessName, long duration, Long id) {
        if (logger.isDebugEnabled()) {
            logger.debug("handle full, parent=" + mainProcessName + ", subProc=" + subProcessName);
        }
        synchronized (allProcessStats) {
            ProcessStats stats = allProcessStats.get(mainProcessName);
            if (stats == null) {
                stats = new ProcessStats(mainProcessName);
                allProcessStats.put(mainProcessName, stats);
            }

            idToName.put(id, mainProcessName);
            stats.putMetrics(duration, subProcessName);
        }
    }

    private void handleError(ProcessInfo info) {
        System.out.println("ERROR in info obj " + info);
    }

    public void handleErrorInfo(CompositeData cd) {
        String name = idToName.get((Long) cd.get("Id"));
        if (name != null) {
            allProcessStats.get(name).tallyError();
        }
    }
}

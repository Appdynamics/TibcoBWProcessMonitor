package com.appdynamics.tibco.jmx;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by trader on 1/23/16.
 */
public class ProcessInfoAggregator {

    private Map<String, ProcessTree> processTrees;

    public static final Logger logger = Logger.getLogger("com.singularity.TibcoJMXMonitor.ProcessInfo");

    public ProcessInfoAggregator() {
        processTrees = new HashMap<String, ProcessTree>();
    }

    public Map<String, ProcessTree> getProcessTrees() {
        return processTrees;
    }

    public void handle(ProcessInfo info) {
        String procName = info.getProcessName();
        String mainProcName = info.getMainProcessName();
        String subProcessName = info.getSubProcessName();
        if (procName == null || procName.isEmpty()) {
            logger.info("handling error, no process name");
            handleError(info);
        } else if (mainProcName == null || mainProcName.isEmpty()) {
            // Ignore the sub process, this case only makes sense if the sub-process name is empty
            handleStandaloneProcess(info);
        } else if (mainProcName.equals(procName)) {
            if (subProcessName == null || subProcessName.isEmpty()) {
                // handle as a standalone process info object
                handleStandaloneProcess(info);
            } else if (subProcessName.equals(procName)) {
                // all names are equal -- doesn't really make sense.  Handle as standalone
                handleStandaloneProcess(info);
            } else {
                // Does this make sense?  Why have a sub-process name if the information is not
                // relevant to that sub-process?  Just to say that a sub-process exists?
                handleMainAndSub(procName, subProcessName, info.getDuration(), false);
            }
        } else if (subProcessName == null || subProcessName.isEmpty()) {
            // This seems wrong -- a non-null main process name and a different non-null process name, but no
            // subprocess name??  Handle as main and sub-process, but that might not be the right thing.
            handleMainAndSub(mainProcName, procName, info.getDuration(), true);
        } else if (procName.equals(subProcessName)) {
            // Error.  A Process cannot have itself as a sub-process
            handleError(info);
        } else {
            // Main non-null, process name non-null, sub-process name non-null
            handleFullInfo(info);
        }
    }

    private void handleMainAndSub(String mainProcessName, String subProcessName, long duration, boolean metricsBelongToSub) {
        if (logger.isDebugEnabled()) {
            logger.debug("handle main/sub, main=" + mainProcessName + ", sub=" + subProcessName);
        }
        synchronized (processTrees) {
            ProcessTree parentTree = processTrees.get(mainProcessName);
            if (parentTree == null) {
                parentTree = new ProcessTree(mainProcessName);
                processTrees.put(mainProcessName, parentTree);
            }

            ProcessTree childTree = parentTree.getChild(subProcessName);
            if (metricsBelongToSub) {
                childTree.putMetrics(duration);
            } else {
                parentTree.putMetrics(duration);
            }
        }
    }

    private void handleStandaloneProcess(ProcessInfo info) {
        if (logger.isDebugEnabled()) {
            logger.debug("handle proc, name=" + info.getProcessName() + ", main=" + info.getMainProcessName());
        }
        synchronized (processTrees) {
            ProcessTree existing = processTrees.get(info.getProcessName());
            if (existing == null) {
                existing = new ProcessTree(info.getProcessName());
                processTrees.put(info.getProcessName(), existing);
            }

            existing.putMetrics(info.getDuration());
        }
    }

    private void handleFullInfo(ProcessInfo info) {
        if (logger.isDebugEnabled()) {
            logger.debug("handle full, parent=" + info.getMainProcessName() + ", proc=" + info.getProcessName() + ", sub=" + info.getSubProcessName());
        }
        synchronized (processTrees) {
            ProcessTree parent = processTrees.get(info.getMainProcessName());
            if (parent == null) {
                parent = new ProcessTree(info.getMainProcessName());
                processTrees.put(info.getMainProcessName(), parent);
            }

            ProcessTree process = parent.getChild(info.getProcessName());
            process.putMetrics(info.getDuration());

            process.getChild(info.getSubProcessName());
        }
    }

    private void handleError(ProcessInfo info) {
        System.out.println("ERROR in info obj " + info);
    }
}

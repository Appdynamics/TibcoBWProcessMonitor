package com.appdynamics.tibco.jmx;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by trader on 1/23/16.
 */
public class ProcessStats {

    private String processName;
    private long totalDuration;
    private int count;
    private int errors;

    private Map<String, int[]> subProcessInvocations;

    public ProcessStats(String name) {
        processName = name;
        count = 0;
        totalDuration = 0;
        subProcessInvocations = new HashMap<String, int[]>();
        errors = 0;
    }

    public String getProcessName() {
        return processName;
    }

    public void putMetrics(long duration) {
        count++;
        totalDuration += duration;
    }

    public void putMetrics(long duration, String subProcessName) {
        count++;
        totalDuration += duration;

        int[] val = subProcessInvocations.get(subProcessName);
        if (val == null) {
            val = new int[1];
            subProcessInvocations.put(subProcessName, val);
        }

        val[0]++;
    }

    public void tallyError() {
        errors++;
    }

    public long getAverageDuration() {
        if (count > 0) {
            return totalDuration / count;
        } else {
            return -1;
        }
    }

    public Map<String, int[]> getSubProcessInvocations() {
        return subProcessInvocations;
    }

    public int getCount() {
        return count;
    }

    public int getErrorCount() {
        return errors;
    }

    public String toString() {
        return getString("");
    }

    private String getString(String root) {
        StringBuilder sb = new StringBuilder(root);
        sb.append("[ProcessTree: p=");
        sb.append(processName);
        sb.append("; count=");
        sb.append(count);
        sb.append(", avg duration=");
        sb.append(getAverageDuration());
        sb.append("]");

        return sb.toString();
    }
}

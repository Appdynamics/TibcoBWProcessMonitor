package com.appdynamics.tibco.jmx;

import javax.management.openmbean.CompositeData;

/**
 * Created by trader on 1/23/16.
 */
public class ProcessInfo {

    private String mainProcessName;
    private String processName;
    private String subProcessName;
    private long duration;
    private int statusCode;

    public ProcessInfo(String mainProcessName, String processName, String subProcessName) {
        this.mainProcessName = mainProcessName;
        this.processName = processName;
        this.subProcessName = subProcessName;
    }

    public ProcessInfo(CompositeData source) {
        mainProcessName = (String) source.get("MainProcessName");
        processName = (String) source.get("Name");
        subProcessName = (String) source.get("SubProcessName");

        String status = (String) source.get("Status");
        if (status == null) {
            statusCode = 0;
        } else if (status.equals("SUCCESS")) {
            statusCode = 1;
        } else if (status.equals("FAILURE")) {
            statusCode = 2;
        }

        Long durationObj = (Long) source.get("Duration");
        if (durationObj != null) {
            duration = durationObj.longValue();
        } else {
            duration = 0;
        }
    }

    public String getMainProcessName() {
        return mainProcessName;
    }

    public String getProcessName() {
        return processName;
    }

    public String getSubProcessName() {
        return subProcessName;
    }

    public long getDuration() {
        return duration;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("[ProcessInfo: m=");
        sb.append(getMainProcessName());
        sb.append(", p=");
        sb.append(getProcessName());
        sb.append(", s=");
        sb.append(getSubProcessName());
        sb.append("]");
        return sb.toString();
    }
}

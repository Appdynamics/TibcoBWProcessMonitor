package com.appdynamics.tibco.jmx.currentinfo;

import javax.management.openmbean.CompositeData;

/**
 * Created by trader on 1/23/16.
 */
public class ProcessInfo {

    private String mainProcessName;
    private String processName;
    private String subProcessName;
    private Long id;
    private long duration;

    public ProcessInfo(String mainProcessName, String processName, String subProcessName) {
        this.mainProcessName = mainProcessName;
        this.processName = processName;
        this.subProcessName = subProcessName;
    }

    public ProcessInfo(CompositeData source) {
        this((String) source.get("MainProcessName"), (String) source.get("Name"), (String) source.get("SubProcessName"));

        id = (Long) source.get("Id");
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

    public Long getId() {
        return id;
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

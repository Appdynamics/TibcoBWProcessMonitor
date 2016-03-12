package com.appdynamics.tibco.jmx;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import java.util.Collection;

/**
 * Created by trader on 1/23/16.
 */
public class ProcessCompositeData implements CompositeData {

    private String mainProcessName;
    private String processName;
    private String subProcessName;
    private String status;
    private long duration;

    public ProcessCompositeData(String mainProcessName, String processName, String subProcessName, String status, long duration) {
        this.mainProcessName = mainProcessName;
        this.processName = processName;
        this.subProcessName = subProcessName;
        this.status = status;
        this.duration = duration;
    }

    @Override
    public CompositeType getCompositeType() {
        return null;
    }

    @Override
    public Object get(String key) {
        Object result = null;
        if (key != null) {
            if (key.equals("Name")) {
                result = processName;
            } else if (key.equals("Status")) {
                result = status;
            } else if (key.equals("Duration")) {
                result = duration;
            } else if (key.equals("MainProcessName")) {
                result = mainProcessName;
            } else if (key.equals("SubProcessName")) {
                result = subProcessName;
            }
        }

        return result;
    }

    @Override
    public Object[] getAll(String[] keys) {
        return new Object[0];
    }

    @Override
    public boolean containsKey(String key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public Collection<?> values() {
        return null;
    }
}

package com.appdynamics.tibco.jmx;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by trader on 1/23/16.
 */
public class ProcessTree {

    private String processName;
    private Map<String, ProcessTree> children;

    private long totalDuration;
    private int count;

    public ProcessTree(String name) {
        processName = name;
        children = new HashMap<String, ProcessTree>();
        totalDuration = 0;
    }

    public String getProcessName() {
        return processName;
    }

    public ProcessTree getChild(String childProcessName) {
        ProcessTree child = children.get(childProcessName);
        if (child == null) {
            synchronized (this) {
                child = new ProcessTree(childProcessName);
                children.put(childProcessName, child);
            }
        }

        return child;
    }

    public Map<String, ProcessTree> getChildren() {
        return children;
    }

    public void putMetrics(long duration) {
        count++;
        totalDuration += duration;
    }

    public long getAverageDuration() {
        if (count > 0) {
            return totalDuration / count;
        } else {
            return -1;
        }
    }

    public int getCount() {
        return count;
    }

    public String toString() {
        return getString("");
    }

    private String getString(String root) {
        StringBuilder sb = new StringBuilder(root);
        sb.append("[ProcessTree: p=" + processName);
        if (children.isEmpty()) {
            sb.append(", no children");
        } else {
            sb.append(", c=\r\n");
            for (String name : children.keySet()) {
                sb.append(children.get(name).getString(root + "  "));
            }
            sb.append("\r\n");
            sb.append(root);
            sb.append("]");
        }

        return sb.toString();
    }
}

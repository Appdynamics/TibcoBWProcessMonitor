package com.appdynamics.tibco.jmx;

import javax.management.MBeanServerConnection;

/**
 * Created by trader on 1/23/16.
 */
public class MockJMXMonitor extends TibcoJMXMonitor {

    protected boolean connect() {

        try {
            MBeanServerConnection newConn = (MBeanServerConnection) Class.forName("com.appdynamics.tibco.jmx.MockMBeanServerConnection").newInstance();
            setMbeanConnection(newConn);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    protected void printCollectiveMetric(String key, String value) {
        System.out.println(metricPathPrefix + key + ":" + value + " -> AVERAGE,CURRENT,COLLECTIVE");
    }

    protected void printProcessInfo(String key, long duration, int observationCount) {
        System.out.println(metricPathPrefix + key + "|duration:" + duration + " -> AVERAGE,CURRENT,INDIVIDUAL");
        System.out.println(metricPathPrefix + key + "|activeCount:" + observationCount + " -> AVERAGE,CURRENT,INDIVIDUAL");
    }
}

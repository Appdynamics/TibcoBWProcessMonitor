package com.appdynamics.tibco.jmx;

import javax.management.MBeanServerConnection;

/**
 * Created by trader on 1/23/16.
 */
public class MockJMXMonitor extends TibcoJMXMonitor {

    protected MBeanServerConnection connect(int port) {

        MBeanServerConnection newConn = null;
        try {
            newConn = (MBeanServerConnection) Class.forName("com.appdynamics.tibco.jmx.MockMBeanServerConnection").newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newConn;
    }

    protected void printCollectiveMetric(String key, String value) {
        System.out.println(metricPathPrefix + key + ":" + value + " -> AVERAGE,CURRENT,COLLECTIVE");
    }

    protected void printProcessInfo(String key, long duration, int observationCount) {
        System.out.println(metricPathPrefix + key + "|duration:" + duration + " -> AVERAGE,CURRENT,INDIVIDUAL");
        System.out.println(metricPathPrefix + key + "|activeCount:" + observationCount + " -> AVERAGE,CURRENT,INDIVIDUAL");
    }
}

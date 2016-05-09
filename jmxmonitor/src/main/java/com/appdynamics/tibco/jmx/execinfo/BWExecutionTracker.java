package com.appdynamics.tibco.jmx.execinfo;

import com.appdynamics.tibco.jmx.MetricHandler;
import org.apache.log4j.Logger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

/**
 * Created by trader on 4/25/16.
 */
public class BWExecutionTracker {

    private MetricHandler metricHandler;

    private static final Logger logger = Logger.getLogger("com.singularity.TibcoJMXMonitor.execTracker");

    public BWExecutionTracker(MetricHandler metricHandler) {
        this.metricHandler = metricHandler;
    }

    public void getExecutionInfo(MBeanServerConnection mbeanConn, ObjectName mbeanName) {
        try {
            CompositeData obj = (CompositeData) mbeanConn.invoke(mbeanName, "GetExecInfo", new Object[]{}, new String[]{});
            Object status = obj.get("Status");

            if (status != null && !status.equals("FAILURE")) {
                metricHandler.printCollective("ExecInfo|Status", "1");
            } else {
                metricHandler.printCollective("ExecInfo|Status", "0");
            }

            metricHandler.printCollective("ExecInfo|Uptime", obj.get("Uptime").toString());
            metricHandler.printCollective("ExecInfo|Threads", obj.get("Threads").toString());
        } catch (Exception e) {
            logger.error("Error in getExecutionInfo", e);
        }
    }
}

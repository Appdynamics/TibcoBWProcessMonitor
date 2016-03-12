package com.appdynamics.tibco.jmx;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by trader on 1/23/16.
 */
public class BasicSanityTest {

    public void testHasMetrics() throws Exception {
        TibcoJMXMonitor mon = new MockJMXMonitor();
        Map<String, String> args = new HashMap<String, String>();
        args.put("metric-path-prefix", "Custom Metrics|Tibco|BW|ProcessInfo|");
        args.put("retries", "1");
        args.put("object-name-pattern", "com.tibco.bw.engine");
        args.put("port", "1234");
        mon.execute(args, null);
    }
}

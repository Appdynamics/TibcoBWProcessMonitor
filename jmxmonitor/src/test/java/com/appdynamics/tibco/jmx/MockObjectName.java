package com.appdynamics.tibco.jmx;

import javax.management.ObjectName;

/**
 * Created by trader on 1/23/16.
 */
public class MockObjectName extends ObjectName {

    MockObjectName() throws Exception {
        super("com.tibco.bw.engine.mocked:type=foo");
    }
}


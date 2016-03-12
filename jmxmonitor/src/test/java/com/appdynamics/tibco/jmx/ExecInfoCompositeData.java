package com.appdynamics.tibco.jmx;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by trader on 1/23/16.
 */
public class ExecInfoCompositeData implements CompositeData {

    @Override
    public CompositeType getCompositeType() {
        return null;
    }

    @Override
    public Object get(String key) {
        Object result = null;
        if (key != null) {
            if (key.equals("Status")) {
                result = "SUCCESS";
            } else if (key.equals("Uptime")) {
                result = String.valueOf("333333");
            } else if (key.equals("Threads")) {
                result = String.valueOf(60);
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


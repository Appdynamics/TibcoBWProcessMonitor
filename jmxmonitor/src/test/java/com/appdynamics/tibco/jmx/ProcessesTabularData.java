package com.appdynamics.tibco.jmx;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;
import java.util.*;

/**
 * Created by trader on 1/23/16.
 */

public class ProcessesTabularData implements TabularData {

    private List<CompositeData> values;

    public ProcessesTabularData() {
        values = new ArrayList<>();
        values.add(new ProcessCompositeData(null, null, null, null, 0));
        values.add(new ProcessCompositeData("p1", null, null, null, 0));
        values.add(new ProcessCompositeData(null, "p2", null, null, 0));
        values.add(new ProcessCompositeData(null, null, "p3", null, 0));
        values.add(new ProcessCompositeData("p4", "p5", null, null, 0));
        values.add(new ProcessCompositeData(null, "p6", "p7", null, 0));
        values.add(new ProcessCompositeData("p4", null, "p8", null, 0));
        values.add(new ProcessCompositeData("p9", "p10", "p11", null, 0));
        values.add(new ProcessCompositeData("p6", "p7", null, "SUCCESS", 2));
        values.add(new ProcessCompositeData(null, "p4", "p5", "SUCCESS", 2));
        values.add(new ProcessCompositeData(null, "p9", null, "SUCCESS", 2));

        values.add(new ProcessCompositeData(null, null, null, "SUCCESS", 0));
        values.add(new ProcessCompositeData("p1", null, null, "SUCCESS", 0));
        values.add(new ProcessCompositeData(null, "p2", null, "SUCCESS", 0));
        values.add(new ProcessCompositeData(null, null, "p3", "SUCCESS", 0));
        values.add(new ProcessCompositeData("p4", "p5", null, "SUCCESS", 0));
        values.add(new ProcessCompositeData(null, "p6", "p7", "SUCCESS", 0));
        values.add(new ProcessCompositeData("p4", null, "p8", "SUCCESS", 0));
        values.add(new ProcessCompositeData("p9", "p10", "p11", "SUCCESS", 0));

        values.add(new ProcessCompositeData(null, null, null, "SUCCESS", 20));
        values.add(new ProcessCompositeData("p1", null, null, "SUCCESS", 20));
        values.add(new ProcessCompositeData(null, "p2", null, "SUCCESS", 20));
        values.add(new ProcessCompositeData(null, null, "p3", "SUCCESS", 20));
        values.add(new ProcessCompositeData("p4", "p5", null, "SUCCESS", 20));
        values.add(new ProcessCompositeData(null, "p6", "p7", "SUCCESS", 20));
        values.add(new ProcessCompositeData("p4", null, "p8", "SUCCESS", 20));
        values.add(new ProcessCompositeData("p9", "p10", "p11", "SUCCESS", 20));

        values.add(new ProcessCompositeData(null, null, null, "SUCCESS", 30));
        values.add(new ProcessCompositeData("p1", null, null, "SUCCESS", 30));
        values.add(new ProcessCompositeData(null, "p2", null, "SUCCESS", 30));
        values.add(new ProcessCompositeData(null, null, "p3", "SUCCESS", 30));
        values.add(new ProcessCompositeData("p4", "p5", null, "SUCCESS", 30));
        values.add(new ProcessCompositeData(null, "p6", "p7", "SUCCESS", 30));
        values.add(new ProcessCompositeData("p4", null, "p8", "SUCCESS", 30));
        values.add(new ProcessCompositeData("p9", "p10", "p11", "SUCCESS", 30));

        values.add(new ProcessCompositeData(null, null, null, "SUCCESS", 45));
        values.add(new ProcessCompositeData("p1", null, null, "SUCCESS", 45));
        values.add(new ProcessCompositeData(null, "p2", null, "SUCCESS", 35));
        values.add(new ProcessCompositeData(null, null, "p3", "SUCCESS", 45));
        values.add(new ProcessCompositeData("p4", "p5", null, "SUCCESS", 45));
        values.add(new ProcessCompositeData(null, "p6", "p7", "SUCCESS", 40));
        values.add(new ProcessCompositeData("p4", null, "p8", "SUCCESS", 45));
        values.add(new ProcessCompositeData("p9", "p10", "p11", "SUCCESS", 55));
    }

    @Override
    public TabularType getTabularType() {
        return null;
    }

    @Override
    public Object[] calculateIndex(CompositeData value) {
        return new Object[0];
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object[] key) {
        return false;
    }

    @Override
    public boolean containsValue(CompositeData value) {
        return false;
    }

    @Override
    public CompositeData get(Object[] key) {
        return null;
    }

    @Override
    public void put(CompositeData value) {

    }

    @Override
    public CompositeData remove(Object[] key) {
        return null;
    }

    @Override
    public void putAll(CompositeData[] values) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<?> keySet() {
        return null;
    }

    @Override
    public Collection<?> values() {
        return values;
    }
}

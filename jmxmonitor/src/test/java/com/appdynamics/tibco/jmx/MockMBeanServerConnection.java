package com.appdynamics.tibco.jmx;

import javax.management.*;
import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by trader on 1/23/16.
 */
public class MockMBeanServerConnection implements MBeanServerConnection {

    private Set<ObjectName> names;

    MockMBeanServerConnection() throws Exception {
        names = new TreeSet<>();
        ObjectName name = new MockObjectName();
        names.add(name);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
        return null;
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
        return null;
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
        return null;
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
        return null;
    }

    @Override
    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException, IOException {

    }

    @Override
    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException, IOException {
        return null;
    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) throws IOException {
        return null;
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException {
        return names;
    }

    @Override
    public boolean isRegistered(ObjectName name) throws IOException {
        return false;
    }

    @Override
    public Integer getMBeanCount() throws IOException {
        return null;
    }

    @Override
    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
        return null;
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException, IOException {
        return null;
    }

    @Override
    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException, IOException {

    }

    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException, IOException {
        return null;
    }

    @Override
    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
            throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {

        Object result = null;

        if (name.getCanonicalName().contains("com.tibco.bw")) {
            if (operationName.equals("GetProcesses")) {
                ProcessesTabularData td = new ProcessesTabularData();
                result = td;
            } else if (operationName.equals("GetExecInfo")) {
                ExecInfoCompositeData cd = new ExecInfoCompositeData();
                result = cd;
            }
        } else {
            throw new InputMismatchException(name.getCanonicalName());
        }

        return result;
    }

    @Override
    public String getDefaultDomain() throws IOException {
        return null;
    }

    @Override
    public String[] getDomains() throws IOException {
        return new String[0];
    }

    @Override
    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, IOException {

    }

    @Override
    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, IOException {

    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {

    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {

    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {

    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {

    }

    @Override
    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
        return null;
    }

    @Override
    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException, IOException {
        return false;
    }
}

# TibcoBWProcessMonitor
Tibco BW Process monitoring extension

Tibco BW must be run with JMX enabled for this extension to work.  As of this writing the Hawk Console monitoring capability is not functional, so only the JMX monitor is usable.

When enabling JMX in Tibco BW, you select the JMX port.  That port number must be configured in monitor.xml for this extension to work.

To build you must have Tibco dependencies available, they are not available through Maven.  Also, the AppDynamics machine agent is a dependency.  Variables are used in the build files to resolve these dependencies.  For example, if you have Tibco installed in a directory called /Users/paulbunyan/tibco and an AppDynamics machine agent installed at /Users/paulbunyan/machine-agent, then you build using:

% mvn -Dtibhawk=/Users/paulbunyan/tibco/bw6/hawk/5.1 -Dappd-ma=/Users/paulbunyan/machine-agent clean package

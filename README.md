# TibcoBWProcessMonitor
Tibco BW Process monitoring extension

Tibco BW must be run with JMX enabled for this extension to work.

LIMITATIONS

As of this writing the Hawk Console monitoring capability is not functional, so only the JMX monitor is usable.  Do not try to build the hawk console monitor in the github repo, it will not build and is as of now just a template.

USE CASE

This extension monitors Tibco BW processes and their sub-processes ("Jobs"), giving count and duration metrics for each.  Process counts are important because in a BW environment there is usually a fixed thread pool available for running process jobs -- high counts mean that jobs are sitting around waiting for an available thread.  Duration metrics are basic ART info.

PREREQUISITES AND TROUBLESHOOTING

Build:

To build you must have Tibco dependencies available, they are not available through Maven.  Also, the AppDynamics machine agent is a dependency.  Variables are used in the build files to resolve these dependencies.  For example, if you have Tibco installed in a directory called /Users/paulbunyan/tibco and an AppDynamics machine agent installed at /Users/paulbunyan/machine-agent, then you build using:

% mvn -Dtibhawk=/Users/paulbunyan/tibco/bw6/hawk/5.1 -Dappd-ma=/Users/paulbunyan/machine-agent clean package

Deploy/run

When enabling JMX in Tibco BW, you select the JMX port in the ".tra" file of your Tibco deployment.  That port number must be configured in monitor.xml for this extension to work.  This extension levarages localhost JMX, so the machine agent running this extension should be on the same OS instance as a Tibco process with JMX enabled.

The port is the only value in monitor.xml that MUST be changed.  The other values should work fine with the defaults, only change if there is a use case reason to do so.

Loggers used are com.singularity.TibcoJMSMonitor and com.singularity.TibcoJMSMonitor.ProcessInfo.  Info-level logging is not great for troubleshooting, but debug-level logging should give reasonable information on how the JMX connection is working, and on what stats objects are being returned from JMX.

Tibco

Tibco processes must be run with the standard JMX localhost command-line options:
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.port=<JMX port>
-Dcom.sun.management.jmxremote.ssl=false

The .tra file option Jmx.Enabled=true must be set in the .tra file for every process to be monitored.

METRICS PROVIDED

Per the info in USE CASE: count and duration for Tibco proesses, by process.  The default metric tree prefix is

Custom Metrics|Tibco|BW|ProcessInfo|

Below that will be a process name, and under that count and duration metrics.

INSTALLATION

Create a directory under monitors for the extension.  Copy monitor.xml (with the port number changed) and the jar file for the monitor into that directory, and restart the machine agent.

CONFIGURATION

Per the info in PREREQUISITES AND TROUBLESHOOTING, only the port number in monitor.xml must be changed -- to the value used for the JMX port in the .tra file for one of the Tibco BW nodes.

CONTRIBUTION

https://github.com/Appdynamics/TibcoBWProcessMonitor

SUPPORT

1.0: contact help@appdynamics.com

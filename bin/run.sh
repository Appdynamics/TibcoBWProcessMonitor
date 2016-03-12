#!/bin/bash

set -x

TIBCO=/Users/trader/tibco
ARCHIVE=/Users/trader/dev/tibco/hawkconsole/target/hawkmonitor.jar
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.7.0_51.jdk/Contents/Home

HAWK=${TIBCO}/bw6/hawk/5.1
RV=${TIBCO}/bw6/bw/6.2/system/design/plugins/com.tibco.bw.5x.libraries_1.3.0.001/jars/tibrv/8.3
TALON=${HAWK}/lib/talon.jar
CONSOLE=${HAWK}/lib/console.jar
UTIL=${HAWK}/lib/util.jar
TIBRV=${RV}/lib/tibrvj.jar

HAWK_DOMAIN=myDomain
RV_SVC=mySvc
RV_NET=myNet
RV_DAEMON=myDaemon

export PATH=${JAVA_HOME}/bin:${PATH}
export CLASSPATH=${CLASSPATH}:${TALON}:${CONSOLE}:${UTIL}:${TIBRV}

java -Xbootclasspath/a:${CLASSPATH} -jar ${ARCHIVE} ${HAWK_DOMAIN} ${RV_SVC} ${RV_NET} ${RV_DAEMON}


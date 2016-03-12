#!/bin/bash

HAWK=/Users/trader/tibco/bw6/hawk/5.1/
PRJ=/Users/trader/dev/tibco/hawkconsole
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.7.0_51.jdk/Contents/Home

TALON=${HAWK}/lib/talon.jar
CONSOLE=${HAWK}/lib/console.jar
SRC=${PRJ}/src/main/java
CLASSES=${PRJ}/classes
TARGET=${PRJ}/target
ARCHIVE=${TARGET}/hawkmonitor.jar
MANIFEST=${PRJ}/src/main/resources/META-INF/hawkmonitor.mf

rm -rf ${CLASSES}
mkdir ${CLASSES}
rm -f ${ARCHIVE}

export PATH=${JAVA_HOME}/bin:${PATH}
export CLASSPATH=${CLASSPATH}:${TALON}:${CONSOLE}

cd ${SRC}
javac -d ${CLASSES} com/appdynamics/tibco/hawkmonitor/HawkConsoleMetrics.java
cd ${CLASSES}
jar cmf ${MANIFEST} ${ARCHIVE} *


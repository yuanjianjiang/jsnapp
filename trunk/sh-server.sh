#!/bin/sh

JRE_HOME="/opt/sun-jdk-6/1.6.0_01"
CLASSPATH="bin:lib/jakarta-httpcore.jar:lib/xerces.jar:lib/hsqldb.jar:lib/postgresql.jar:lib/log4j.jar"

${JRE_HOME}/bin/java -Duser.language=en -cp $CLASSPATH org.jsnap.Launcher conf/log4j.xml conf/server.xml

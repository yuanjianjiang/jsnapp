#!/bin/sh

JRE_HOME="/opt/sun-jdk-6/1.6.0_01"
CPATH="bin:lib/jakarta-httpcore.jar:lib/xerces.jar:lib/hsqldb.jar:lib/postgresql.jar:lib/log4j.jar"

${JRE_HOME}/bin/java -Duser.language=en -cp $CPATH org.jsnap.StresClient org.jsnap.request.SizePackedRequest execute
psql -Ujsnap -dinternal -f res/truncatehistory.sql
${JRE_HOME}/bin/java -Duser.language=en -cp $CPATH org.jsnap.StresClient org.jsnap.request.HttpRequest execute
psql -Ujsnap -dinternal -f res/truncatehistory.sql
${JRE_HOME}/bin/java -Duser.language=en -cp $CPATH org.jsnap.StresClient org.jsnap.request.SizePackedSecureRequest execute
psql -Ujsnap -dinternal -f res/truncatehistory.sql
${JRE_HOME}/bin/java -Duser.language=en -cp $CPATH org.jsnap.StresClient org.jsnap.request.SSLRequest execute
psql -Ujsnap -dinternal -f res/truncatehistory.sql
${JRE_HOME}/bin/java -Duser.language=en -cp $CPATH org.jsnap.StresClient org.jsnap.request.SizePackedRequest reserve
psql -Ujsnap -dinternal -f res/truncatehistory.sql
${JRE_HOME}/bin/java -Duser.language=en -cp $CPATH org.jsnap.StresClient org.jsnap.request.HttpRequest reserve
psql -Ujsnap -dinternal -f res/truncatehistory.sql
${JRE_HOME}/bin/java -Duser.language=en -cp $CPATH org.jsnap.StresClient org.jsnap.request.SizePackedSecureRequest reserve
psql -Ujsnap -dinternal -f res/truncatehistory.sql
${JRE_HOME}/bin/java -Duser.language=en -cp $CPATH org.jsnap.StresClient org.jsnap.request.SSLRequest reserve
psql -Ujsnap -dinternal -f res/truncatehistory.sql

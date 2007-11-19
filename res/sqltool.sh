#! /bin/bash
#
# urlid: A valid urlid specified in sqltool.rc.
# script: The script file that will create the internal database on the target url.
#
# java -cp ../lib/hsqldb.jar org.hsqldb.util.SqlTool --rcfile ./sqltool.rc <urlid> <script>
#
# Do not limit yourself with HSQLDB, SqlTool accesses all databases through a JDBC driver.
# Specify database specific driver with --driver if necessary.

# java -cp ../lib/hsqldb.jar org.hsqldb.util.DatabaseManager

java -cp ../lib/hsqldb.jar org.hsqldb.util.SqlTool --rcfile ./sqltool.rc internaldb hsqldb.create


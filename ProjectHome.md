jsnap is a standalone server that acts as a middleware between clients that want to execute SQL statements -yet not want to deal with database programming issues like pooling, timeouts, cursors etc.- and database servers.

If Java is installed, running jsnap is as easy as running the script sh-server. No other external dependencies, all needed jar's are in the lib/ folder. Development started with JDK 1.5 and is now continuing with 1.6. Though not tested, it probably would work perfectly with 1.4.

# Features #

1) Server configuration is done via an XML file and configuration can be modified on the fly even when the server is up and running. Server's outputs are done via log4j, configure it anyway (console, file, smtp etc.) you like.

2) Server configuration mainly consists of listeners and databases; listeners listen to TCP/IP requests from clients and databases are the resources on which accepted client requests are executed. One of the configured databases is the internal database that is used by the jsnap server for user/group management, access control and logging.

3) Listeners may accept requests of different types. That is, the way a request is encoded and transported is totally parametric. Requests could be sent via HTTP to a listener that is accepting HTTP connections or they could be sent via a user-defined protocol/transport over plain/secure sockets. Requests could be sent over an SSL connection regardless of the way they are encoded.

A listener is configured to accept a certain type of request, e.g. HTTPRequest, SSLRequest, from a certain port. jsnap server may be configured to run as many listeners as desired.

4) Databases are basically connection pools. Pool and connection management (connect/disconnect/ping/reconnect...) is performed by jsnap and clients are totally isolated from these processes. A connection management policy for each database is easily set via the configuration file also; jsnap could take databases online/offline (in terms of client access) according to the defined policy and the status of the database.

All databases with JDBC drivers could be managed by jsnap; however, this is not a strict requirement. Although not currently in place, support for other data sources (spreadsheets, text files etc.) could very easily be implemented. Also the generic JDBC driver could be extended (specialized) in order to take advantage of some features of the original JDBC drivers, e.g. Oracle's ping, Postgres' cancelQuery etc.

5) jsnap server allows a client to execute a SQL statement. Moreover, a client may also ask the server to execute a SQL statement but keep the resultset open until a commit/rollback or timeout. That is, for INSERT/UPDATE/DELETE statements, the client is able to delay the commit/rollback, disabling autocommit. This also means that, for the SELECT statements, the client may scroll over the query's output (if the database supports scrolling) or view only certain sections (in terms of row numbers) of the result set.

The server also allows the client to specify how the output of the executed SQL statement should be formatted. Currently, the client may receive outputs in CSV, HTML or XML format. Formatting is done transparently from the client; the only thing the client needs to do is to read from the socket. It is also very easy to implement a user-defined binary/ascii encoding for the result sets, allowing jsnap to work with, for example, Borland's datasets.

6) Java classes for Java clients are already in place. A Java client only needs to instantiate an object, fill in the SQL body, parameters, user credentials etc. and call send() to send the request; the call to receive() fetches all the requested data nicely formatted for processing. With jsnap, database access is equivalent to writing/reading from a socket.

Since database access is simplified to socket programming, it is possible to code in any programming language. For example, when coding with C it is easier to open a socket and write/read from it rather than working with low-level database libraries or precompilers. Even a web browser could be jsnap's client, an example SqlExecutor.html exists within the source.

7) jsnap allows setting an authentication policy and an access control policy for every database. jsnap server could allow/deny running a SQL statement according to the database's security policies. User based policies are implemented, group and IP address based policies are on the way, database object (e.g. tables, fields) based policies are planned. All access to databases are logged if required.

8) jsnap's web interface allows administrators and users to login for certain operations. Users, as expected, are only able to view limited features, whereas, administrators also perform user/group management and view statistics.

# Ongoing #

1) **FIRST AND FOREMOST: Documentation! (except inline.)**

2) Several access control policies will be implemented.

3) Support for scheduling SQL statements will be implemented.

4) JDBC's timeout mechanism is not good enough; a better and generic timeout mechanism will be implemented.

5) Web interface will have more features.

6) Generic JDBC driver will be extended to take advantage of original JDBC drivers.
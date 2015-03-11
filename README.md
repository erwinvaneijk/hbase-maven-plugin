hbase-maven-plugin
==================

A maven plugin that starts/stops a mini HBase cluster.

This plugin is useful for integration testing code that interacts with
an HBase cluster.  Typically, you will bind the `start` goal to your
`pre-integration-test` phase and the `stop` goal to the
`post-integration-test` phase of the maven build lifecycle.


Selecting a Hadoop backend
--------------------------

This plugin can be run against multiple Hadoop backends. Version 1.1.0
will run with a standard hbase cluster using the YARN/Hadoop 2 based
HBase 1.0.0 line.

To run with 1.1.0, you must:
* Set `umask 0022`
* Disable IPv6 in your maven process: `export MAVEN_OPTS="-Djava.net.preferIPv4Stack=true"`


### Documentation

* [Plugin Usage](http://docs.kiji.org/apidocs/hbase-maven-plugin/1.0.10-cdh4/usage.html)
* [Plugin Goals](http://docs.kiji.org/apidocs/hbase-maven-plugin/1.0.10-cdh4/plugin-info.html)
* [Java API (Javadoc)](http://docs.kiji.org/apidocs/hbase-maven-plugin/1.0.10-cdh4/apidocs/)

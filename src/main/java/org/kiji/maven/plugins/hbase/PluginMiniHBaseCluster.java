/**
 * Licensed to WibiData, Inc. under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  WibiData, Inc.
 * licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.kiji.maven.plugins.hbase;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HConstants;
import org.apache.maven.plugin.logging.Log;

/**
 * A in-process mini HBase cluster that may be started and stopped.
 */
public class PluginMiniHBaseCluster extends MavenLogged {
    private org.apache.hadoop.hbase.MiniHBaseCluster _miniHbaseCluster;

    /**
     * An HBase testing utility for starting/stopping the cluster.
     */
    private final HBaseTestingUtility _testUtility;

    /**
     * Whether a mini MapReduce cluster should also be run.
     */
    private final boolean _isMapReduceEnabled;

    /**
     * Whether the cluster is running.
     */
    private boolean _isRunning;

    /**
     * Creates a new <code>MiniHBaseCluster</code> instance.
     *
     * @param log             The maven log.
     * @param enableMapReduce Whether to also use a mini MapReduce cluster.
     * @param conf            Hadoop configuration for the cluster.
     */
    public PluginMiniHBaseCluster(final Log log, final boolean enableMapReduce, final Configuration conf) {
        //this(log, enableMapReduce, new HBaseTestingUtility(conf));
        super(log);
        getLog().info("PluginMiniHBaseCluster(configuration)");
        final Configuration configuration = reconfigure(conf);
        _testUtility = new HBaseTestingUtility((configuration));
        _isMapReduceEnabled = enableMapReduce;
        _isRunning = false;
    }

    /**
     * Creates a new <code>MiniHBaseCluster</code> instance.
     *
     * @param log             The maven log.
     * @param enableMapReduce Whether to also use a mini MapReduce cluster.
     * @param hbaseTestUtil   An HBase testing utility that can start and stop mini clusters.
     */
    public PluginMiniHBaseCluster(final Log log, final boolean enableMapReduce, final HBaseTestingUtility hbaseTestUtil) {
        super(log);
        getLog().info("The other one.");
        _testUtility = configure(hbaseTestUtil);
        _isMapReduceEnabled = enableMapReduce;
        _isRunning = false;
    }

    /**
     * Configures an HBase testing utility.
     *
     * @param testUtil The test utility to reconfigure.
     * @return The configured utility.
     */
    private static HBaseTestingUtility configure(final HBaseTestingUtility testUtil) {
        // If HBase servers are running locally, the utility will use
        // the "normal" ports. We override *all* ports first, so that
        // we ensure that this can start without a problem.

        final Configuration conf = testUtil.getConfiguration();
        reconfigure(conf);
        return testUtil;
    }

    /**
     * Configures an HBase testing utility.
     *
     * @param configuration The configuration to adapt.
     * @return The configured utility.
     */
    private static Configuration reconfigure(final Configuration configuration) {
        // If HBase servers are running locally, the utility will use
        // the "normal" ports. We override *all* ports first, so that
        // we ensure that this can start without a problem.

        final int offset = new Random(System.currentTimeMillis()).nextInt(1500) + 500;

        // Move the master to a hopefully unused port.
        // configuration.setInt(HConstants.MASTER_PORT, findOpenPort(HConstants.DEFAULT_MASTER_PORT + offset));
        // Disable the master's web UI.
        configuration.setInt("hbase.master.info.port", -1);

        // Move the regionserver to a hopefully unused port.
        configuration.setInt(HConstants.REGIONSERVER_PORT,
            findOpenPort(HConstants.DEFAULT_REGIONSERVER_PORT + offset));
        configuration.setInt("hbase.regionserver.info.port",
            findOpenPort(HConstants.DEFAULT_REGIONSERVER_PORT + 10 + offset));

        configuration.setBoolean("hbase.cluster.distributed", false);

        // Increase max zookeeper client connections.
        configuration.setInt("hbase.zookeeper.property.maxClientCnxns", 80);

        configuration.set("hbase.master.logcleaner.plugins",
            "org.apache.hadoop.hbase.master.cleaner.TimeToLiveLogCleaner");

        return configuration;
    }

    /**
     * Find an available port.
     *
     * @param startPort the starting port to check.
     * @return an open port number.
     * @throws IllegalArgumentException if it can't find an open port.
     */
    public static int findOpenPort(final int startPort) {
        if (startPort < 1024 || startPort > 65534) {
            throw new IllegalArgumentException("Invalid start port: " + startPort);
        }

        for (int port = startPort; port < 65534; port++) {
            ServerSocket ss = null;
            try {
                ss = new ServerSocket(port);
                ss.setReuseAddress(true);
                return port;
            }
            catch (final IOException ioe) {
                // This port isn't open. Loop around.
            }
            finally {
                if (ss != null) {
                    try {
                        // TODO(aaron): Technically, this causes a race condition. Another instance of
                        // findOpenPort() could determine that this port is available between now and
                        // when the client of this method calls this function.
                        ss.close();
                    }
                    catch (final IOException ioe) {
                        // Shouldn't happen.
                    }
                }
            }
        }

        throw new IllegalArgumentException("No port available starting at " + startPort);
    }

    /**
     * Starts the cluster.  Blocks until ready.
     *
     * @throws Exception If there is an error.
     */
    public void startup() throws Exception {
        if (isRunning()) {
            throw new RuntimeException("Cluster already running.");
        }
        getLog().info("Starting my mini cluster");

        getLog().info(_testUtility.getConfiguration().toString());
        _miniHbaseCluster = _testUtility.startMiniCluster();

        getLog().info("Mini cluster started");
        if (_isMapReduceEnabled) {
            getLog().info("Starting MapReduce cluster...");


            // Work around a bug in HBaseTestingUtility that requires this conf var to be set.
            getConfiguration().set("hadoop.log.dir", getConfiguration().get("hadoop.tmp.dir"));

            // Start a mini MapReduce cluster with one server.
            _testUtility.startMiniMapReduceCluster();

            // Set the mapred.working.dir so stuff like partition files get written somewhere reasonable.
            getConfiguration().set("mapred.working.dir",
                _testUtility.getDataTestDir("mapred-working").toString());

            getLog().info("MapReduce cluster started.");
        }
        _isRunning = true;
    }

    /**
     * Determine whether the cluster is running.
     *
     * @return Whether the cluster is running.
     */
    public boolean isRunning() {
        return _isRunning;
    }

    /**
     * Provides access to the HBase cluster configuration.
     *
     * @return The cluster configuration.
     */
    public Configuration getConfiguration() {
        return _testUtility.getConfiguration();
    }

    /**
     * Stops the cluster.  Blocks until shut down.
     *
     * @throws Exception If there is an error.
     */
    public void shutdown() throws Exception {
        if (!_isRunning) {
            getLog().error(
                "Attempting to shut down a cluster, but one was never started in this process.");
            return;
        }
        if (_isMapReduceEnabled) {
            getLog().info("Shutting down MapReduce cluster...");
            _testUtility.shutdownMiniMapReduceCluster();
            getLog().info("MapReduce cluster shut down.");
        }
        if (_miniHbaseCluster != null) {
            _miniHbaseCluster.shutdown();
        }
        _testUtility.shutdownMiniCluster();
    }
}

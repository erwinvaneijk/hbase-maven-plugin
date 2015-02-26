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

import org.apache.maven.plugin.logging.Log;

/**
 * A thread that starts and runs a mini HBase cluster.
 */
public class PluginMiniHBaseClusterThread extends Thread implements MavenLoggable {
    /**
     * The maven log.
     */
    private final Log _log;

    /**
     * The local hbase cluster.
     */
    private final PluginMiniHBaseCluster _pluginMiniHBaseCluster;

    /**
     * Whether the cluster is started and ready.
     */
    private volatile boolean _isClusterReady;

    /**
     * Whether the thread has been asked to stop.
     */
    private volatile boolean _isStopRequested;

    /**
     * Creates a new <code>MiniHBaseClusterThread</code> instance.
     *
     * @param log          The maven log.
     * @param hbaseCluster The hbase cluster to run.
     */
    public PluginMiniHBaseClusterThread(Log log, PluginMiniHBaseCluster hbaseCluster) {
        _log = log;
        _pluginMiniHBaseCluster = hbaseCluster;
        _isClusterReady = false;
        _isStopRequested = false;
    }

    /**
     * Determine whether the HBase cluster is up and running.
     *
     * @return Whether the cluster has completed startup.
     */
    public boolean isClusterReady() {
        return _isClusterReady;
    }

    /**
     * Stops the HBase cluster gracefully.  When it is fully shut down, the thread will exit.
     */
    public void stopClusterGracefully() {
        _isStopRequested = true;
        interrupt();
    }

    /**
     * Runs the mini HBase cluster.
     *
     * <p>This method blocks until {@link #stopClusterGracefully()} is called.</p>
     */
    @Override
    public void run() {
        getLog().info("Starting up HBase cluster...");
        try {
            _pluginMiniHBaseCluster.startup();
        }
        catch (Exception e) {
            getLog().error("Unable to start an HBase cluster.", e);
            interrupt();
            return;
        }
        getLog().info("HBase cluster started.");
        _isClusterReady = true;
        yield();

        // Twiddle our thumbs until somebody requests the thread to stop.
        while (!_isStopRequested) {
            try {
                sleep(1000);
            }
            catch (InterruptedException e) {
                getLog().debug("Main thread interrupted while waiting for cluster to stop.");
            }
        }

        getLog().info("Starting graceful shutdown of the HBase cluster...");
        try {
            _pluginMiniHBaseCluster.shutdown();
        }
        catch (Exception e) {
            getLog().error("Unable to stop the HBase cluster.", e);
            return;
        }
        getLog().info("HBase cluster shut down.");
    }

    @Override
    public Log getLog() {
        return _log;
    }
}

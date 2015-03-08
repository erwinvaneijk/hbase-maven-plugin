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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.ServerSocket;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the MiniHbaseCluster for all its subtleties.
 */
public class PluginMiniHBaseClusterTest {
    /**
     * A mock maven log.
     */
    private Log _log;

    /**
     * A mock HBase testing utility that starts and stops clusters.
     */
    private HBaseTestingUtility _hbaseTestingUtil;

    @Before
    public void createMocks() {
        _log = createMock(Log.class);
        _hbaseTestingUtil = createMock(HBaseTestingUtility.class);
    }

    @Test
    public void testWithMapReduceDisabled() throws Exception {
        // Expect any number of log calls.
        _log.info(anyObject(String.class));
        expectLastCall().anyTimes();

        // Expect the HBase cluster to be configured, started, and stopped.
        expect(_hbaseTestingUtil.getConfiguration()).andReturn(new Configuration()).anyTimes();
        expect(_hbaseTestingUtil.startMiniCluster()).andReturn(null);
        _hbaseTestingUtil.shutdownMiniCluster();

        replayMocks();
        final PluginMiniHBaseCluster cluster = new PluginMiniHBaseCluster(_log, false /* Disable MR */, _hbaseTestingUtil);
        cluster.startup();
        cluster.shutdown();
        verifyMocks();
    }

    /**
     * Tells the mock to start listening for expected calls.
     */
    private void replayMocks() {
        replay(_log);
        replay(_hbaseTestingUtil);
    }

    /**
     * Verifies that the mocks have received all their expected calls.
     */
    private void verifyMocks() {
        verify(_log);
        verify(_hbaseTestingUtil);
    }

    @Test
    public void testWithMapReduceEnabled() throws Exception {
        // Expect any number of log calls.
        _log.info(anyObject(String.class));
        expectLastCall().anyTimes();

        // Expect the HBase testing utility to be configured.
        expect(_hbaseTestingUtil.getConfiguration()).andReturn(new Configuration()).anyTimes();

        // Expect the HBase cluster to be started and stopped.
        expect(_hbaseTestingUtil.startMiniCluster()).andReturn(null);
        _hbaseTestingUtil.shutdownMiniCluster();

        // Expect that the MapReduce cluster will be started and stopped.
        expect(_hbaseTestingUtil.startMiniMapReduceCluster()).andReturn(null).anyTimes();

        _hbaseTestingUtil.shutdownMiniMapReduceCluster();
        // Expect the HBase testing utility to request a test dir for mapred.
        expect(_hbaseTestingUtil.getDataTestDir(anyObject(String.class)))
            .andReturn(new Path("/mapred-working"));

        replayMocks();
        final PluginMiniHBaseCluster cluster = new PluginMiniHBaseCluster(_log, true /* Enable MR */, _hbaseTestingUtil);
        cluster.startup();
        cluster.shutdown();
        verifyMocks();
    }

    @Test
    public void testListenerOccupied() throws Exception {
        // Test that findOpenPort() doesn't return a port we know to be in use.
        final ServerSocket ss = new ServerSocket(9867);
        ss.setReuseAddress(true);
        try {
            final int openPort = PluginMiniHBaseCluster.findOpenPort(9867);
            assertTrue("Port 9867 is already bound!", openPort > 9867);
        }
        finally {
            ss.close();
        }
    }

    @Test
    public void testListenerOpen() throws Exception {
        // Test that findOpenPort() can return a port we don't believe is in use.
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(9867);
            ss.setReuseAddress(true);
        }
        finally {
            if (null != ss) {
                ss.close();
            }
        }

        // Port 9867 is unlikely to be in use, since we just successfully bound to it
        // and closed it.

        final int openPort = PluginMiniHBaseCluster.findOpenPort(9867);
        assertEquals("Port 9867 shouldn't be currently bound!", 9867, openPort);
    }
}

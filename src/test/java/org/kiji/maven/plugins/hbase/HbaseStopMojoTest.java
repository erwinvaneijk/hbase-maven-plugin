package org.kiji.maven.plugins.hbase;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test the StopMojo class.
 *
 * @author Erwin van Eijk
 */
public class HbaseStopMojoTest {
    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

    @Rule
    public TestResources _resources = new TestResources();

    @Test
    public void testMojoGoal() throws Exception {
        final File testResourceDir = _resources.getBasedir("unit/basic-test/");
        assertThat(testResourceDir, is(not(equalTo(null))));
        final File pom = new File(testResourceDir, "basic-test-plugin-config.xml");
        assertThat(pom, is(not(equalTo(null))));
        assertTrue(pom.exists());
        final StopMojo mojo = (StopMojo) rule.lookupMojo("stop", pom);

        assertThat(mojo, is(not(equalTo(null))));
    }

}

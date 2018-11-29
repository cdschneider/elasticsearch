package org.elasticsearch.gradle.precommit;

import org.elasticsearch.gradle.test.GradleUnitTestCase;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class ForbiddenPatternsTaskTests extends GradleUnitTestCase {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    public void testCheckInvalidPatterns() throws Exception {

    }
}

package org.elasticsearch.gradle.precommit;

import org.elasticsearch.gradle.test.GradleUnitTestCase;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class ForbiddenPatternsTaskTests extends GradleUnitTestCase {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    public void testCheckInvalidPatternsWhenNoSourceFilesExist() throws Exception {
        Project project = createProject();
        ForbiddenPatternsTask2 forbiddenPatternsTask = createTask(project);

        forbiddenPatternsTask.checkInvalidPatterns();

        File outputMarker = new File(project.getBuildDir(), "markers/forbiddenPatterns");
        List<String> result = Files.readAllLines(outputMarker.toPath(), StandardCharsets.UTF_8);
        assertEquals("done", result.get(0));
    }

    private Project createProject() throws IOException {
        Project project = ProjectBuilder.builder().withProjectDir(temporaryFolder.newFolder()).build();
        project.getPlugins().apply(JavaPlugin.class);
        return project;
    }

    private ForbiddenPatternsTask2 createTask(Project project) {
        return project.getTasks().create("forbiddenPatternsTask", ForbiddenPatternsTask2.class);
    }
}

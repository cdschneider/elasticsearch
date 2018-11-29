package org.elasticsearch.gradle.precommit;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks for patterns in source files for the project which are forbidden.
 */
public class ForbiddenPatternsTask2 extends DefaultTask {

    @OutputFile
    File outputMarker = new File(getProject().getBuildDir(), "markers/forbiddenPatterns");

    /** The rules: a map from the rule name, to a rule regex pattern. */
    private static final Map<String, String> patterns = new HashMap<>();
    static {
        // add mandatory rules
        patterns.put("nocommit", "nocommit|NOCOMMIT");
        patterns.put("nocommit should be all lowercase or all uppercase", "((?i)nocommit)(?<!(nocommit|NOCOMMIT))");
        patterns.put("tab", "\t");

        //nocommit
    }

    /** A pattern set of which files should be checked. */
    private final PatternFilterable filesFilter = new PatternSet()
        // we always include all source files, and exclude what should not be checked
        .include("**")
        // exclude known binary extensions
        .exclude("**/*.gz")
        .exclude("**/*.ico")
        .exclude("**/*.jar")
        .exclude("**/*.zip")
        .exclude("**/*.jks")
        .exclude("**/*.crt")
        .exclude("**/*.png");

    public ForbiddenPatternsTask2() {
        setDescription("Checks source files for invalid patterns like nocommits or tabs");

        getInputs().property("excludes", filesFilter.getExcludes());
        getInputs().property("rules", patterns);
    }

    /** Adds a file glob pattern to be excluded */
    public void exclude(String... excludes) {
        filesFilter.exclude(excludes);
    }

    void rule(Map<String,String> props) {
        String name = props.remove("name");
        if (name == null) {
            throw new InvalidUserDataException("Missing [name] for invalid pattern rule");
        }
        String pattern = props.remove("pattern");
        if (pattern == null) {
            throw new InvalidUserDataException("Missing [pattern] for invalid pattern rule");
        }
        if (props.isEmpty() == false) {
            throw new InvalidUserDataException("Unknown arguments for ForbiddenPatterns rule mapping: ${props.keySet()}");
        }
        // TODO: fail if pattern contains a newline, it won't work (currently)
        patterns.put(name, pattern);
    }

    /** Returns the files this task will check */
    @InputFiles
    FileCollection files() {
        List<FileCollection> collections = new ArrayList<>();
        for (SourceSet sourceSet : getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()) {
            collections.add(sourceSet.getAllSource().matching(filesFilter));
        }
        return getProject().files(collections.toArray());
    }

    @TaskAction
    void checkInvalidPatterns() throws IOException {
        Pattern allPatterns = Pattern.compile("(" + String.join(")|(", patterns.values()) + ")");
        List<String> failures = new ArrayList<>();
        for (File f : files()) {
            /* TODO
            if (allPatterns.matcher(line).find()) {
                addErrorMessages(failures, f, line, lineNumber);
            }
            */
        }
        if (failures.isEmpty() == false) {
            throw new GradleException("Found invalid patterns:\n" + String.join("\n", failures));
        }

        outputMarker.getParentFile().mkdirs();
        Files.write(outputMarker.toPath(), "done".getBytes("UTF-8"));
    }

    // iterate through patterns to find the right ones for nice error messages
    void addErrorMessages(List<String> failures, File f, String line, int lineNumber) {
        String path = getProject().getRootProject().getProjectDir().toURI().relativize(f.toURI()).toString();
        for (Map.Entry<String,String> pattern : patterns.entrySet()) {
            if (Pattern.compile(pattern.getValue()).matcher(line).find()) {
                failures.add("- " + pattern.getKey() + " on line " + lineNumber + " of " + path);
            }
        }
    }
}

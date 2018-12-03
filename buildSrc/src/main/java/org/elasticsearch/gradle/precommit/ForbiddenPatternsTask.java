package org.elasticsearch.gradle.precommit;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Checks for patterns in source files for the project which are forbidden.
 */
public class ForbiddenPatternsTask extends DefaultTask {

    /*
     * A pattern set of which files should be checked.
     */
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

    /*
     * The rules: a map from the rule name, to a rule regex pattern.
     */
    private static final Map<String, String> patterns = new HashMap<>();
    static {
        // add mandatory rules
        patterns.put("nocommit", "nocommit|NOCOMMIT");
        patterns.put("nocommit should be all lowercase or all uppercase", "((?i)nocommit)(?<!(nocommit|NOCOMMIT))");
        patterns.put("tab", "\t");
    }

    private File outputMarker = new File(getProject().getBuildDir(), "markers/forbiddenPatterns");

    public ForbiddenPatternsTask() {
        setDescription("Checks source files for invalid patterns like nocommits or tabs");
        getInputs().property("excludes", filesFilter.getExcludes());
        getInputs().property("rules", patterns);
    }

    @InputFiles
    @SkipWhenEmpty
    public FileCollection files() {
        List<FileCollection> collections = new ArrayList<>();
        for (SourceSet sourceSet : getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()) {
            collections.add(sourceSet.getAllSource().matching(filesFilter));
        }
        return getProject().files(collections.toArray());
    }

    @TaskAction
    public void checkInvalidPatterns() throws IOException {
        Pattern allPatterns = Pattern.compile("(" + String.join(")|(", patterns.values()) + ")");
        List<String> failures = new ArrayList<>();
        for (File f : files()) {
            List<String> lines = getLines(f.toPath());
            List<Integer> invalidLines = IntStream.range(0, lines.size())
                .filter(i -> allPatterns.matcher(lines.get(i)).find())
                .boxed()
                .collect(Collectors.toList());

            String path = getProject().getRootProject().getProjectDir().toURI().relativize(f.toURI()).toString();
            failures = invalidLines.stream()
                .map(l -> new AbstractMap.SimpleEntry<>(l+1, lines.get(l)))
                .flatMap(kv -> patterns.entrySet().stream()
                    .filter(p -> Pattern.compile(p.getValue()).matcher(kv.getValue()).find())
                    .map(p -> "- " + p.getKey() + " on line " + kv.getKey() + " of " + path)
                )
                .collect(Collectors.toList());
        }
        if (failures.isEmpty() == false) {
            throw new GradleException("Found invalid patterns:\n" + String.join("\n", failures));
        }

        outputMarker.getParentFile().mkdirs();
        Files.write(outputMarker.toPath(), "done".getBytes(StandardCharsets.UTF_8));
    }

    @OutputFile
    public File getOutputMarker() {
        return outputMarker;
    }

    public void exclude(String... excludes) {
        filesFilter.exclude(excludes);
    }

    public void rule(Map<String,String> props) {
        String name = props.remove("name");
        if (name == null) {
            throw new InvalidUserDataException("Missing [name] for invalid pattern rule");
        }
        String pattern = props.remove("pattern");
        if (pattern == null) {
            throw new InvalidUserDataException("Missing [pattern] for invalid pattern rule");
        }
        if (props.isEmpty() == false) {
            throw new InvalidUserDataException("Unknown arguments for ForbiddenPatterns rule mapping: "
                + props.keySet().toString());
        }
        // TODO: fail if pattern contains a newline, it won't work (currently)
        patterns.put(name, pattern);
    }

    private List<String> getLines(Path path) throws IOException {
        for (Charset encoding : Arrays.asList(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1)) {
            try (Stream<String> stream = Files.lines(path, encoding)) {
                return stream.collect(Collectors.toList());
            } catch (Exception e){
                continue;
            }
        }

        throw new IOException("Unable to read lines from source file: " + path);
    }
}

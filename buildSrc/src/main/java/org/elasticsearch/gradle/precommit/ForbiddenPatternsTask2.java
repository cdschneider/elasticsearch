package org.elasticsearch.gradle.precommit;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;

import java.util.HashMap;
import java.util.Map;

/**
 * Checks for patterns in source files for the project which are forbidden.
 */
public class ForbiddenPatternsTask2 extends DefaultTask {

    /** The rules: a map from the rule name, to a rule regex pattern. */
    private static final Map<String, String> patterns = new HashMap<>();
    static {
        // add mandatory rules
        patterns.put("nocommit", ""); //TODO escape patterns
        patterns.put("nocommit should be all lowercase or all uppercase", "");
        patterns.put("tab", "");
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

        this.getInputs().property("excludes", filesFilter.getExcludes());
        this.getInputs().property("rules", patterns);
    }
}

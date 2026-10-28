import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

/**
 * Fails the build when this project declares a dependency on a project that [isForbidden] matches.
 * Keeps the module graph one-way: features don't depend on features, core never depends on
 * features or the app. See docs/engineering/modules.md.
 */
internal fun Project.forbidProjectDependencies(
    rule: String,
    isForbidden: (path: String) -> Boolean,
) {
    configurations.configureEach {
        dependencies.withType(ProjectDependency::class.java).configureEach {
            if (path != this@forbidProjectDependencies.path && isForbidden(path)) {
                throw GradleException(
                    "${this@forbidProjectDependencies.path} depends on $path, but $rule. " +
                        "See docs/engineering/modules.md.",
                )
            }
        }
    }
}

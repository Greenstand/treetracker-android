import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/** ktlint, detekt and the spotless license header, the same for every module. */
class CodeQualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")
            extensions.configure<KtlintExtension> {
                android.set(true)
                verbose.set(true)
                filter {
                    exclude { element -> element.file.path.contains("generated/") }
                }
            }

            pluginManager.apply("dev.detekt")
            extensions.configure<DetektExtension> {
                config.setFrom(files("${rootDir}/detekt.yml"))
                parallel.set(true)
                buildUponDefaultConfig.set(true)
            }

            pluginManager.apply("com.diffplug.spotless")
            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("**/*.kt")
                    targetExclude("${rootDir}/build-logic/**/*.kt")
                    licenseHeaderFile(
                        rootProject.file("${rootDir}/spotless/copyright.kt"),
                        "^(package|object|import|interface)",
                    )
                }
            }
        }
    }
}

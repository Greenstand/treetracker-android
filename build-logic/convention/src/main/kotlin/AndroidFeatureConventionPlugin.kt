import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * A feature module: an Android library with Compose that builds on core:ui (and through it
 * core:data and core:designsystem), with the usual ViewModel/Koin/test setup. Features don't
 * depend on each other; they navigate with the routes in core:navigation.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("treetracker.android.library")
            pluginManager.apply("treetracker.android.compose")

            forbidProjectDependencies("feature modules can't depend on other feature modules (navigate with the routes in core:navigation)") {
                it.startsWith(":feature:")
            }

            dependencies {
                add("implementation", project(":core:ui"))

                add("implementation", libs.library("androidx-core-ktx"))
                add("implementation", libs.library("koin-android"))
                add("implementation", libs.library("koin-androidx-compose"))
                add("implementation", libs.library("koin-compose-navigation3"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("kotlinx-coroutines-core"))
                add("implementation", libs.library("timber"))

                add("testImplementation", project(":core:testing"))
                add("testImplementation", testFixtures(project(":core:navigation")))
                add("testImplementation", libs.findBundle("testing").get())
                add("testImplementation", libs.library("androidx-arch-core-testing"))
            }
        }
    }
}

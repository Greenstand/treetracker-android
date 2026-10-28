import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("treetracker.code-quality")

            if (path.startsWith(":core:")) {
                forbidProjectDependencies("core modules can't depend on feature modules or the app") {
                    it.startsWith(":feature:") || it == ":app"
                }
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                // The app has beta, dev and prerelease build types as well as debug and release.
                // Libraries declare the same ones so every app variant finds a matching library
                // variant, and core:config can give each one its own values.
                buildTypes.maybeCreate("beta")
                buildTypes.maybeCreate("dev")
                buildTypes.maybeCreate("prerelease")
                testOptions.targetSdk = libs.version("targetSdk").toInt()
                lint.targetSdk = libs.version("targetSdk").toInt()
            }
        }
    }
}

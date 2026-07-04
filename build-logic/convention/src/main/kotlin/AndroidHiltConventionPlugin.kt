import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.google.dagger.hilt.android")
            apply("org.jetbrains.kotlin.kapt")
        }
        dependencies {
            "implementation"(libs.hilt.android)
            "kapt"(libs.hilt.compiler)
        }
    }
}

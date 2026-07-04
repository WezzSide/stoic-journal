import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("stoic.android.library")
            apply("stoic.android.hilt")
        }
        dependencies {
            "implementation"(project(":core:domain"))
            "implementation"(project(":core:common"))
            "implementation"(libs.androidx.lifecycle.viewmodel.compose)
            "implementation"(libs.androidx.lifecycle.runtime.compose)
            "implementation"(libs.androidx.navigation.compose)
            "implementation"(libs.hilt.navigation.compose)
            "implementation"(libs.androidx.ui)
            "implementation"(libs.androidx.ui.graphics)
            "implementation"(libs.androidx.ui.tooling.preview)
            "implementation"(libs.androidx.material3)
        }
    }
}

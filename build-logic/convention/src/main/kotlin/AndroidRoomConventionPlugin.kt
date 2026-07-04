import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.kapt")
        }
        dependencies {
            "implementation"(libs.androidx.room.runtime)
            "implementation"(libs.androidx.room.ktx)
            "kapt"(libs.androidx.room.compiler)
        }
    }
}

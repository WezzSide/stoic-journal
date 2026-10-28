import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            // AGP 9 compiles Kotlin itself (built-in Kotlin); org.jetbrains.kotlin.android is no longer applied.
            apply("com.android.application")
            apply("org.jetbrains.kotlin.plugin.compose")
            apply("stoic.android.hilt")
        }
        extensions.configure<ApplicationExtension> {
            compileSdk = 34
            defaultConfig {
                minSdk = 26
                targetSdk = 34
                versionCode = 1
                versionName = "1.0"
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                // With built-in Kotlin, kotlin.compilerOptions.jvmTarget defaults to targetCompatibility (17).
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            buildFeatures {
                compose = true
            }
        }
    }
}

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            // AGP 9 compiles Kotlin itself (built-in Kotlin); org.jetbrains.kotlin.android is no longer applied.
            apply("com.android.library")
            // KSP replaces kapt (kapt is incompatible with built-in Kotlin). Applied here so modules can
            // declare `ksp(...)` processors directly (e.g. :core:domain, :core:notifications).
            apply("com.google.devtools.ksp")
        }
        extensions.configure<LibraryExtension> {
            compileSdk = 34
            defaultConfig {
                minSdk = 26
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                // With built-in Kotlin, kotlin.compilerOptions.jvmTarget defaults to targetCompatibility (17).
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}

# Build Environment Diagnosis

## Environment Info
- **JDK**: 17
- **Android SDK API**: 34
- **Build Tools**: 34.0.0
- **OS**: Windows (PowerShell/CMD)

## AAPT2 Status
- **Issue**: Some environments report "Timeout waiting to lock journal cache" when running AAPT2 daemon.
- **Diagnosis**: This is typically caused by multiple Gradle instances or file system locks.
- **Workaround Applied**: Force AGP to use the SDK AAPT2 wrapper if running in restricted environments (ARM/Termux).
- **Current Status**: Local build in this environment is SUCCESSFUL.

## Troubleshooting Steps
1. Ensure `local.properties` contains `sdk.dir`.
2. Run `./gradlew clean` if lock issues persist.
3. Check for zombie `gradle` or `java` processes.

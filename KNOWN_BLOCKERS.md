# Known Blockers

## Build Issues
- **AAPT2 Daemon Startup**: Some restricted environments (e.g., Termux, low-RAM VMs) may experience timeouts starting the AAPT2 daemon.
    - **Status**: Workaround documented in `BUILD_ENVIRONMENT_DIAGNOSIS.md`.
- **Gradle Journal Cache Lock**: Occasional build failures due to "Timeout waiting to lock journal cache" when multiple Gradle daemons are active.
    - **Status**: Resolved by running `./gradlew clean` or stopping zombie processes.

## AI Reflection Feature
- **Streaming JSON Parsing**: The current "progressive parsing" uses regex which is fragile for complex nested JSON.
    - **Risk**: Potential for truncated fields if the LLM output is extremely long.
    - **Mitigation**: Switched to a simpler flat JSON structure and robust regex matching.
- **Context Limits**: Large journal entries might exceed the Gemini context window if not summarized.
    - **Status**: Currently taking only the last 3 entries. Future iterations should implement a summarization use case.

## Security
- **API Key Extraction**: The `GEMINI_API_KEY` is currently embedded in the APK via `BuildConfig`.
    - **Risk**: Critical. High-level extraction risk for production.
    - **Status**: Documented as a blocker for non-MVP release in `SECURITY_NOTES.md`.

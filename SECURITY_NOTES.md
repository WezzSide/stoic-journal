# Security Notes

## Gemini API Key Management

The current implementation of the Gemini API key is for **development purposes only**.

### Current Setup
- The key is stored in `local.properties` (gitignored).
- It is injected into the app's `BuildConfig` via the `secrets-gradle-plugin`.

### Production Risks
- **Key Extraction**: API keys embedded in Android APKs can be extracted relatively easily by reverse-engineering the APK. Once extracted, an attacker could use your API quota, potentially leading to unexpected costs or service disruption.
- **Quota Abuse**: Direct client-side API calls are difficult to rate-limit effectively across your user base from the provider's perspective without more complex authentication.

### Recommended Production Architecture
For a production release, it is strongly recommended to use a **Secure API Gateway**:

1.  **Backend Proxy**: Route all Gemini requests through your own backend (e.g., Firebase Functions, Cloudflare Workers, or a dedicated Node.js/Go server).
2.  **Authentication**: The backend should authenticate the mobile app (e.g., using Firebase Auth, App Check, or custom tokens).
3.  **Secret Management**: Store the Gemini API key in a secure environment variable on the server, not in the mobile app.
4.  **Rate Limiting**: Implement user-based rate limiting on your proxy to prevent abuse.

### API Key Restrictions
If you must use an embedded key temporarily:
- **Restrict the Key**: In the Google Cloud Console, restrict the API key to only allow calls to the "Generative Language API" (Gemini).
- **Separate Keys**: Use different keys for development, staging, and production environments.
- **Rotation**: Have a procedure in place to rotate keys if a leak is suspected.

## User Privacy
- Journal entries are stored locally in a Room database.
- AI reflections are cached locally for offline use.
- No user data is currently synced to a cloud service outside of the direct Gemini API calls (which are subject to Google's Generative AI terms of service).

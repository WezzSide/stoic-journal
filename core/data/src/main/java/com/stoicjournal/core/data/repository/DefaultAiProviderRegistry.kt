package com.stoicjournal.core.data.repository

import com.stoicjournal.core.domain.model.AiProvider
import com.stoicjournal.core.domain.model.AiProviderRegistry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAiProviderRegistry @Inject constructor(
    openAiProvider: OpenAiProvider,
    claudeProvider: AnthropicClaudeProvider,
    geminiProvider: GeminiAiProvider,
    fakeProvider: FakeAiProvider
) : AiProviderRegistry {

    private val providers = mutableMapOf<String, AiProvider>()
    private var activeProviderId: String? = null

    init {
        registerProvider(openAiProvider)
        registerProvider(claudeProvider)
        registerProvider(geminiProvider)
        registerProvider(fakeProvider)
    }

    override fun registerProvider(provider: AiProvider) {
        providers[provider.providerId] = provider
    }

    override fun getSupportedProviders(): List<AiProvider> {
        return providers.values.toList()
    }

    override fun getProvider(providerId: String): AiProvider? {
        return providers[providerId]
    }

    override fun getActiveProvider(): AiProvider? {
        return activeProviderId?.let { providers[it] } ?: providers.values.firstOrNull()
    }

    fun setActiveProvider(providerId: String) {
        if (providers.containsKey(providerId)) {
            activeProviderId = providerId
        }
    }
}

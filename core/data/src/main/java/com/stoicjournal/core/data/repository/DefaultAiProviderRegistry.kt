package com.stoicjournal.core.data.repository

import com.stoicjournal.core.domain.model.AiProvider
import com.stoicjournal.core.domain.model.AiProviderRegistry
import com.stoicjournal.core.domain.repository.AiProviderSelectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry of AI providers. The active provider is the one the user explicitly selected in
 * Settings (persisted via [AiProviderSelectionRepository]); until a choice exists the registry
 * falls back to [DEFAULT_PROVIDER_ID]. The test double [FakeAiProvider] is registered so the
 * instrumentation suite can exercise the full UI without network, but it is never chosen
 * implicitly and is excluded from [getSupportedProviders] so it cannot surface in the UI.
 */
@Singleton
class DefaultAiProviderRegistry @Inject constructor(
    openAiProvider: OpenAiProvider,
    claudeProvider: AnthropicClaudeProvider,
    geminiProvider: GeminiAiProvider,
    fakeProvider: FakeAiProvider,
    private val selectionRepository: AiProviderSelectionRepository
) : AiProviderRegistry {

    private val providers = linkedMapOf<String, AiProvider>()

    init {
        registerProvider(openAiProvider)
        registerProvider(claudeProvider)
        registerProvider(geminiProvider)
        registerProvider(fakeProvider)
    }

    override fun registerProvider(provider: AiProvider) {
        providers[provider.providerId] = provider
    }

    override fun getSupportedProviders(): List<AiProvider> =
        providers.values.filter { it.providerId != FakeAiProvider.PROVIDER_ID }

    override fun getProvider(providerId: String): AiProvider? = providers[providerId]

    override suspend fun getActiveProvider(): AiProvider? {
        val selected = selectionRepository.activeProviderId.first()
        return selected?.let { providers[it] } ?: providers[DEFAULT_PROVIDER_ID] ?: getSupportedProviders().firstOrNull()
    }

    override suspend fun setActiveProvider(providerId: String) {
        require(providers.containsKey(providerId)) { "Unknown AI provider: $providerId" }
        selectionRepository.setActiveProviderId(providerId)
    }

    override fun observeActiveProviderId(): Flow<String?> = selectionRepository.activeProviderId

    companion object {
        const val DEFAULT_PROVIDER_ID = "openai"
    }
}

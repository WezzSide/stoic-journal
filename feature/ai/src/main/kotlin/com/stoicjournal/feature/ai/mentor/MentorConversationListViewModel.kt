package com.stoicjournal.feature.ai.mentor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stoicjournal.core.domain.model.MentorConversation
import com.stoicjournal.core.domain.repository.StoicMentorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MentorConversationListViewModel @Inject constructor(
    private val mentorRepository: StoicMentorRepository
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<MentorConversation>>(emptyList())
    val conversations: StateFlow<List<MentorConversation>> = _conversations.asStateFlow()

    init {
        viewModelScope.launch {
            mentorRepository.getAllConversations().collectLatest { list ->
                _conversations.update { list }
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            mentorRepository.deleteConversation(id)
        }
    }
}

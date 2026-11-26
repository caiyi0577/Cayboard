package com.cayboard.service

import com.cayboard.contacts.CandidateRows
import com.cayboard.contacts.ContactCandidates
import com.cayboard.contacts.ContactIndex
import com.cayboard.contacts.ContactItem
import com.cayboard.contacts.ContactSuggestions
import com.cayboard.engine.EngineCandidate
import com.cayboard.engine.EngineProcessResult
import com.cayboard.ui.keyboard.isT9Schema


internal data class CandidateTransformResult(
    val candidates: List<EngineCandidate>,
    val actions: List<CandidateAction>,
)


data class ExtraCandidateSnapshot(
    val text: String,
    val comment: String,
)


data class T9CandidateInjection(
    val anchorEngineIndex: Int?,
    val snapshot: ExtraCandidateSnapshot,
    
    val contactName: Boolean? = null,
)


internal class CandidateTransformCoordinator(private val service: CayboardInputMethodService) {

    
    fun transformFor(result: EngineProcessResult): CandidateTransformResult? {
        if (result.isAsciiMode) return null
        if (result.inputText.isEmpty() && result.candidates.isEmpty()) return null
        return transform(
            inputText = result.inputText,
            preedit = result.preeditText,
            engineCandidates = result.candidates.toList(),
            asciiMode = result.isAsciiMode,
            firstPage = !result.hasPrevPage,
        )
    }

    
    fun transform(
        inputText: String,
        preedit: String,
        engineCandidates: List<EngineCandidate>,
        asciiMode: Boolean,
        firstPage: Boolean = true,
    ): CandidateTransformResult? {
        if (asciiMode) return null
        if (isT9Schema(service.uiState.value.currentSchemaId)) return null
        val state = service.uiState.value
        return glideAlternatives.inject(withContacts(null, inputText, preedit, engineCandidates, firstPage),
            inputText, engineCandidates, firstPage, state.currentSchemaId, state.inputSessionId)
    }

    
    val glideAlternatives = GlideAlternatives()

    
    private fun withContacts(
        transformed: CandidateTransformResult?,
        inputText: String,
        preedit: String,
        engineCandidates: List<EngineCandidate>,
        firstPage: Boolean,
    ): CandidateTransformResult? {
        val items = contactItems(inputText, preedit, firstPage, t9 = false)
        if (items.isEmpty()) return transformed
        val base = transformed?.candidates ?: engineCandidates
        val merged = ContactCandidates.merge(
            CandidateRows(base.map { it.text }, base.map { it.comment }, transformed?.actions.orEmpty()), items,
        )
        val candidates = merged.texts.indices.map { i ->
            val from = merged.origin[i]
            val original = base.getOrNull(from)
            when {
                original == null -> EngineCandidate(merged.texts[i], merged.comments[i])
                original.comment == merged.comments[i] -> original
                else -> original.copy(comment = merged.comments[i])
            }
        }
        return CandidateTransformResult(candidates, merged.actions)
    }

    private fun contactItems(inputText: String, preedit: String, firstPage: Boolean, t9: Boolean): List<ContactItem> {
        if (!firstPage || !ContactSuggestions.isActive || inputText.isEmpty()) return emptyList()
        // 密码框（含终端）不给。
        if (service.isSecretEditor()) return emptyList()
        val profile = service.uiState.value.inputProfile
        if (profile.language != com.cayboard.settings.InputLanguage.CHINESE ||
            profile.mode != com.cayboard.settings.InputMode.KEYBOARD) return emptyList()
        val letters = ContactIndex.lettersOf(preedit) ?: ContactIndex.lettersOf(inputText)
        val digits = if (t9) ContactIndex.digitsOf(inputText) else null
        return ContactSuggestions.itemsFor(letters, digits)
    }

    


    
    fun transformForT9(result: EngineProcessResult): List<T9CandidateInjection>? {
        if (result.isAsciiMode) return null
        if (result.inputText.isEmpty() && result.candidates.isEmpty()) return null
        val contacts = contactItems(result.inputText, result.preeditText, !result.hasPrevPage, t9 = true).map {
            T9CandidateInjection(null, ExtraCandidateSnapshot(it.text, it.comment), contactName = it.isName)
        }
        return contacts.takeIf { it.isNotEmpty() }
    }



    


    

}

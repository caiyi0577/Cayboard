package com.cayboard.service

internal object OcrDraftTransitions {
    fun open(state:InputUIState,text:String)=state.copy(showOcrDraft=true,ocrDraftFocused=true,ocrDraftText=text,enterKeyText="换行")
    fun close(state:InputUIState,enterKeyText:String)=state.copy(showOcrDraft=false,ocrDraftFocused=false,ocrDraftText="",enterKeyText=enterKeyText)
    fun textFocus(state:InputUIState,focused:Boolean)=state.copy(ocrDraftFocused=focused,enterKeyText="换行")
}
internal fun CayboardInputMethodService.openOcrDraft(text:String) {
    closeClipboardSearch(openBoard=false);closeToolPanel();keyboardViewModel.closeOverlay()
    uiState.value=OcrDraftTransitions.open(uiState.value,text)
}
internal fun CayboardInputMethodService.closeOcrDraft() {
    if(!uiState.value.showOcrDraft && !uiState.value.ocrDraftFocused) return
    uiState.value=OcrDraftTransitions.close(uiState.value,editorEnterKeyText())
    OcrDraftEditTextHolder.editText=null
}

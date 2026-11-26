package com.cayboard.service

import com.cayboard.engine.InputEngine
import com.cayboard.settings.InputModes
import com.cayboard.settings.LanguagePreferences
import com.cayboard.settings.OrientationLayouts
import com.cayboard.settings.SchemaManager
import com.cayboard.settings.SettingsPreferences
import com.cayboard.ui.keyboard.isHandwritingSchema
import com.cayboard.util.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Keep focus callbacks responsive and serialize dictionary work with subsequent typing. */
internal class EditorInputPreparation(private val service: CayboardInputMethodService) {
    fun schedule(restarting: Boolean, numberEditor: Boolean) {
        if (!InputEngine.isInitialized() || service.uiState.value.isDeploying) return
        service.keyRouter.postEngineJob {
            val owner = InputCommandOwner.requireOwner()
            val engine = service.inputEngine
            if (engine.isMaintaining()) return@postEngineJob
            val available = engine.getAvailableSchemas().toList()
            val saved = SettingsPreferences.getCurrentSchema(service)
            // 横竖屏分开记布局：这个方向记的条目（横屏 26 键、竖屏九键），关着时就是 saved。
            val stored = if (service.editorInputPolicy.scoped) saved
                else OrientationLayouts.prepared(service, saved, InputModes.available(service.uiState.value.schemas))
            val selectable = selectableWithStored(stored, available, { SchemaManager.getEnabledSchemas(service) },
                ::isHandwritingSchema) { SchemaManager.isSchemaCompiled(service, it) }
            val policy = service.editorInputPolicy
            var target = policy.schemaOverride ?: LanguagePreferences.nativeSchema(stored, selectable, LanguagePreferences.enabled(service))
            if (policy.directInput && isHandwritingSchema(target)) {
                target = engine.getCurrentSchema().takeIf { it in available } ?: available.firstOrNull() ?: return@postEngineJob
            }
            var missingHandwriting = false
            if (isHandwritingSchema(target)) {
                com.cayboard.model.ModelStorage.migrateLegacyForModel(service, "ochwpro")
                val directory = com.cayboard.model.ModelStorage.getModelDir(service, "ochwpro")
                missingHandwriting = !java.io.File(directory, "ochwpro.onnx").isFile ||
                    !java.io.File(directory, "char_index.json").isFile
                if (missingHandwriting) target = available.firstOrNull() ?: return@postEngineJob
            }
            val handwriting = isHandwritingSchema(target)
            if (!handwriting && target !in available && !SchemaManager.isSchemaCompiled(service, target)) {
                target = available.firstOrNull() ?: return@postEngineJob
            }
            val current = engine.getCurrentSchema()
            val nativeTarget = if (handwriting) current else target
            if (nativeTarget.isEmpty()) return@postEngineJob
            service.schemaController.applyPageSizeSetting(nativeTarget)
            if (!prepareEditorEngine(current, nativeTarget, engine::switchSchema,
                    { engine.clearQueuedComposition(); engine.clearQueuedT9Composition() },
                    service.sessionController::restorePersistedSchemaOptions)) {
                FileLogger.w("InputLifecycle", "Editor preparation failed: schema unavailable")
                return@postEngineJob
            }
            val ascii = engine.isAsciiMode()
            withContext(Dispatchers.Main) {
                owner.requireCurrent(service.inputReadiness, service.uiState.value.inputSessionId)
                if (target != saved && !policy.scoped) SettingsPreferences.setCurrentSchema(service, target)
                // 换了方向的条目时，该语言的选中条目也跟着换，语言键切回来、菜单里打勾的都是它。
                if (stored != saved && target == stored && !policy.scoped) InputModes.selectProfile(service, target,
                    InputModes.languageOf(target, service.uiState.value.schemas))
                val refreshName = service.uiState.value.currentSchemaId != target || service.uiState.value.schemas.isEmpty()
                service.uiState.value = service.uiState.value.copy(currentSchemaId = target, isAsciiMode = ascii)
                if (policy.scoped && !handwriting) {
                    service.keyboardViewModel.discardTemporaryHandwriting()
                    service.keyboardViewModel.switchMain(com.cayboard.keyboard.MainType.FULL)
                }
                service.keyboardViewModel.applyPreparedEditorKeyboard(ascii, target, handwriting,
                    restarting && !policy.scoped, policy.field.numberPanel ||
                        numberEditor && SettingsPreferences.isAutoNumberKeyboardEnabled(service))
                service.keyboardViewModel.synchronizeInputSession(service.uiState.value.inputSessionId,
                    service.uiState.value.t9ResetSignal, ascii, target)
                policy.completePreparation(ascii, target)
                if (refreshName) service.sessionController.updateSchemaName()
                else service.sessionController.refreshSchemaSwitches()
                if (missingHandwriting) android.widget.Toast.makeText(service, "请先下载手写模型", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}

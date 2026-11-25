package com.cayboard.rime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** One accepted T9 choice fans out to the fast and durable learning layers. */
internal class T9LearningCoordinator(
    private val engine: RimeEngine,
    private val scope: CoroutineScope,
) {
    fun enqueue(input: String, pinyin: String, text: String) {
        if (input.isBlank() || pinyin.isBlank() || text.isBlank()) return
        // The caller invokes this only after the host accepted the commit. Keep the
        // durable write off the input coroutine, but serialize native access through
        // RimeEngine's lock so it cannot race a schema switch or deployment.
        scope.launch(Dispatchers.IO) {
            engine.t9Memorize(canonicalT9Input(input), text, pinyin)
        }
    }
}

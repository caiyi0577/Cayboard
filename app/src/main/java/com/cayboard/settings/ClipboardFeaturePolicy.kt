package com.cayboard.settings

/** Fixed local-only clipboard. Legacy connection preferences cannot enable transports. */
internal object ClipboardFeaturePolicy {
    const val CONNECTIONS_ENABLED = false
}

package com.cayboard.runtime.adaptation

/** Monotonic timestamps; null means unavailable, never a zero-cost/high-performance device. */
data class DeviceCapabilities(
    val sampledAtMs: Long,
    val sdk: Int,
    val abis: List<String>,
    val lowRam: Boolean?,
    val totalMemoryBytes: Long?,
    val processors: Int?,
    /** Hardware maximum, not the current/governor-limited frequency. Unknown when unreadable. */
    val maxCpuKHz: Long? = null,
    /** 本机 CPU 实测（CPU measurement）；没测过为 null。档位靠它，不靠主频（[PerformanceTiers]）。 */
    val cpu: CpuMeasurement? = null,
) {
    val memoryEstimate: MemoryEstimate get() = when {
        lowRam == true || totalMemoryBytes?.let { it in 1 until 4 * GIB } == true -> MemoryEstimate.CONSERVATIVE
        lowRam == null || totalMemoryBytes == null || totalMemoryBytes <= 0 -> MemoryEstimate.UNKNOWN
        totalMemoryBytes < 8 * GIB -> MemoryEstimate.STANDARD
        else -> MemoryEstimate.MEASUREMENT_ELIGIBLE
    }

    companion object { const val GIB = 1024L * 1024 * 1024 }
}

enum class MemoryEstimate { UNKNOWN, CONSERVATIVE, STANDARD, MEASUREMENT_ELIGIBLE }

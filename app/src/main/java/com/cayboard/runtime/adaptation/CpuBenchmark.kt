package com.cayboard.runtime.adaptation

/**
 * 一次 CPU 实测：同一段固定计算（app/src/main/jni/cpu_bench.cc）一份单独跑、两份同时跑，各自跑完一遍的中位耗时。
 * 越快越强。[version] 跟着计算量走，计算量一改旧结果就作废。
 */
data class CpuMeasurement(val singleNs: Long, val dualNs: Long, val version: Int = 1)


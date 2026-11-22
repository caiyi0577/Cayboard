package com.cayboard.update

object AppVersion {
    fun parseVersion(text: String): List<Int> =
        Regex("""^v?(\d+(?:\.\d+)*)""").find(text.trim())?.groupValues?.get(1)
            ?.split('.')?.map { it.toIntOrNull() ?: 0 } ?: emptyList()

    fun compareVersions(a: String, b: String): Int {
        val x = parseVersion(a)
        val y = parseVersion(b)
        for (i in 0 until maxOf(x.size, y.size)) {
            val diff = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    fun isNewer(candidate: String, current: String): Boolean =
        parseVersion(candidate).isNotEmpty() && compareVersions(candidate, current) > 0

}

package lt.tbu.a9.search

/** User history for ranking. */
data class UsageInfo(val launchCount: Int = 0, val lastUsedMs: Long = 0L, val pinned: Boolean = false)

/** Search by linear scan (enough for thousands of entries, <1 ms) and ranking. */
class SearchEngine {
    @Volatile private var entries: List<SearchEntry> = emptyList()

    fun setEntries(list: List<SearchEntry>) { entries = list }

    /** Returns ids by ranking. An empty query → by usage only. */
    fun search(rawQuery: String, usage: (String) -> UsageInfo, now: Long = System.currentTimeMillis(), limit: Int = 60): List<String> {
        val q = T9Map.normalize(rawQuery).filter { !it.isWhitespace() }
        val scored = ArrayList<Triple<String, Int, String>>()
        for (e in entries) {
            val m = if (q.isEmpty()) 1 else matchScore(q, e)
            if (m > 0) scored += Triple(e.id, m + usageScore(usage(e.id), now), e.label.lowercase())
        }
        scored.sortWith(compareByDescending<Triple<String, Int, String>> { it.second }.thenBy { it.third }.thenBy { it.first })
        return scored.take(limit).map { it.first }
    }

    companion object {
        fun usageScore(u: UsageInfo, now: Long): Int {
            var s = if (u.pinned) 5000 else 0
            s += minOf(u.launchCount, 50) * 6
            if (u.launchCount > 0 && u.lastUsedMs > 0) {
                val days = ((now - u.lastUsedMs) / 86_400_000L).toInt()
                s += maxOf(0, 30 - days) * 3
            }
            return s
        }

        /** 0 – no match. [q] is already normalized and without spaces. */
        fun matchScore(q: String, e: SearchEntry): Int {
            val digitsMode = q.all { it.isDigit() }
            var best = 0
            for (c in e.candidates) {
                val hay = if (digitsMode) c.digits else c.text
                if (hay.isEmpty()) continue
                val score = when {
                    hay.startsWith(q) -> c.kind.base + q.length * 100 / hay.length
                    c.kind == MatchKind.FULL && hay.contains(q) -> MatchKind.SUBSTRING.base
                    c.kind == MatchKind.PHONE && hay.contains(q) -> 250
                    else -> 0
                }
                if (score > best) best = score
            }
            return best
        }
    }
}

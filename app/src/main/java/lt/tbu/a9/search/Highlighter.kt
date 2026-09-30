package lt.tbu.a9.search

/**
 * Determines which characters of the name matched the query (with the same priority as search):
 * start prefix → word prefix → initials → anywhere in the name.
 * Returns the character indices in the original text [label].
 */
object Highlighter {
    fun matchIndices(label: String, rawQuery: String): Set<Int> {
        val q = T9Map.normalize(rawQuery).filter { !it.isWhitespace() }
        if (q.isEmpty()) return emptySet()
        val digits = q.all { it.isDigit() }

        val idx = ArrayList<Int>()          // character index in the original text
        val ch = ArrayList<Char>()          // normalized character
        val wordStart = ArrayList<Int>()    // word start positions in the ch list
        var prevAlnum = false
        var prevLower = false
        label.forEachIndexed { i, c ->
            if (!c.isLetterOrDigit()) {
                prevAlnum = false; prevLower = false
                return@forEachIndexed
            }
            val n = T9Map.normalize(c.toString()).firstOrNull() ?: return@forEachIndexed
            if (!prevAlnum || (prevLower && c.isUpperCase())) wordStart += ch.size
            idx += i; ch += n
            prevAlnum = true
            prevLower = c.isLowerCase() || c.isDigit()
        }

        fun key(x: Char): Char = if (digits) T9Map.digitOf(x) ?: x else x
        fun matchAt(pos: Int) = pos + q.length <= ch.size && q.indices.all { key(ch[pos + it]) == q[it] }
        fun range(pos: Int): Set<Int> = q.indices.map { idx[pos + it] }.toSet()

        if (matchAt(0)) return range(0)
        for (k in 1 until wordStart.size) {
            val start = wordStart[k]
            val end = wordStart.getOrElse(k + 1) { ch.size }
            if (start + q.length <= end && matchAt(start)) return range(start)
        }
        if (q.length <= wordStart.size && q.indices.all { key(ch[wordStart[it]]) == q[it] }) {
            return q.indices.map { idx[wordStart[it]] }.toSet()
        }
        for (p in 0..(ch.size - q.length)) if (matchAt(p)) return range(p)
        return emptySet()
    }
}

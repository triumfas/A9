package lt.tbu.a9.search

/**
 * Nustato, kurie pavadinimo simboliai atitiko užklausą (tuo pačiu prioritetu kaip paieška):
 * pradžios prefiksas → žodžio prefiksas → inicialai → bet kur pavadinime.
 * Grąžina simbolių indeksus pradiniame tekste [label].
 */
object Highlighter {
    fun matchIndices(label: String, rawQuery: String): Set<Int> {
        val q = T9Map.normalize(rawQuery).filter { !it.isWhitespace() }
        if (q.isEmpty()) return emptySet()
        val digits = q.all { it.isDigit() }

        val idx = ArrayList<Int>()          // simbolio indeksas pradiniame tekste
        val ch = ArrayList<Char>()          // normalizuotas simbolis
        val wordStart = ArrayList<Int>()    // žodžių pradžios pozicijos ch sąraše
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

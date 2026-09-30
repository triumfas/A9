package lt.tbu.a9.search

import java.text.Normalizer

/** Letter → T9 digit and text normalization (no diacritics, lowercase). */
object T9Map {
    private val keyLetters = arrayOf("abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz")
    private val digitByChar: Map<Char, Char> = buildMap {
        keyLetters.forEachIndexed { i, letters -> letters.forEach { put(it, '2' + i) } }
    }
    private val special = mapOf('ł' to 'l', 'ø' to 'o', 'đ' to 'd', 'ß' to 's', 'æ' to 'a', 'œ' to 'o', 'ı' to 'i')
    private val marks = Regex("\\p{Mn}+")

    fun lettersOf(digit: Char): String = if (digit in '2'..'9') keyLetters[digit - '2'] else ""

    /** Lowercase, without diacritics (ą→a, š→s). Letters of other alphabets stay as they are. */
    fun normalize(s: String): String {
        val d = Normalizer.normalize(s, Normalizer.Form.NFD).replace(marks, "").lowercase()
        return buildString(d.length) { d.forEach { append(special[it] ?: it) } }
    }

    fun digitOf(c: Char): Char? = if (c in '0'..'9') c else digitByChar[c]

    /** T9 code of normalized text; characters without a mapping are skipped. */
    fun toDigits(normalized: String): String = buildString(normalized.length) {
        normalized.forEach { c -> digitOf(c)?.let { append(it) } }
    }
}

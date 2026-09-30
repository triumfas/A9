package lt.tbu.a9.search

enum class MatchKind(val base: Int) { FULL(1000), PHONE(900), WORD(700), INITIALS(600), PACKAGE(300), SUBSTRING(200) }

/** Ieškomas fragmentas: [text] – normalizuotas, [digits] – jo T9 kodas. */
class Candidate(val kind: MatchKind, val text: String) {
    val digits: String = T9Map.toDigits(text)
}

class SearchEntry(val id: String, val candidates: List<Candidate>, val label: String = "")

/** Iš pavadinimo / paketo / telefonų sudaro ieškomus fragmentus. */
object Tokenizer {
    private val camel = Regex("(?<=[\\p{Ll}\\d])(?=\\p{Lu})")
    private val nonAlnum = Regex("[^\\p{L}\\p{N}]+")
    private val ignoredPackageParts = setOf("com", "org", "net", "android", "app", "apps", "google", "www")

    fun words(label: String): List<String> =
        label.split(nonAlnum).filter { it.isNotEmpty() }
            .flatMap { it.split(camel) }.map(T9Map::normalize).filter { it.isNotEmpty() }

    fun forApp(label: String, packageName: String): List<Candidate> {
        val list = ArrayList<Candidate>()
        addNameCandidates(list, words(label))
        packageName.split('.').map(T9Map::normalize)
            .filter { it.length > 2 && it !in ignoredPackageParts }
            .forEach { list += Candidate(MatchKind.PACKAGE, it) }
        return list
    }

    fun forContact(name: String, phones: List<String>): List<Candidate> {
        val list = ArrayList<Candidate>()
        addNameCandidates(list, words(name))
        phones.forEach { p ->
            val d = p.filter { it.isDigit() }
            if (d.isNotEmpty()) list += Candidate(MatchKind.PHONE, d)
        }
        return list
    }

    private fun addNameCandidates(out: MutableList<Candidate>, w: List<String>) {
        if (w.isEmpty()) return
        out += Candidate(MatchKind.FULL, w.joinToString(""))
        w.drop(1).forEach { out += Candidate(MatchKind.WORD, it) }
        if (w.size > 1) out += Candidate(MatchKind.INITIALS, w.map { it.first() }.joinToString(""))
    }
}

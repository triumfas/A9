package lt.tbu.a9.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineTest {
    private fun app(id: String, label: String, pkg: String) = SearchEntry(id, Tokenizer.forApp(label, pkg))

    private val engine = SearchEngine().apply {
        setEntries(listOf(
            app("fb", "Facebook", "com.facebook.katana"),
            app("maps", "Google Maps", "com.google.android.apps.maps"),
            app("yt", "YouTube", "com.google.android.youtube"),
            app("sv", "Šviesa", "lt.test.sviesa"),
            app("cal", "Calendar", "com.calendar"),
        ))
    }
    private val none = { _: String -> UsageInfo() }

    @Test fun t9Digits() = assertEquals("32", T9Map.toDigits("fb"))
    @Test fun diacritics() = assertEquals("sviesa", T9Map.normalize("Šviesa"))
    @Test fun t9FindsFacebook() = assertEquals("fb", engine.search("32", none).first())
    @Test fun lettersFindFacebook() = assertEquals("fb", engine.search("face", none).first())
    @Test fun initials() = assertEquals("maps", engine.search("gm", none).first())
    @Test fun wordPrefix() = assertTrue("maps" in engine.search("map", none))
    @Test fun lithuanianT9() = assertEquals("sv", engine.search("78", none).first())
    @Test fun camelCaseWord() = assertTrue("yt" in engine.search("tube", none))
    @Test fun noMatch() = assertTrue(engine.search("zzzz", none).isEmpty())

    @Test fun usageBreaksTies() {
        // „g“ atitinka ir Google Maps, ir YouTube (per paketą? ne) – tikrinam, kad dažniau naudojama eina pirmiau
        val usage = { id: String -> if (id == "cal") UsageInfo(launchCount = 20, lastUsedMs = System.currentTimeMillis()) else UsageInfo() }
        val r = engine.search("c", usage)
        assertEquals("cal", r.first())
    }

    @Test fun emptyQueryOrdersByUsage() {
        val usage = { id: String -> if (id == "cal") UsageInfo(launchCount = 5, lastUsedMs = System.currentTimeMillis()) else UsageInfo() }
        assertEquals("cal", engine.search("", usage).first())
    }

    @Test fun pinnedFirst() {
        val usage = { id: String -> if (id == "yt") UsageInfo(pinned = true) else UsageInfo() }
        assertEquals("yt", engine.search("", usage).first())
    }
}

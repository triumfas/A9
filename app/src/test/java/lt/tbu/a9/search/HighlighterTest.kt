package lt.tbu.a9.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlighterTest {
    @Test fun prefixByLetters() = assertEquals(setOf(0, 1, 2), Highlighter.matchIndices("Google Maps", "goo"))
    @Test fun prefixByDigits() = assertEquals(setOf(0, 1, 2), Highlighter.matchIndices("Google Maps", "466"))
    @Test fun wordPrefix() = assertEquals(setOf(7, 8), Highlighter.matchIndices("Google Maps", "62"))
    @Test fun initials() = assertEquals(setOf(0, 7), Highlighter.matchIndices("Google Maps", "gm"))
    @Test fun diacritics() = assertEquals(setOf(0, 1), Highlighter.matchIndices("Šviesa", "78"))
    @Test fun camelCaseWord() = assertEquals(setOf(3, 4, 5, 6), Highlighter.matchIndices("YouTube", "tube"))
    @Test fun substring() = assertEquals(setOf(2, 3), Highlighter.matchIndices("Facebook", "ce"))
    @Test fun noMatch() = assertTrue(Highlighter.matchIndices("Facebook", "zzz").isEmpty())
    @Test fun emptyQuery() = assertTrue(Highlighter.matchIndices("Facebook", "").isEmpty())
}

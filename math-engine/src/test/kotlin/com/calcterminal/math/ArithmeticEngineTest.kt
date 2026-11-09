package com.calcterminal.math
import org.junit.Assert.*
import org.junit.Test
class ArithmeticEngineTest {
    private val engine = ArithmeticEngine()
    private fun result(s: String) = engine.evaluate(s).text
    @Test fun decimalsAreExact() {
        assertEquals("0.3", result("0.1+0.2"))
        assertEquals("1000000000000000000000000", result("999999999999999999999999+1"))
    }
    @Test fun precedenceAndSigns() {
        assertEquals("14", result("2+3*4")); assertEquals("-20", result("-(2+3)*4"))
        assertEquals("6", result("2*-(-3)"))
    }
    @Test fun percentSemantics() {
        assertEquals("200.1", result("200+10%")); assertEquals("20", result("200*10%"))
    }
    @Test fun roundingIsExplicit() {
        assertTrue(engine.evaluate("1/3").approximate); assertFalse(engine.evaluate("1/8").approximate)
    }
    @Test fun powersRespectPrecedence() {
        assertEquals("512", result("2^3^2"))
        assertEquals("-4", result("-2^2"))
        assertEquals("4", result("(-2)^2"))
        assertEquals("0.125", result("2^-3"))
        assertEquals("9", result("2*3^2/2"))
        assertTrue(engine.evaluate("3^-1").approximate)
    }
    @Test fun invalidPowersAreBounded() {
        listOf("0^0", "0^-1", "2^0.5", "2^101", "2^99999999999", "2^100^100").forEach {
            try { engine.evaluate(it); fail(it) } catch (_: IllegalArgumentException) {}
        }
    }
    @Test fun decimalAndPercentEdges() {
        assertEquals("0.5", result(" .5 + .25 * 0 "))
        assertEquals("0.0001", result("1%%"))
        assertEquals("-2", result("6 / -3"))
        assertEquals("1", result("10^0"))
    }
    @Test fun rejectsInvalidInput() {
        listOf("1/0", "(2+3", "2..3", "2(3)", "sin(2)", "", "1+",
            "(".repeat(70)+"1"+")".repeat(70)).forEach {
            try { engine.evaluate(it); fail(it) } catch (_: IllegalArgumentException) {}
        }
    }
}

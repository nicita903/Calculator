package com.calcterminal.math

import java.math.BigDecimal
import java.math.MathContext

/** Percent always divides the preceding value by 100. No implicit multiplication. */
class ArithmeticEngine {
    data class Answer(val value: BigDecimal, val approximate: Boolean) {
        val text: String get() = value.stripTrailingZeros().toPlainString()
    }
    fun evaluate(expression: String): Answer {
        require(expression.length <= 512) { "Expression too long (512 characters maximum)" }
        return Parser(expression.replace('×', '*').replace('÷', '/').replace('−', '-')).parse()
    }
    private class Parser(val source: String) {
        var pos = 0
        var depth = 0
        var approximate = false
        fun parse(): Answer {
            val value = sum()
            space()
            require(pos == source.length) { "Unexpected character at position ${pos + 1}" }
            return Answer(value, approximate)
        }
        fun space() { while (pos < source.length && source[pos].isWhitespace()) pos++ }
        fun take(c: Char): Boolean {
            space()
            return if (pos < source.length && source[pos] == c) { pos++; true } else false
        }
        fun sum(): BigDecimal {
            var v = product()
            while (true) v = when { take('+') -> v + product(); take('-') -> v - product(); else -> return v }
        }
        fun product(): BigDecimal {
            var v = unary()
            while (true) v = when {
                take('*') -> v * unary()
                take('/') -> {
                    val divisor = unary()
                    require(divisor.signum() != 0) { "Cannot divide by zero" }
                    try { v.divide(divisor) } catch (_: ArithmeticException) {
                        approximate = true
                        v.divide(divisor, MathContext.DECIMAL128)
                    }
                }
                else -> return v
            }
        }
        fun unary(): BigDecimal {
            require(++depth <= 64) { "Expression nesting limit reached" }
            val value = when { take('+') -> unary(); take('-') -> -unary(); else -> power() }
            depth--
            return value
        }
        // Right associative; unary minus binds less tightly than exponentiation.
        fun power(): BigDecimal {
            val base = atom()
            if (!take('^')) return base
            val exponent = try { unary().intValueExact() } catch (_: ArithmeticException) {
                throw IllegalArgumentException("Only integer powers are supported")
            }
            require(exponent in -100..100) { "Power must be between -100 and 100" }
            require(base.signum() != 0 || exponent > 0) { "Zero requires a positive power" }
            val magnitude = kotlin.math.abs(exponent)
            require(base.precision().toLong() * magnitude <= 4096 &&
                kotlin.math.abs(base.scale().toLong()) * magnitude <= 4096) { "Power result is too large" }
            val value = base.pow(magnitude)
            if (exponent >= 0) return value
            return try { BigDecimal.ONE.divide(value) } catch (_: ArithmeticException) {
                approximate = true
                BigDecimal.ONE.divide(value, MathContext.DECIMAL128)
            }
        }
        fun atom(): BigDecimal {
            var value = if (take('(')) {
                val v = sum(); require(take(')')) { "Missing closing parenthesis" }; v
            } else {
                space(); val start = pos
                while (pos < source.length && (source[pos] in '0'..'9' || source[pos] == '.')) pos++
                require(pos > start) { "Enter a number at position ${pos + 1}" }
                source.substring(start, pos).toBigDecimalOrNull()
                    ?: throw IllegalArgumentException("Invalid decimal number")
            }
            while (take('%')) value = value.movePointLeft(2)
            return value
        }
    }
}

package com.example.util

import kotlin.math.*

object MathParser {
    fun eval(str: String, xVal: Double = 0.0, isDegreeMode: Boolean = true): Double {
        // Clean and prepare string
        var cleaned = str.replace("×", "*")
            .replace("÷", "/")
            .replace("π", "pi")
            .replace("X", "x")
            .replace("√", "sqrt")
            .replace("ctg", "cot")
            .lowercase()

        // Auto-balance open parentheses by appending matching close parentheses
        val openCount = cleaned.count { it == '(' }
        val closeCount = cleaned.count { it == ')' }
        if (openCount > closeCount) {
            cleaned += ")".repeat(openCount - closeCount)
        }

        val sanitized = cleaned

        if (sanitized.isBlank()) return 0.0

        return object {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < sanitized.length) sanitized[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < sanitized.length) throw RuntimeException("Неожиданный символ: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else break
                }
                return x
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) {
                        val d = parseFactor()
                        if (d == 0.0) throw ArithmeticException("Деление на ноль")
                        x /= d
                    } else if (ch == '('.code || (ch >= '0'.code && ch <= '9'.code) || ch == '.'.code || (ch >= 'a'.code && ch <= 'z'.code)) {
                        // Implicit multiplication when followed by a factor (parenthesis, digit, constant, or function)
                        x *= parseFactor()
                    } else break
                }
                return x
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = this.pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = sanitized.substring(startPos, this.pos).toDouble()
                } else if ((ch >= 'a'.code && ch <= 'z'.code)) {
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val name = sanitized.substring(startPos, this.pos)
                    if (name == "x") {
                        x = xVal
                    } else if (name == "pi") {
                        x = Math.PI
                    } else if (name == "e") {
                        x = Math.E
                    } else {
                        // Function call
                        val hasParen = eat('('.code)
                        val arg = parseExpression()
                        if (hasParen) {
                            eat(')'.code)
                        }

                        // Apply degree convert if isDegreeMode is true and it's trig
                        val trigArg = if (isDegreeMode && (name == "sin" || name == "cos" || name == "tan" || name == "cot" || name == "ctg")) {
                            arg * Math.PI / 180.0
                        } else {
                            arg
                        }

                        x = when (name) {
                            "sin" -> sin(trigArg)
                            "cos" -> cos(trigArg)
                            "tan" -> {
                                val t = tan(trigArg)
                                if (abs(t) > 1e14) Double.NaN else t
                            }
                            "cot", "ctg" -> {
                                val t = tan(trigArg)
                                if (abs(t) < 1e-14) throw ArithmeticException("Котангенс не определен")
                                1.0 / t
                            }
                            "sqrt" -> {
                                if (arg < 0) throw ArithmeticException("Квадратный корень из отриц. числа")
                                sqrt(arg)
                            }
                            "ln" -> {
                                if (arg <= 0) throw ArithmeticException("ln от неположительного числа")
                                ln(arg)
                            }
                            "log" -> {
                                if (arg <= 0) throw ArithmeticException("log от неположительного числа")
                                log10(arg)
                            }
                            "abs" -> abs(arg)
                            else -> throw RuntimeException("Неизвестная функция: $name")
                        }
                    }
                } else {
                    throw RuntimeException("Неожиданный символ: " + ch.toChar())
                }

                if (eat('^'.code)) x = x.pow(parseFactor())

                // Check for factorial or percentage trailing suffixes
                while (true) {
                    if (eat('!'.code)) {
                        x = factorial(x)
                    } else if (eat('%'.code)) {
                        x = x / 100.0
                    } else {
                        break
                    }
                }

                return x
            }

            fun factorial(n: Double): Double {
                val value = n.toInt()
                if (value < 0 || n != value.toDouble()) return Double.NaN
                var fact = 1.0
                for (i in 1..value) {
                    fact *= i
                }
                return fact
            }
        }.parse()
    }
}

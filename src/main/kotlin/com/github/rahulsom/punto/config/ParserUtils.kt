package com.github.rahulsom.punto.config

import com.github.rahulsom.punto.config.PadMode.Both
import com.github.rahulsom.punto.config.PadMode.Left
import com.github.rahulsom.punto.config.PadMode.Right
import org.jparsec.Parser
import org.jparsec.Parsers
import org.jparsec.Scanners

enum class PadMode { Left, Right, Both }

fun <T> Parser<T>.pad(mode: PadMode = Both): Parser<T> =
    Scanners.isChar { "\n\r\t ".contains(it) }
        .many()
        .let {
            when (mode) {
                Both -> Parsers.sequence(it, this, it) { _, a, _ -> a }
                Left -> Parsers.sequence(it, this) { _, a -> a }
                Right -> Parsers.sequence(this, it) { a, _ -> a }
            }
        }

fun <T> Parser<T>.paren(): Parser<T> =
    Parsers.sequence(Scanners.isChar('('), this, Scanners.isChar(')')) { _, a, _ -> a }
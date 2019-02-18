package com.github.rahulsom.punto.utils

sealed class StatusLine {
    data class Unstaged(val file: String) : StatusLine()
    data class Modified(val file: String) : StatusLine()
    object Error : StatusLine()

    companion object {

        private val parsers =
            mutableMapOf<Regex, (MatchResult.Destructured) -> StatusLine>()

        init {
            parsers[Regex(" M (.+)")] = { Modified(it.component1()) }
            parsers[Regex("\\?\\? (.+)")] = { Unstaged(it.component1()) }
        }

        fun parse(line: String) =
            parsers.keys
                .find { it.matches(line) }
                ?.let { regex ->
                    parsers[regex]?.let { f ->
                        regex.matchEntire(line)?.destructured?.let { d ->
                            f(d)
                        } ?: Error
                    }
                } ?: Error
    }

}

operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)

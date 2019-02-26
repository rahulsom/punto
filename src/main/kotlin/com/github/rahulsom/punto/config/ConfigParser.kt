package com.github.rahulsom.punto.config

import com.github.rahulsom.punto.config.PadMode.Left
import com.github.rahulsom.punto.config.PadMode.Right
import org.jparsec.Parser
import org.jparsec.Parsers.EOF
import org.jparsec.Parsers.longest
import org.jparsec.Parsers.or
import org.jparsec.Parsers.sequence
import org.jparsec.Scanners.DOUBLE_QUOTE_STRING
import org.jparsec.Scanners.SINGLE_QUOTE_STRING
import org.jparsec.Scanners.isChar
import org.jparsec.Scanners.string
import org.jparsec.Scanners.WHITESPACES as WS

object ConfigParser {
    private val STRING =
        or(SINGLE_QUOTE_STRING, DOUBLE_QUOTE_STRING)
    private val NL =
        isChar { "\n\r".contains(it) }.many1()
    private val STRINGS =
        STRING.sepBy1(isChar(',').pad(Right))

    @JvmStatic
    val userHome: Parser<PuntoConfig> =
        methodMultiArg("userHome") { v -> PuntoConfig().also { it.userHome = v[0] } }

    @JvmStatic
    val puntoHome: Parser<PuntoConfig> =
        methodMultiArg("puntoHome") { v -> PuntoConfig().also { it.puntoHome = v[0] } }

    @JvmStatic
    val ignore: Parser<PuntoConfig> =
        methodMultiArg("ignore") { v -> PuntoConfig().also { it.ignore.addAll(v) } }

    @JvmStatic
    val git: Parser<Repository> =
        methodMappedArg("git") { name, repo -> repo.also { it.repo = name; it.mode = RepoType.git } }

    val mappedRepoParams: Parser<Repository> =
        longest(
            sequence(kv("into"), kv("branch")) { into, branch ->
                Repository().also { it.into = into; it.branch = branch }
            },
            kv("into") { into ->
                Repository().also { it.into = into }
            },
            kv("branch") { branch ->
                Repository().also { it.branch = branch }
            },
            sequence(kv("branch"), kv("into")) { branch, into ->
                Repository().also { it.into = into; it.branch = branch }
            }
        )

    private fun repo(into: String, branch: String) =
        Repository().also { it.into = into; it.branch = branch }

    @JvmStatic
    val puntoConfig: Parser<PuntoConfig> =
        sequence(or(userHome, puntoHome, ignore), or(NL, EOF)) { f: PuntoConfig, _ -> f }
            .many()
            .map {
                it.reduce { acc, puntoConfig ->
                    acc.userHome = puntoConfig.userHome ?: acc.userHome
                    acc.puntoHome = puntoConfig.puntoHome ?: acc.puntoHome
                    acc.ignore.addAll(puntoConfig.ignore)
                    acc.repositories.addAll(puntoConfig.repositories)
                    acc
                }
            }

    private fun extract(input: String) =
        input.drop(1).dropLast(1)

    private fun extract(input: List<String>) =
        input.map { extract(it) }

    private fun kv(key: String) =
        kv(key) { it }

    private fun <T> kv(key: String, f: (String) -> T) =
        sequence(isChar(',').pad(), string(key), isChar(':').pad(), STRING.pad()) { _, _, _, v ->
            f(extract(v))
        }

    private fun <T> methodMultiArg(methodName: String, f: (List<String>) -> T) =
        longest(
            sequence(string(methodName).pad(), STRINGS.pad().paren()) { _, value -> f(extract(value)) },
            sequence(string(methodName).pad(Left), WS, STRINGS) { _, _, value -> f(extract(value)) }
        )

    fun <T> methodMappedArg(methodName: String, f: (String, Repository) -> T) =
        or(
            sequence(string(methodName), STRING.pad(Left)) { _, n -> f(extract(n), Repository()) },
            sequence(string(methodName), STRING.pad(Left), mappedRepoParams) { _, n, r -> f(extract(n), r) }
        )
}
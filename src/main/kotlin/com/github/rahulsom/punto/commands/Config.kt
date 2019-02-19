package com.github.rahulsom.punto.commands

import com.github.rahulsom.punto.config.Repository
import org.slf4j.LoggerFactory
import picocli.CommandLine.Command
import picocli.CommandLine.Mixin

@Command(name = "config", description = ["Prints configuration"])
class Config : Runnable {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Mixin
    lateinit var configurable: Configurable

    override fun run() {
        logger.info("Running config")

        val config = configurable.getConfig() ?: return

        println("userHome '${config.userHome}'")
        println("puntoHome '${config.puntoHome}'")
        if (config.repositories.isNotEmpty()) {
            println(
                config.repositories
                    .joinToString("\n", "\n", transform = Companion::renderRepository)
            )
        }

        if (config.ignore.isNotEmpty()) {
            println()
            val ignores = config.ignore.joinToString(", ") { "'$it'" }
            println("ignore $ignores")
        }
    }

    companion object {
        fun renderRepository(repository: Repository): String {
            val sb = StringBuilder()
            sb.append(repository.mode)

            when {
                repository.include.isNotEmpty() -> sb.append("('${repository.repo}'")
                else -> sb.append(" '${repository.repo}'")
            }

            mapParam(sb, "branch", repository.branch)
            mapParam(sb, "into", repository.into)

            if (repository.include.isNotEmpty()) {
                val includes = repository.include.joinToString(", ") { "'$it'" }
                sb.append(listOf(") {", "    include $includes", "}").joinToString("\n"))
            }

            return sb.toString()
        }

        private fun mapParam(sb: StringBuilder, paramName: String, paramValue: String?) {
            if (paramValue != null) {
                sb.append(", $paramName: '$paramValue'")
            }
        }
    }
}
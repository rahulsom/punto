package com.github.rahulsom.punto.commands

import org.slf4j.LoggerFactory
import picocli.CommandLine.Command
import picocli.CommandLine.Mixin

@Command(
    name = "config",
    description = ["Prints configuration"]
)
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
            println()
        }
        config.repositories.map { repository ->
            val sb = StringBuilder()
            sb.append(repository.mode)

            when {
                repository.include.isNotEmpty() -> sb.append("('${repository.repo}'")
                else -> sb.append(" '${repository.repo}'")
            }

            mapParam(sb, "branch", repository.branch)
            mapParam(sb, "into", repository.into)

            when {
                repository.include.isNotEmpty() -> {
                    val includes = repository.include.joinToString(", ") { "'$it'" }
                    sb.append(") {")
                        .append("\n")
                        .append("    include $includes")
                        .append("\n")
                        .append("}")
                }
            }
            sb.toString()

        }.forEach { println(it) }
        if (config.ignore.isNotEmpty()) {
            println()
            println("ignore ${config.ignore.joinToString(", ") { "'$it'" }}")
        }
    }

    private fun mapParam(sb: StringBuilder, paramName: String, paramValue: String?) {
        if (paramValue != null) {
            sb.append(", $paramName: '$paramValue'")
        }
    }
}
package com.github.rahulsom.punto.commands

import com.github.rahulsom.punto.Ignores
import com.github.rahulsom.punto.config.PuntoConfig
import com.github.rahulsom.punto.utils.ExecUtil
import org.slf4j.LoggerFactory
import picocli.CommandLine.Command
import picocli.CommandLine.Mixin
import java.io.File

@Command(
    name = "diff",
    description = ["Computes diff between staging and current"]
)
class Diff : Runnable {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Mixin
    lateinit var configurable: Configurable

    override fun run() {
        val config = configurable.getConfig() ?: return

        Stage().also { it.configurable = configurable }.run()

        logger.info("Starting diff...")
        diff(config)
        logger.info("... diff complete")
    }

    fun diff(config: PuntoConfig) {
        val ignores = (config.ignore + Ignores.standardIgnores).toMutableList()
        val stagingDir = "${config.puntoHome}/staging"
        File(stagingDir).list()
            .filter { !ignores.contains(it) }
            .forEach { ignores.add("!$it") }
        logger.info("ignores: $ignores")

        val (copy, skip) = ignores.partition { it.startsWith("!") }

        logger.info("Copy: ${copy.map { it.substring(1) }}")
        logger.info("Skip: $skip")

        copy.forEach { rootLevelDirectory ->
            val name = rootLevelDirectory.substring(1)
            val file = File(config.userHome, name)
            if (file.exists()) {
                when {
                    file.isDirectory -> copyDirectory(stagingDir, name, config, skip)
                    else -> File("${config.userHome}/$name").copyTo(File("$stagingDir/$name"), true)
                }
            }
        }
    }

    private fun copyDirectory(stagingDir: String, name: String, config: PuntoConfig, skip: List<String>) {
        val excluded = skip
            .filter { excluded -> excluded.startsWith(name) }
            .map { "--exclude=${it.replace(Regex("^$name"), "")}" }
        val command = listOf("rsync", "-arv") +
                excluded +
                listOf("${config.userHome}/$name/", "$stagingDir/$name/")
        ExecUtil.exec(File(config.userHome), *command.toTypedArray())
    }

}
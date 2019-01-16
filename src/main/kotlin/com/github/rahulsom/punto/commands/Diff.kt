package com.github.rahulsom.punto.commands

import com.github.rahulsom.punto.Ignores
import com.github.rahulsom.punto.config.PuntoConfig
import com.github.rahulsom.punto.utils.FileUtil.copyDirectory
import com.github.rahulsom.punto.utils.FileUtil.copyFile
import org.slf4j.LoggerFactory
import picocli.CommandLine.Command
import picocli.CommandLine.Mixin
import java.io.File

@Command(name = "diff", description = ["Computes diff between staging and current"])
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
                    file.isDirectory -> copyDirectory(config.userHome, stagingDir, name, skip)
                    else -> copyFile(config.userHome, stagingDir, name)
                }
            }
        }
    }

}
package com.github.rahulsom.punto.commands

import com.github.rahulsom.punto.Ignores
import com.github.rahulsom.punto.config.PuntoConfig
import com.github.rahulsom.punto.utils.FileUtil
import org.slf4j.LoggerFactory
import picocli.CommandLine.Command
import picocli.CommandLine.Mixin
import java.io.File

@Command(name = "update", description = ["Updates user home with latest staging contents."])
class Update : Runnable {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Mixin
    lateinit var configurable: Configurable

    override fun run() {
        val config = configurable.getConfig()?.withDefaults() ?: return

        Stage().also { it.configurable = configurable }.run()

        logger.info("Starting update...")
        update(config)
        logger.info("... update complete")
    }

    fun update(config: PuntoConfig) {
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
                    file.isDirectory -> FileUtil.copyDirectory(stagingDir, config.userHome!!, name, skip)
                    else -> FileUtil.copyFile(stagingDir, config.userHome!!, name)
                }
            }
        }
        println("... Dotfiles Updated in ${config.userHome}")
    }
}
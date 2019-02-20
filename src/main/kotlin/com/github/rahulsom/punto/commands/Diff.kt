package com.github.rahulsom.punto.commands

import com.github.rahulsom.punto.Ignores
import com.github.rahulsom.punto.config.PuntoConfig
import com.github.rahulsom.punto.utils.ExecUtil
import com.github.rahulsom.punto.utils.FileUtil.copyDirectory
import com.github.rahulsom.punto.utils.FileUtil.copyFile
import com.github.rahulsom.punto.utils.StatusLine
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
        val config = configurable.getConfig()?.withDefaults() ?: return

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
                    file.isDirectory -> copyDirectory(config.userHome!!, stagingDir, name, skip)
                    else -> copyFile(config.userHome!!, stagingDir, name)
                }
            }
        }

        if (config.repositories.isNotEmpty()) {
            val personalRepo = config.repositories.last().getDestination()

            val status = ExecUtil.exec(File(stagingDir), "git", "status", "--porcelain=1")

            val changes = status.err
                .split("\n")
                .map(StatusLine.Companion::parse)
                .mapNotNull {
                    when (it) {
                        is StatusLine.Modified -> File(config.userHome, it.file)
                        is StatusLine.Unstaged -> File(config.userHome, it.file)
                        StatusLine.Error -> null
                    }
                }

            changes
                .forEach { file ->
                    when {
                        file.isDirectory -> copyDirectory(config.userHome!!, stagingDir, file.name, skip)
                        else -> copyFile(
                            config.userHome!!, "${config.puntoHome}/repositories/$personalRepo",
                            file.toRelativeString(File(config.userHome))
                        )
                    }
                }

            if (changes.isNotEmpty()) {
                println("... Diff updated in $stagingDir and ${config.puntoHome}/repositories/$personalRepo")
            }
        }
    }
}
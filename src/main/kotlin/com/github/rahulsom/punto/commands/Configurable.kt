package com.github.rahulsom.punto.commands

import com.github.rahulsom.punto.config.PuntoConfig
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import picocli.CommandLine.*
import picocli.CommandLine.Help.Visibility.ALWAYS
import java.io.File
import java.io.FileInputStream

class Configurable {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Option(
        names = ["-h", "--help"],
        usageHelp = true,
        description = ["Show this help message and exit."]
    )
    var helpRequested: Boolean = false

    @Option(
        names = ["-c", "--configFile"],
        description = ["Punto config file"],
        showDefaultValue = ALWAYS
    )
    var configFile: File = File("${System.getProperty("user.home")}/punto.yaml")

    fun getConfig() =
        when {
            configFile.exists() -> {
                val runtimeProperty = "java.runtime.name"
                val runtime = System.getProperty(runtimeProperty)
                System.setProperty(runtimeProperty, runtime ?: "GraalVM")

                val fileInputStream = FileInputStream(configFile)
                val yaml = Yaml(Constructor(PuntoConfig::class.java))
                yaml.loadAs(fileInputStream, PuntoConfig::class.java) ?: PuntoConfig()
            }
            else -> {
                logger.error("Could not find file $configFile")
                null
            }
        }
}
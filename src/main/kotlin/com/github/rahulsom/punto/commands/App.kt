package com.github.rahulsom.punto.commands

import com.github.rahulsom.punto.VersionProvider
import org.slf4j.LoggerFactory
import picocli.CommandLine
import picocli.CommandLine.*

@Command(
    name = "punto",
    description = ["Manages dotfiles."],
    versionProvider = VersionProvider::class,
    subcommands = [
        Config::class
    ],
    showDefaultValues = true
)
class App : Runnable {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Option(
        names = ["-h", "--help"],
        usageHelp = true,
        description = ["Show this help message and exit."]
    )
    var helpRequested: Boolean = false

    @Option(
        names = ["-V", "--version"],
        versionHelp = true,
        description = ["Print version information and exit."]
    )
    var versionRequested: Boolean = false

    override fun run() {
        logger.info("Running")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = CommandLine.run(App(), *args)
    }
}


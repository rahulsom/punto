package com.github.rahulsom.punto

import org.slf4j.LoggerFactory
import picocli.CommandLine

@CommandLine.Command(
    name = "punto",
    description = ["Prints the checksum (MD5 by default) of a file to STDOUT."],
    versionProvider = VersionProvider::class
)
class App : Runnable {
    val logger = LoggerFactory.getLogger(App::class.java)

    @CommandLine.Option(
        names = ["-h", "--help"],
        usageHelp = true,
        description = ["Show this help message and exit."]
    )
    var helpRequested: Boolean = false

    @CommandLine.Option(
        names = ["-V", "--version"],
        versionHelp = true,
        description = ["Print version information and exit."]
    )
    var versionRequested: Boolean = false

    override fun run() {
        logger.info("Running")
    }

    val greeting: String
        get() {
            return "Hello world."
        }
}

fun main(args: Array<String>) {
    CommandLine.run(App(), *args)
}

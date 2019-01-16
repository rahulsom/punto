package com.github.rahulsom.punto.commands

import com.github.rahulsom.punto.VersionProvider
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(
    name = "punto",
    description = ["Manages dotfiles."],
    versionProvider = VersionProvider::class,
    subcommands = [
        Config::class, Stage::class, Diff::class, Update::class
    ],
    showDefaultValues = true
)
class App : Runnable {

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

    override fun run() = CommandLine.run(App(), "-h")

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = CommandLine.run(App(), *args)
    }
}
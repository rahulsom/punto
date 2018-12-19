package com.github.rahulsom.punto.utils

import java.io.File
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

object ExecUtil {

    data class ProcessReturn(val out: String, val err: String)

    private val logger = Logger.getLogger(javaClass.canonicalName)

    fun exec(workingDir: File, vararg command: String): ProcessReturn {
        val out = File.createTempFile("punto", "out.txt")
        val err = File.createTempFile("punto", "err.txt")

        logger.fine("exec(workingDir: $workingDir, command: ${command.toList()})")

        val process = ProcessBuilder(*command)
            .directory(workingDir)
            .redirectError(ProcessBuilder.Redirect.to(out))
            .redirectOutput(ProcessBuilder.Redirect.to(err))
            .start()

        process.waitFor(60, TimeUnit.MINUTES)

        return ProcessReturn(out.readText(), err.readText())
    }
}
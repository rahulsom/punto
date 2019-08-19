package com.github.rahulsom.punto.utils

import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit.SECONDS

object ExecUtil {

    data class ProcessReturn(val out: String, val err: String, val workingDir: File, val command: List<String>)

    private val logger = LoggerFactory.getLogger(javaClass.canonicalName)

    @JvmStatic
    fun exec(workingDir: File, vararg command: String): ProcessReturn {
        val tmpDir = File(System.getProperty("java.io.tmpdir", "/tmp"))
        val out = File.createTempFile("punto", "out.txt", tmpDir)
        val err = File.createTempFile("punto", "err.txt", tmpDir)

        logger.debug("exec(workingDir: $workingDir, command: ${command.toList()})")

        val process = ProcessBuilder(*command)
            .directory(workingDir)
            .redirectError(ProcessBuilder.Redirect.to(out))
            .redirectOutput(ProcessBuilder.Redirect.to(err))
            .start()

        process.waitFor(60, SECONDS)

        return ProcessReturn(out.readText(), err.readText(), workingDir, command.toList())
    }
}
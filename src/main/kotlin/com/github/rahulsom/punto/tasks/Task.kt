package com.github.rahulsom.punto.tasks

import org.slf4j.LoggerFactory

class Task(val identifier: String, val code: () -> Unit) {
    val dependencies = mutableSetOf<Task>()
    fun dependsOn(task: Task) = dependencies.add(task)
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun equals(other: Any?) =
        when {
            this === other -> true
            other !is Task -> false
            identifier != other.identifier -> false
            else -> true
        }

    override fun hashCode() =
        identifier.hashCode()

    override fun toString() =
        "$identifier\n${dependencies.joinToString("\n") { "  - ${it.identifier}" }}"

    @Volatile
    private var hasRun: Boolean = false
    @Volatile
    var isRunning: Boolean = false

    @Synchronized
    fun run() {
        if (isRunnable()) {
            isRunning = true
            logger.info("Starting $identifier")
            code()
            hasRun = true
            isRunning = false
            logger.info("Finished $identifier")
        }
    }

    private fun isRunnable() = !hasRun && !isRunning && dependencies.all { it.hasRun }

    fun runTree() {
        dependencies.forEach { it.runTree() }
        run()
    }
}
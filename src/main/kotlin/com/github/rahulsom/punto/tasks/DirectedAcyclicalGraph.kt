package com.github.rahulsom.punto.tasks

import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter

class Task(val identifier: String, val code: () -> Unit) {
    val dependencies = mutableSetOf<Task>()
    fun dependsOn(task: Task) = dependencies.add(task)
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun equals(other: Any?) = when {
        this === other -> true
        javaClass != other?.javaClass -> false
        identifier != (other as Task).identifier -> false
        else -> true
    }

    override fun hashCode() = identifier.hashCode()

    override fun toString(): String {
        return "$identifier\n${dependencies.joinToString("\n") { "  - " + it.identifier }}"
    }

    @Volatile
    private var hasRun: Boolean = false
    @Volatile
    var isRunning: Boolean = false

    @Synchronized
    fun run() {
        if (hasRun) {
            // Has already run
        } else if (isRunning) {
            // Is already running
        } else if (!dependencies.all { it.hasRun }) {
            // Unsatisfied dependencies
        } else {
            isRunning = true
            logger.info("Starting $identifier")
            code()
            hasRun = true
            isRunning = false
            logger.info("Finished $identifier")
        }
    }

    fun runTree() {
        dependencies.forEach { it.runTree() }
        run()
    }

}

class DirectedAcyclicalGraph {
    val allTasks = mutableListOf<Task>()
    fun createTask(identifier: String, code: () -> Unit) =
        allTasks.find { it.identifier == identifier }
            ?: Task(identifier, code).also { allTasks.add(it) }

    fun getTask(identifier: String) = allTasks.find { it.identifier == identifier }

    fun toGraphviz(): String {
        val sb = StringWriter()
        val pw = PrintWriter(sb)
        pw.println("digraph G {")
        pw.println("  ranksep=.75; rankdir=RL;")

        allTasks.forEachIndexed { index, task ->
            val label = task.identifier
                .split(" ")
                .joinToString("\\n") {
                    it.replace(Regex(".git\$"), "")
                        .takeLast(15)
                }
            val color = when (label.split("\\n")[0]) {
                "clone" -> "#FFDDDD"
                "checkout" -> "#DDFFDD"
                "copy" -> "#DDDDFF"
                else -> "#DDDDDD"
            }
            pw.println("  a$index [label=\"$label\" color=\"$color\" style=\"filled\"];")
        }

        allTasks.forEach { task ->
            val fi = allTasks.indexOf(task)
            task.dependencies.forEach { dep ->
                val ti = allTasks.indexOf(dep)
                pw.println("  a$fi -> a$ti;")
            }
        }
        pw.println("}")
        pw.flush()
        return sb.toString()
    }

}
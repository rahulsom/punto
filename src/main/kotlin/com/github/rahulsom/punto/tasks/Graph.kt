package com.github.rahulsom.punto.tasks

import java.io.PrintWriter
import java.io.StringWriter

class Graph {
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
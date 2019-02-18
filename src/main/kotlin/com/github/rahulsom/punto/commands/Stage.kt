package com.github.rahulsom.punto.commands

import com.github.rahulsom.punto.config.PuntoConfig
import com.github.rahulsom.punto.config.Repository
import com.github.rahulsom.punto.tasks.Graph
import com.github.rahulsom.punto.tasks.Task
import com.github.rahulsom.punto.utils.ExecUtil
import com.github.rahulsom.punto.utils.ExecUtil.exec
import com.github.rahulsom.punto.utils.FileUtil
import org.slf4j.LoggerFactory
import picocli.CommandLine.Command
import picocli.CommandLine.Mixin
import java.io.File

@Command(name = "stage", description = ["Sets up the staging directory"])
class Stage : Runnable {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Mixin
    lateinit var configurable: Configurable

    override fun run() {
        val config = configurable.getConfig() ?: return

        logger.info("Running stage")
        val graph = Graph()

        val stagingDir = "${config.puntoHome}/staging"
        val stageTask = graph.createTask("Stage") {
            println("... Dotfiles Staged in $stagingDir")
        }

        val setupStaging = graph.createTask("Setup Staging") {
            File(stagingDir).deleteRecursively()
            File(stagingDir).mkdirs()
            ExecUtil.exec(File(stagingDir), "git", "init")
        }

        var lastCopyTask: Task? = null

        config.repositories.forEach {
            logger.info("${it.identifier} is ${Config.renderRepository(it)}")
        }

        config.repositories.forEach { repository ->
            val copyTask = createTasks(config, repository, graph, stagingDir, setupStaging)
            lastCopyTask?.let { copyTask.dependsOn(it) }
            lastCopyTask = copyTask
            stageTask.dependsOn(copyTask)
        }

        config.repositories
            .groupBy { it.getUrl() }
            .filterValues { it.size > 1 }
            .values
            .forEach { setupDependencies(it, graph) }

        File("/tmp/graph.dot").writeText(graph.toGraphviz())
        stageTask.runTree()
    }

    private fun setupDependencies(repos: List<Repository>, graph: Graph) {
        var lastCopyTask: Task? = null
        repos.forEach { repository ->
            val checkoutTask = graph.getTask("checkout ${repository.identifier}")
            val copyTask = graph.getTask("copy from ${repository.identifier}")
            lastCopyTask?.let { l -> checkoutTask?.dependsOn(l) }
            lastCopyTask = copyTask
        }
    }

    private fun createTasks(
        config: PuntoConfig,
        repository: Repository,
        graph: Graph,
        stagingDir: String,
        setupStaging: Task
    ): Task {
        val localRepo = "${config.puntoHome}/repositories/${repository.getDestination()}"
        val cloneTask = graph.createTask("clone ${repository.getUrl()}") {
            cloneRepository(repository.getUrl(), localRepo)
        }
        val checkoutTask = graph.createTask("checkout ${repository.identifier}") {
            exec(File(localRepo), "git", "checkout", repository.branch ?: "master")
        }
        checkoutTask.dependsOn(cloneTask)
        val copyTask = graph.createTask("copy from ${repository.identifier}") {
            val destination = "$stagingDir/${repository.into ?: ""}"
            FileUtil.copy(localRepo, destination, repository.include, config.puntoHome, config.userHome)
            commitFiles(config.puntoHome, repository, stagingDir)
        }
        copyTask.dependsOn(setupStaging)
        copyTask.dependsOn(checkoutTask)
        return copyTask
    }

    private fun cloneRepository(url: String, checkoutDir: String): ExecUtil.ProcessReturn {
        logger.info("cloneRepo $url")
        val repoDir = File(checkoutDir)

        if (!repoDir.exists()) {
            repoDir.parentFile.mkdirs()
            exec(repoDir.parentFile, "git", "clone", url, repoDir.absolutePath)
        } else {
            exec(repoDir, "git", "clean", "-fdqx")
        }

        return exec(repoDir, "git", "fetch", "--all")
    }

    companion object {
        fun commitFiles(puntoHome: String, repository: Repository, stagingDir: String) {
            ExecUtil.exec(File(stagingDir), "git", "add", ".")

            val repositoryDir =
                File("$puntoHome/repositories/${repository.getDestination()}")
            val result = ExecUtil.exec(repositoryDir, "git", "rev-parse", "HEAD")

            val commitId = result.err.substring(0, 8)
            val commitDescription = "Commit Id is: $commitId"
            val title = "Add ${repository.mode} repo '${repository.repo}' commit $commitId"
            val repoDsl = "```\n${Config.renderRepository(repository)}\n```"
            val message = listOf(title, repoDsl, commitDescription).joinToString("\n\n")

            ExecUtil.exec(File(stagingDir), "git", "commit", "--allow-empty", "-m", message)
        }
    }
}
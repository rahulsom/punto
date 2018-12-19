package com.github.rahulsom.punto.commands

import com.github.rahulsom.punto.config.Repository
import com.github.rahulsom.punto.tasks.DirectedAcyclicalGraph
import com.github.rahulsom.punto.tasks.Task
import com.github.rahulsom.punto.utils.ExecUtil
import com.github.rahulsom.punto.utils.ExecUtil.exec
import com.github.rahulsom.punto.utils.FileUtil
import org.slf4j.LoggerFactory
import picocli.CommandLine.Command
import picocli.CommandLine.Mixin
import java.io.File

@Command(
    name = "stage",
    description = ["Sets up the staging directory"]
)
class Stage : Runnable {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Mixin
    lateinit var configurable: Configurable

    override fun run() {
        logger.info("Running stage")
        val config = configurable.getConfig() ?: return

        val graph = DirectedAcyclicalGraph()

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
            lastCopyTask?.let { copyTask.dependsOn(it) }
            lastCopyTask = copyTask
            stageTask.dependsOn(copyTask)
        }

        config.repositories
            .groupBy { it.getUrl() }
            .filterValues { it.size > 1 }
            .values
            .forEach { repos ->
                lastCopyTask = null
                repos.forEach { repository ->
                    val checkoutTask = graph.getTask("checkout ${repository.identifier}")
                    val copyTask = graph.getTask("copy from ${repository.identifier}")
                    if (lastCopyTask != null) {
                        checkoutTask!!.dependsOn(lastCopyTask!!)
                    }
                    lastCopyTask = copyTask
                }
            }

        File("/tmp/graph.dot").writeText(graph.toGraphviz())
        stageTask.runTree()
    }

    val cloneRepository = { url: String, checkoutDir: String ->
        logger.info("cloneRepo $url")
        val repoDir = File(checkoutDir)

        if (!repoDir.exists()) {
            repoDir.parentFile.mkdirs()
            exec(repoDir.parentFile, "git", "clone", url, repoDir.absolutePath)
        }

        exec(repoDir, "git", "fetch", "--all")
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
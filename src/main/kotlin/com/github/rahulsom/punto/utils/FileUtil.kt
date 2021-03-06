package com.github.rahulsom.punto.utils

import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitResult.CONTINUE
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.attribute.BasicFileAttributes

object FileUtil {

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun copyFiles(includes: Set<Path>, find: String, replace: String, puntoHome: String, userHome: String) =
        includes
            .forEach { path ->
                val dest = path.toString()
                    .replace(find, replace)
                    .replace("//", "/")
                    .replace(Regex("/$"), "")
                val relativePath = path.toString().replace("$find/", "")

                val sourceHome = find.replace(puntoHome, "\$PUNTOHOME")
                val destHome = replace.replace(userHome, "\$HOME").replace(Regex("/$"), "")
                logger.debug("    -> cp {$sourceHome,$destHome}/$relativePath")
                File(dest).parentFile.mkdirs()
                Files.copy(path, Paths.get(dest), REPLACE_EXISTING)
            }

    private fun applyExcludes(path: Path?, files: MutableSet<Path>, initialExclude: List<String>) =
        (initialExclude + listOf("**/.git/**"))
            .forEach {
                val pathMatcher = FileSystems.getDefault().getPathMatcher("glob:$it")
                Files.walkFileTree(path, CopyVisitor(pathMatcher, files))
            }

    private fun applyIncludes(path: Path?, files: MutableSet<Path>, initialInclude: List<String>) =
        initialInclude
            .ifEmpty { listOf("**") }
            .forEach {
                val pathMatcher = FileSystems.getDefault().getPathMatcher("glob:$it")
                Files.walkFileTree(path, CopyVisitor(pathMatcher, files))
            }

    @JvmStatic
    fun copy(source: String, destination: String, includes: List<String>, puntoHome: String, userHome: String) {
        logger.info("copy from $source to $destination. includes: $includes")
        val files = mutableSetOf<Path>()
        val excluded = mutableSetOf<Path>()
        val repoHome = Paths.get(source)

        val (excludeOnlyPatterns, includeOnlyPatterns) =
            includes.partition { it.startsWith("!") }

        applyIncludes(repoHome, files, includeOnlyPatterns)
        applyExcludes(repoHome, excluded, excludeOnlyPatterns.map { it.substring(1) })

        val effectiveIncludes = files - excluded
        copyFiles(effectiveIncludes, source, destination, puntoHome, userHome)
    }

    class CopyVisitor(private val pathMatcher: PathMatcher, private val paths: MutableSet<Path>) :
        SimpleFileVisitor<Path>() {

        override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
            if (pathMatcher.matches(path)) {
                paths.add(path)
            }
            return CONTINUE
        }

        override fun visitFileFailed(file: Path, exc: IOException) = CONTINUE
    }

    @JvmStatic
    fun copyDirectory(from: String, to: String, name: String, skip: List<String>) {
        val excluded = skip
            .filter { excluded -> excluded.startsWith(name) }
            .map { "--exclude=${it.replace(Regex("^$name"), "")}" }
        val command = listOf("rsync", "-arv") + excluded + listOf("$from/$name/", "$to/$name/")
        ExecUtil.exec(File(from), *command.toTypedArray())
    }

    @JvmStatic
    fun copyFile(from: String, to: String, name: String) {
        File("$from/$name").copyTo(File("$to/$name"), true)
    }
}
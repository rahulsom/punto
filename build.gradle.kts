import org.ajoberstar.grgit.Grgit
import org.apache.commons.codec.digest.DigestUtils
import org.apache.tools.ant.taskdefs.condition.Os
import org.asciidoctor.gradle.AsciidoctorTask
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
    dependencies {
        classpath("commons-codec:commons-codec:1.12")
    }
}

plugins {
    id("com.gradle.build-scan") version "2.1"
    id("me.champeau.buildscan-recipes") version "0.2.3"
    application
    id("groovy")
    id("nebula.release") version "9.1.0"
    id("org.jetbrains.kotlin.jvm") version "1.3.11"
    id("com.github.johnrengelman.shadow") version "4.0.3"
    id("com.jfrog.bintray") version "1.8.5"
    id("org.asciidoctor.convert") version "1.5.9.2"
    id("org.ajoberstar.git-publish") version "2.0.0"
    id("org.jlleitschuh.gradle.ktlint") version "7.1.0"
}

repositories {
    jcenter()
}

val picocli: Configuration by configurations.creating
val distro: Configuration by configurations.creating

sourceSets {
    main {
        withConvention(KotlinSourceSet::class) {
            kotlin.srcDir("build/generatedSrc/kotlin")
        }
    }
}

dependencyLocking {
    lockAllConfigurations()
}

val vmName: String = System.getProperty("java.vm.name", "GraalVM CE 19.1.1")
println("VM Name: $vmName")
val graalVersion =
    when {
        vmName.contains("GraalVM") -> vmName.split(" ").last()
        else -> "+"
    }

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("info.picocli:picocli:3.8.2")
    implementation("org.yaml:snakeyaml:1.23")
    implementation("org.slf4j:slf4j-api:1.7.25")

    runtime("org.graalvm.sdk:graal-sdk:$graalVersion")
    runtime("com.oracle.substratevm:svm:$graalVersion")
    runtime("ch.qos.logback:logback-classic:1.2.+")
    runtime("org.fusesource.jansi:jansi:1.9")

    picocli("info.picocli:picocli-codegen:3.8.2")

    testImplementation("org.codehaus.groovy:groovy-all:2.5.+")
    testImplementation("org.spockframework:spock-core:1.2-groovy-2.5")
    testImplementation("org.springframework.boot:spring-boot:1.2.1.RELEASE")
    testImplementation("org.apache.commons:commons-text:1.6")
}

application {
    mainClassName = "com.github.rahulsom.punto.commands.App"
}

val createPicocliJson by tasks.creating(JavaExec::class.java) {
    dependsOn("compileKotlin")
    classpath(picocli, configurations.getByName("runtimeClasspath"), tasks.getByPath("compileKotlin").outputs)
    main = "picocli.codegen.aot.graalvm.ReflectionConfigGenerator"
    args("-o", "build/graal-picocli.json", "com.github.rahulsom.punto.commands.App")
}

var commandParts: List<String>? = null
val createScripts by tasks.creating {
    val shadowJarTask = tasks.getByName("shadowJar")
    dependsOn(shadowJarTask)

    doLast {
        val graalFiles = listOf(
            "src/graal/graal-logback.json",
            "src/graal/graal-punto.json",
            "build/graal-picocli.json"
        ).joinToString(",")

        commandParts = listOf(
            "native-image",
            "-H:+ReportUnsupportedElementsAtRuntime",
            "-H:IncludeResources='logback.xml|ignores.txt'",
            "-H:ReflectionConfigurationFiles=$graalFiles",
            "-H:+JNI",
            "--no-server",
            "--allow-incomplete-classpath",
            "-jar",
            shadowJarTask.outputs.files.singleFile.absolutePath.replace(projectDir.absolutePath + "/", "")
        )

        val command = commandParts!!.map { "\"$it\"" }.joinToString(" \\\n    ")
        File(buildDir, "build.sh")
            .writeText(
                """
                #!/bin/bash

                VERSION=${'$'}(ls -1 ~/.sdkman/candidates/java | grep -v current)
                export JAVA_HOME=~/.sdkman/candidates/java/${'$'}VERSION
                export PATH=${'$'}PATH:${'$'}JAVA_HOME/bin

                mkdir -p build/native/linux

                """.trimIndent()
            )

        val moveCommand = "mv '${project.name}-$version' build/native/linux/${project.name}"
        File(buildDir, "build.sh").appendText(command + "\n\n")
        File(buildDir, "build.sh").appendText(moveCommand + "\n\n")

    }
}

val nativeImage: Task by tasks.creating {
    val suffix = when {
        Os.isFamily(Os.FAMILY_MAC) -> "macos"
        Os.isFamily(Os.FAMILY_UNIX) -> "unix"
        else -> "unknown"
    }

    dependsOn(createScripts)
    val shadowJarTask = tasks.getByName("shadowJar")
    inputs.files(shadowJarTask.outputs.files)
    outputs.file(file("$buildDir/native/$suffix/${project.name}"))

    doLast {
        exec {
            commandLine(commandParts)
        }
        file("$buildDir/native").mkdirs()
        file("${project.name}-$version").renameTo(file("$buildDir/native/$suffix/${project.name}"))
    }
}

val runNative: Task  by tasks.creating {
    dependsOn(nativeImage)
    doLast {
        exec {
            commandLine(nativeImage.outputs.files.singleFile.absolutePath)
        }
    }
}

tasks.getByName("shadowJar").dependsOn("createPicocliJson")

val storeVersion: Task by tasks.creating {
    inputs.property("version", project.version.toString())
    outputs.file("build/generatedSrc/kotlin/com/github/rahulsom/punto/VersionProvider.kt")

    doLast {
        println("Storing version $version")
        project
            .file("build/generatedSrc/kotlin/com/github/rahulsom/punto")
            .mkdirs()
        project
            .file("build/generatedSrc/kotlin/com/github/rahulsom/punto/VersionProvider.kt")
            .writeText(
                """
                package com.github.rahulsom.punto

                import picocli.CommandLine

                class VersionProvider : CommandLine.IVersionProvider {
                    override fun getVersion() = arrayOf("${project.version}")
                }""".trimIndent()
            )

        val standardIgnores = project.file("src/main/resources/ignores.txt")
            .reader()
            .readLines()
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        project
            .file("build/generatedSrc/kotlin/com/github/rahulsom/punto/Ignores.kt")
            .writeText(
                """
                package com.github.rahulsom.punto

                object Ignores {
                    val standardIgnores by lazy {
                        listOf(
                            ${standardIgnores.joinToString(",\n".padEnd(30)) { "\"$it\"" }}
                        )
                    }
                }""".trimIndent()
            )
    }
}
tasks.getByName("compileKotlin").dependsOn(storeVersion)

val resolveAndLockAll: Task  by tasks.creating {
    doFirst {
        require(gradle.startParameter.isWriteDependencyLocks)
    }
    doLast {
        configurations
            .filter { it.isCanBeResolved }
            .forEach { it.resolve() }
    }
}

val buildImage by tasks.creating(Exec::class) {
    commandLine("docker", "build", "-t", "rahulsom/linuxbuild", ".docker")
}

val buildLinuxVersion by tasks.creating(Exec::class) {
    val shadowJarTask = tasks.getByName("shadowJar")
    inputs.files(shadowJarTask.outputs.files)
    outputs.file(file("$buildDir/native/linux/${project.name}"))

    dependsOn(buildImage, tasks.getByName("shadowJar"), createScripts)
    commandLine(
        "docker", "run",
        "-v", "$projectDir:/home/builds/src",
        "-v", "${System.getProperty("user.home")}/.gradle:/home/builds/.gradle",
        "--rm", "rahulsom/linuxbuild"
    )
}

val zip by tasks.creating(Zip::class) {
    dependsOn(nativeImage, buildLinuxVersion)

    archiveFileName.set("punto-${project.version}.zip")
    destinationDirectory.set(file("$buildDir/dist"))

    from("$buildDir/native")
}

val createBrewFormula: Task by tasks.creating {
    dependsOn(zip)

    doLast {
        val sha = DigestUtils("SHA-256").digestAsHex(zip.outputs.files.singleFile)
        project.file("$buildDir/punto.rb").writeText(
            """
            |class Punto < Formula
            |  desc "Composable Dotfile Manager"
            |  homepage "https://rahulsom.github.io/punto/"
            |  url "https://bintray.com/api/ui/download/rahulsom/punto/punto/${project.version}/punto-${project.version}.zip"
            |  sha256 "$sha"
            |
            |  def install
            |    bin.install "macos/punto"
            |  end
            |
            |  test do
            |    system "#{bin}/punto", "--version"
            |    system "curl", "-s", "https://raw.githubusercontent.com/rahulsom/punto/master/src/test/resources/sample.punto.yaml", "-o", "/tmp/punto.yml"
            |    system "#{bin}/punto", "config", "-c", "/tmp/punto.yml"
            |    system "rm", "/tmp/punto.yml"
            |  end
            |end
            """.trimMargin()
        )
    }
}

tasks.getByName("build").dependsOn(zip, createBrewFormula)

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    publishAlways()
}

buildScanRecipes {
    recipes("git-commit", "git-status", "teamcity", "gc-stats")
}

val bintrayUser = System.getenv("BINTRAY_USER")
val bintrayKey = System.getenv("BINTRAY_KEY")

bintray {
    user = bintrayUser
    key = bintrayKey

    publish = true

    pkg = PackageConfig().apply {
        userOrg = bintrayUser
        repo = "punto"
        name = "punto"
        publicDownloadNumbers = true

        setLicenses("GPL-3.0")
        vcsUrl = "https://github.com/rahulsom/punto.git"
        websiteUrl = "https://github.com/rahulsom/punto"
        issueTrackerUrl = "https://github.com/rahulsom/punto/issues"
        githubRepo = "rahulsom/punto"
        githubReleaseNotesFile = "CHANGELOG.md"
    }

    setConfigurations("distro")
}


artifacts {
    add("distro", zip)
}

tasks.getByName("final")
    .dependsOn("bintrayUpload", "gitPublishPush")
tasks.getByName("candidate")
    .dependsOn("bintrayUpload")

tasks.getByName("final")
    .doLast {
        if (file("build/homebrew").exists()) {
            file("build/homebrew").deleteRecursively()
        }
        val tap =
            Grgit.clone(mapOf("dir" to "build/homebrew", "uri" to "git@github.com:rahulsom/homebrew-rahulsom.git"))
        file("build/homebrew/Formula/punto.rb").writeText(file("build/punto.rb").readText())
        tap.add(mapOf("patterns" to listOf("punto.rb")))
        tap.commit(mapOf("message" to "Update punto to ${version}"))
        tap.push()
        tap.close()
    }

tasks.getByName("testClasses")
    .doLast { project.file("build/output").mkdirs() }

tasks.getByName("test").outputs.dirs("build/output", "build/resources/test")

tasks.withType<AsciidoctorTask> {
    attributes(
        mapOf(
            "toc" to "left",
            "icons" to "font",
            "docinfo" to "shared",
            "nofooter" to ""
        )
    )
    inputs.dir("build/output")
    dependsOn("test")
}

gitPublish {
    repoUri.set("git@github.com:rahulsom/punto.git")
    branch.set("gh-pages")

    contents {
        from("build/asciidoc/html5")
    }
}

tasks.getByName("gitPublishCopy")
    .dependsOn("asciidoctor")

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.apache.tools.ant.taskdefs.condition.Os

buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
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
}

repositories {
    jcenter()
}

val picocli: Configuration by configurations.creating

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

val vmName: String = System.getProperty("java.vm.name", "GraalVM 1.0.0-rc11")
val graalVersion =
    when {
        vmName.contains("GraalVM") -> vmName.split(" ")[1]
        else -> "+"
    }

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("info.picocli:picocli:3.8.2")
    implementation("org.yaml:snakeyaml:1.23")
    implementation("org.slf4j:slf4j-api:1.7.25")

    runtime("org.graalvm.sdk:graal-sdk:$graalVersion")
    runtime("com.oracle.substratevm:svm:$graalVersion") {
        exclude("com.oracle.substratevm", "svm-hosted-native-windows-amd64")
    }
    runtime("ch.qos.logback:logback-classic:1.2.+")
    runtime("org.fusesource.jansi:jansi:1.9")

    picocli("info.picocli:picocli-codegen:3.8.2")

    testImplementation("org.codehaus.groovy:groovy-all:2.5.+")
    testImplementation("org.spockframework:spock-core:1.2-groovy-2.5")
    testImplementation("org.springframework.boot:spring-boot:1.2.1.RELEASE")
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
            "-H:IncludeResources='/logback.xml,/ignores.txt'",
            "-H:ReflectionConfigurationFiles=$graalFiles",
            "-H:+JNI",
            "--no-server",
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

tasks.getByName("shadowJar")
    .dependsOn("createPicocliJson")

tasks.getByName("compileKotlin")
    .doFirst {
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

    archiveName = "punto.zip"
    destinationDir = file("$buildDir/dist")

    from("$buildDir/native")
}

tasks.getByName("build").dependsOn(zip)

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    publishAlways()
}

buildScanRecipes {
    recipes("git-commit", "git-status", "teamcity", "gc-stats")
}
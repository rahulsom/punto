import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

plugins {
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

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("info.picocli:picocli:3.8.2")
    implementation("org.yaml:snakeyaml:1.23")
    implementation("org.slf4j:slf4j-api:1.7.25")

    runtime("org.graalvm.sdk:graal-sdk:+")
    runtime("com.oracle.substratevm:svm:+")
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

val nativeImage: Task by tasks.creating {
    val shadowJarTask = tasks.getByName("shadowJar")
    dependsOn(shadowJarTask)
    inputs.files(shadowJarTask.outputs.files)
    outputs.file(file("$buildDir/native/${project.name}"))

    val graalFiles = listOf(
        "src/graal/graal-logback.json",
        "src/graal/graal-punto.json",
        "build/graal-picocli.json"
    ).joinToString(",")
    doLast {
        exec {
            commandLine(
                "native-image",
                "-H:+ReportUnsupportedElementsAtRuntime",
                "-H:IncludeResources='/logback.xml,/ignores.txt'",
                "-H:ReflectionConfigurationFiles=$graalFiles",
                "-H:+JNI",
                "--no-server",
                "-jar",
                shadowJarTask.outputs.files.singleFile
            )
        }
        file("$buildDir/native").mkdirs()
        file("${project.name}-$version-all").renameTo(file("$buildDir/native/${project.name}"))
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

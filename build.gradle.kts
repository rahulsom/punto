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

dependencies {
    implementation("org.graalvm.sdk:graal-sdk:+")
    implementation("com.oracle.substratevm:svm:+")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("ch.qos.logback:logback-classic:1.2.+")

    testImplementation("org.codehaus.groovy:groovy-all:2.5.+")
    testImplementation("org.spockframework:spock-core:1.2-groovy-2.5")
}

application {
    mainClassName = "com.github.rahulsom.punto.AppKt"
}

val nativeImage = task("nativeImage") {
    val shadowJarTask = tasks.getByName("shadowJar")
    dependsOn(shadowJarTask)
    inputs.files(shadowJarTask.outputs.files)
    outputs.file(file("$buildDir/native/${project.name}"))
    doLast {
        exec {
            commandLine(
                "native-image",
                "-H:+ReportUnsupportedElementsAtRuntime",
                "-H:IncludeResources='logback.xml'",
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

task("runNative") {
    dependsOn(nativeImage)
    doLast {
        exec {
            commandLine(nativeImage.outputs.files.singleFile.absolutePath)
        }
    }
}
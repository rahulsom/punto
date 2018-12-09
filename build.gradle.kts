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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.codehaus.groovy:groovy-all:2.5.+")
    testImplementation("org.spockframework:spock-core:1.2-groovy-2.5")
}

application {
    mainClassName = "com.github.rahulsom.punto.AppKt"
}

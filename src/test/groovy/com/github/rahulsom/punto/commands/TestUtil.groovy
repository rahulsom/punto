package com.github.rahulsom.punto.commands

import java.nio.file.Files

class TestUtil {
    static void stage(String userHome, String desc) { home("${userHome}/.punto/staging", desc) }

    static void home(String userHome, String desc) {
        desc.split('\n').each { String line ->
            def (name, content) = line.split(":").collect { it.trim() }
            def absolutePath = "${userHome}/$name"
            def dirName = absolutePath.split("/").dropRight(1).join("/")
            new File(dirName).mkdirs()
            new File(absolutePath).text = content
        }
    }

    static String setupHome(String config) {
        def userHome = Files.createTempDirectory("userhome")
        def configFile = new File(userHome.toFile(), "punto.yaml")

        def USERHOME = userHome.toAbsolutePath().toString()

        configFile.text = """\
            userHome: ${USERHOME}
            puntoHome: ${USERHOME}/.punto
        """.stripIndent() + "\n" + this.class.getResourceAsStream('/sample.punto.yaml').text

        return USERHOME
    }

}

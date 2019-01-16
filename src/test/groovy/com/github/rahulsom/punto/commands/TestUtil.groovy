package com.github.rahulsom.punto.commands

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
}

package com.github.rahulsom.punto

import org.slf4j.LoggerFactory

class App {
    val greeting: String
        get() {
            return "Hello world."
        }
}

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger(App::class.java)
    logger.debug("Debug")
    logger.info("Info")
    logger.warn("Warn")
    logger.error("Error")
    println(App().greeting)
}

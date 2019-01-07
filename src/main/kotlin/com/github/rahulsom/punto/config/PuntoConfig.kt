package com.github.rahulsom.punto.config

import com.github.rahulsom.punto.config.RepoType.*
import java.security.MessageDigest
import java.util.*

open class PuntoConfig {
    var userHome: String = System.getProperty("user.home")
    var puntoHome: String = "$userHome/.punto"
    var repositories: MutableList<Repository> = mutableListOf()
    var ignore: ArrayList<String> = ArrayList()
}

enum class RepoType {
    git, github, gist
}

open class Repository {

    lateinit var mode: RepoType
    lateinit var repo: String
    var branch: String? = null
    val identifier: String = UUID.randomUUID().toString().sha1().takeLast(8)

    var include: ArrayList<String> = ArrayList()
    var into: String? = null

    fun getUrl() =
        when (mode) {
            git -> repo
            github -> "https://github.com/$repo.git"
            gist -> "https://gist.github.com/$repo.git"
        }

    fun getDestination() =
        getUrl()
            .replace(Regex("^https?://"), "")
            .replace(Regex("^git@"), "")
            .replace(Regex("\\.git$"), "")
            .replace(Regex(":"), "/")

}

fun String.sha1() = MessageDigest
    .getInstance("SHA-1")
    .digest(this.toByteArray())
    .fold("") { str, it -> str + "%02x".format(it) }
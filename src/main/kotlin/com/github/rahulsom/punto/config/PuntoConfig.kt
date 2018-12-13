package com.github.rahulsom.punto.config

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

    var include: ArrayList<String> = ArrayList()
    var into: String? = null

    fun getUrl() =
        when (mode) {
            RepoType.git -> repo
            RepoType.github -> "https://github.com/$repo.git"
            RepoType.gist -> "https://gist.github.com/$repo.git"
        }

    fun getDestination() =
        getUrl().replace(Regex("^https?://"), "")
            .replace(Regex("^git@"), "")
            .replace(Regex("\\.git$"), "")
            .replace(Regex(":"), "/")

}

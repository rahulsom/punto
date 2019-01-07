package com.github.rahulsom.punto.commands

import com.github.rahulsom.punto.config.PuntoConfig
import spock.lang.Specification

import java.nio.file.Files

class DiffSpec extends Specification {

    void 'only non ignored files are copied'() {
        given:
        def userHome = Files.createTempDirectory("userhome").toFile().absolutePath
        new File("$userHome/.punto/staging/foo").mkdirs()
        new File("$userHome/.punto/staging/foo/bar").text = 'bar-old'
        new File("$userHome/.punto/staging/foo/snafu").text = 'snafu-old'
        new File("$userHome/.punto/staging/.git").text = '.git-old'
        new File("$userHome/foo").mkdirs()
        new File("$userHome/foo/bar").text = 'bar-new'
        new File("$userHome/foo/snafu").text = 'snafu-new'

        println userHome

        when:
        new Diff().diff(new PuntoConfig().tap {
            it.userHome = userHome
            it.puntoHome = "$userHome/.punto"

            it.ignore = ['foo', '!foo/bar']
        })

        then:
        new File("$userHome/.punto/staging/foo/snafu").text == 'snafu-old'
        new File("$userHome/.punto/staging/.git").text == '.git-old'
        new File("$userHome/.punto/staging/foo/bar").text == 'bar-new'
    }

}

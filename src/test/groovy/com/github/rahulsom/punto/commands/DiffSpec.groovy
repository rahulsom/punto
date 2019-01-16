package com.github.rahulsom.punto.commands

import com.github.rahulsom.punto.config.PuntoConfig
import spock.lang.Specification

import java.nio.file.Files

import static com.github.rahulsom.punto.commands.TestUtil.home
import static com.github.rahulsom.punto.commands.TestUtil.stage

class DiffSpec extends Specification {

    void 'only non ignored files are copied'() {
        given:
        def userHome = Files.createTempDirectory("userhome").toFile().absolutePath
        stage(userHome, """\
            foo/bar: bar-old
            foo/snafu: snafu-old
            .git: .git-old
            """.stripIndent())
        home(userHome, """\
            foo/bar: bar-new
            foo/snafu: snafu-new
            src/foo: footext
            """.stripIndent())

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
        !new File("$userHome/.punto/staging/src/foo").exists()
    }

}

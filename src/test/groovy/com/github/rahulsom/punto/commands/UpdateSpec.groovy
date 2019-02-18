package com.github.rahulsom.punto.commands

import com.github.rahulsom.punto.config.PuntoConfig
import com.github.rahulsom.punto.utils.ExecUtil
import org.junit.Rule
import org.springframework.boot.test.OutputCapture
import spock.lang.Ignore
import spock.lang.Specification

import java.nio.file.Files

import static com.github.rahulsom.punto.commands.TestUtil.home
import static com.github.rahulsom.punto.commands.TestUtil.stage

class UpdateSpec extends Specification {

    @Rule
    OutputCapture capture = new OutputCapture()

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
        new Update().update(new PuntoConfig().tap {
            it.userHome = userHome
            it.puntoHome = "$userHome/.punto"

            it.ignore = ['foo', '!foo/bar']
        })

        then:
        new File("$userHome/foo/snafu").text == 'snafu-new'
        new File("$userHome/foo/bar").text == 'bar-old'
        new File("$userHome/src/foo").exists()
        !new File("$userHome/.git").exists()
    }

    void 'diff output is correct'() {
        given:
        def USERHOME = TestUtil.setupHome(this.class.getResourceAsStream('/sample.punto.yaml').text)

        when:
        App.main('update', '-c', "${USERHOME}/punto.yaml")
        def stageOutput = capture.toString()
        new File("build/output/update.txt").text = stageOutput.replace(USERHOME, "~")

        then:
        stageOutput.replace(USERHOME, "~") == '''\
                ... Dotfiles Staged in ~/.punto/staging
                ... Dotfiles Updated in ~
                '''.stripIndent()

    }
}

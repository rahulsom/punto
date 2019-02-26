package com.github.rahulsom.punto.commands

import com.github.rahulsom.punto.config.PuntoConfig
import com.github.rahulsom.punto.utils.ExecUtil
import org.junit.Rule
import org.springframework.boot.test.OutputCapture
import spock.lang.Specification

import java.nio.file.Files

import static com.github.rahulsom.punto.commands.TestUtil.home
import static com.github.rahulsom.punto.commands.TestUtil.stage

class DiffSpec extends Specification {

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

    void 'diff output is correct'() {
        given:
        def USERHOME = TestUtil.setupHome(this.class.getResourceAsStream('/sample.punto.yaml').text)

        when:
        App.main('update', '-c', "${USERHOME}/punto.yaml")
        def stageOutput = capture.toString()

        then:
        stageOutput.replace(USERHOME, "~") == '''\
                ... Dotfiles Staged in ~/.punto/staging
                ... Dotfiles Updated in ~
                '''.stripIndent()

        when:
        new File(USERHOME, ".screenrc").append("\n# This file has changed")
        App.main('diff', '-c', "${USERHOME}/punto.yaml")
        def diffOutput = capture.toString() - stageOutput
        new File("build/output/diff.txt").text = diffOutput.replace(USERHOME, "~")
        def stagingStatus =
                ExecUtil.exec(new File("$USERHOME/.punto/staging"),
                        "git", "status", "--porcelain=1")
        def personalStatus =
                ExecUtil.exec(new File("$USERHOME/.punto/repositories/github.com/rahulsom/dotfiles"),
                        "git", "status", "--porcelain=1")

        then:
        stagingStatus.err.trim() == "M .screenrc"
        personalStatus.err.trim() == "?? .screenrc"
        diffOutput.replace(USERHOME, "~") == '''\
                ... Dotfiles Staged in ~/.punto/staging
                ... Diff updated in ~/.punto/staging and ~/.punto/repositories/github.com/rahulsom/dotfiles
                '''.stripIndent()

    }

}

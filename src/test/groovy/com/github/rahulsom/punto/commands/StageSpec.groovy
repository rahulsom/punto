package com.github.rahulsom.punto.commands

import com.github.rahulsom.punto.utils.ExecUtil
import org.junit.Rule
import org.springframework.boot.test.OutputCapture
import spock.lang.Specification

import java.nio.file.Files

class StageSpec extends Specification {
    @Rule
    OutputCapture capture = new OutputCapture()

    void 'print help when requested'() {
        when:
        App.main('stage', '-h')
        def output = capture.toString()

        then:
        output.contains('Sets up the staging directory')
        output.contains('-c, --configFile=<configFile>')
        output.contains('-h, --help')
    }

    void 'print error when file not found'() {
        given:
        def configFile = new File('/tmp/foo.yaml')
        if (configFile.exists()) {
            configFile.delete()
        }

        when:
        App.main('stage', '-c', configFile.absolutePath)

        then:
        capture.toString().contains('Could not find file /tmp/foo.yaml')
    }

    void 'Creates empty staging when empty'() {
        given:
        def configFile = new File('/tmp/foo.yaml')
        def userHome = Files.createTempDirectory("userhome")
        configFile.text = """\
            userHome: ${userHome.toAbsolutePath().toString()}
            puntoHome: ${userHome.toAbsolutePath().toString()}/.punto
        """.stripIndent()

        when:
        App.main('stage', '-c', configFile.absolutePath)
        def output = capture.toString()

        then:
        output.contains("... Dotfiles Staged in ${userHome.toAbsolutePath().toString()}/.punto/staging")

        userHome.toFile().list().size() == 0
    }

    void 'Creates copy when one repo exists'() {
        given:
        def configFile = new File('/tmp/foo.yaml')
        def userHome = Files.createTempDirectory("userhome")

        configFile.text = """\
            userHome: ${userHome.toAbsolutePath().toString()}
            puntoHome: ${userHome.toAbsolutePath().toString()}/.punto
            repositories:
            - mode: git
              repo: https://github.com/mathiasbynens/dotfiles.git
              include:
              - '!**/bin/subl'
        """.stripIndent()

        when:
        App.main('stage', '-c', configFile.absolutePath)
        def output = capture.toString()

        then:
        output.contains("... Dotfiles Staged in ${userHome.toAbsolutePath().toString()}/.punto/staging")

        userHome.toFile().list().toList() == ['.punto']
        new File("${userHome.toAbsolutePath().toString()}/.punto/staging").exists()
        new File("${userHome.toAbsolutePath().toString()}/.punto/repositories").exists()
        new File("${userHome.toAbsolutePath().toString()}/.punto/repositories").list().length == 1
    }


    void 'Creates copy when two repos exists'() {
        given:
        def configFile = new File('/tmp/foo.yaml')
        def userHome = Files.createTempDirectory("userhome")

        def USERHOME = userHome.toAbsolutePath().toString()

        configFile.text = """\
            userHome: ${USERHOME}
            puntoHome: ${USERHOME}/.punto
        """.stripIndent() + "\n" + this.class.getResourceAsStream('/sample.punto.yaml').text

        when:
        App.main('stage', '-c', configFile.absolutePath)
        def output = capture.toString()
        new File("build/output/stage.txt").text = output.replace(USERHOME, "~")

        then:
        output.contains("... Dotfiles Staged in ${USERHOME}/.punto/staging")

        userHome.toFile().list().toList() == ['.punto']
        new File("${USERHOME}/.punto/staging").exists()
        new File("${USERHOME}/.punto/repositories").exists()
        new File("${USERHOME}/.punto/repositories").list().length == 2
        new File("${USERHOME}/.punto/repositories/gist.github.com").list().length == 1
        new File("${USERHOME}/.punto/repositories/github.com").list().length == 2

        when:
        def ret = ExecUtil.exec(new File("${USERHOME}/.punto/staging"),
                "git", "log", "--graph", "--oneline", "--decorate"
        )
        new File("build/output/staging.gitlog.txt").text = ret.err

        then:
        ret.err.split("\n").length == 4

    }

}

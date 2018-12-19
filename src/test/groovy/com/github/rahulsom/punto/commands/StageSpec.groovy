package com.github.rahulsom.punto.commands

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

        then:
        capture.toString().contains('Sets up the staging directory')
        capture.toString().contains('-c, --configFile=<configFile>')
        capture.toString().contains('-h, --help')
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

        then:
        capture.toString().contains("Running stage")
        capture.toString().contains("Starting Stage")
        capture.toString().contains("... Dotfiles Staged in ${userHome.toAbsolutePath().toString()}/.punto/staging")
        capture.toString().contains("Finished Stage")

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

        then:
        capture.toString().contains("Running stage")
        capture.toString().contains("Starting Stage")
        capture.toString().contains("... Dotfiles Staged in ${userHome.toAbsolutePath().toString()}/.punto/staging")
        capture.toString().contains("Finished Stage")

        userHome.toFile().list().toList() == ['.punto']
        new File("${userHome.toAbsolutePath().toString()}/.punto/staging").exists()
        new File("${userHome.toAbsolutePath().toString()}/.punto/repositories").exists()
        new File("${userHome.toAbsolutePath().toString()}/.punto/repositories").list().length == 1
    }


    void 'Creates copy when two repos exists'() {
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
            - mode: github
              repo: rahulsom/dotfiles
              branch: demo
        """.stripIndent()

        when:
        App.main('stage', '-c', configFile.absolutePath)

        then:
        capture.toString().contains("Running stage")
        capture.toString().contains("Starting Stage")
        capture.toString().contains("... Dotfiles Staged in ${userHome.toAbsolutePath().toString()}/.punto/staging")
        capture.toString().contains("Finished Stage")

        userHome.toFile().list().toList() == ['.punto']
        new File("${userHome.toAbsolutePath().toString()}/.punto/staging").exists()
        new File("${userHome.toAbsolutePath().toString()}/.punto/repositories").exists()
        new File("${userHome.toAbsolutePath().toString()}/.punto/repositories").list().length == 1
        new File("${userHome.toAbsolutePath().toString()}/.punto/repositories/github.com").list().length == 2
    }

}

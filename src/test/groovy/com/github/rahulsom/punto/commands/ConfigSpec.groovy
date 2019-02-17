package com.github.rahulsom.punto.commands

import org.apache.commons.text.WordUtils
import org.junit.Rule
import org.springframework.boot.test.OutputCapture
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.TimeUnit

class ConfigSpec extends Specification {
    @Rule
    OutputCapture capture = new OutputCapture()

    void 'print help when requested'() {
        when:
        App.main('config', '-h')
        def output = capture.toString()

        then:
        output.contains('Prints configuration')
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
        App.main('config', '-c', configFile.absolutePath)

        then:
        capture.toString().contains('Could not find file /tmp/foo.yaml')
    }

    void 'print defaults when config file is empty'() {
        given:
        def configFile = new File('/tmp/foo.yaml')
        configFile.text = ''

        when:
        App.main('config', '-c', configFile.absolutePath)
        def output = capture.toString()

        then:
        output.contains("userHome '${System.getProperty('user.home')}'")
        output.contains("puntoHome '${System.getProperty('user.home')}/.punto'")
    }

    void 'print userHome correctly'() {
        given:
        def configFile = new File('/tmp/foo.yaml')
        configFile.text = 'userHome: /tmp/loc1'

        when:
        App.main('config', '-c', configFile.absolutePath)
        def output = capture.toString()

        then:
        output.contains("userHome '/tmp/loc1'")
        output.contains("puntoHome '${System.getProperty('user.home')}/.punto'")
    }

    void 'print puntoHome correctly'() {
        given:
        def configFile = new File('/tmp/foo.yaml')
        configFile.text = 'puntoHome: /tmp/loc2'

        when:
        App.main('config', '-c', configFile.absolutePath)
        def output = capture.toString()

        then:
        output.contains("userHome '${System.getProperty('user.home')}'")
        output.contains("puntoHome '/tmp/loc2'")
    }

    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void 'print repos correctly'() {
        given:
        def configFile = new File('/tmp/foo.yaml')
        configFile.text = this.class.getResourceAsStream('/sample.punto.yaml').text

        when:
        App.main('config', '-c', configFile.absolutePath)
        def output = capture.toString()
        new File('build/output/sample.punto.groovy').text =
                output.split('\n').
                        collect {
                            WordUtils.
                                    wrap("|$it", 80, '\n        ', true).
                                    replaceAll("^\\|", '')
                        }.
                        join('\n')

        then:
        output.contains("git('https://github.com/mathiasbynens/dotfiles.git', into: '') {\n" +
                "    include '**/*', '!**/bin/*', '!**/foo/*.sh'\n" +
                "}")
        output.contains("github 'rahulsom/dotfiles', branch: 'demo'")
        output.contains("gist '9def705d16b8995ebdefe731d5d19e5a', into: 'bin'")
        output.contains("github 'rahulsom/dotfiles'")
    }

    void 'print ignores correctly'() {
        given:
        def configFile = new File('/tmp/foo.yaml')
        configFile.text = '''\
            ignore:
              - .git
              - bin/jvm.sh
              - bin/git-changelog
              - .sdkman/candidates'''

        when:
        App.main('config', '-c', configFile.absolutePath)

        then:
        capture.toString().contains("ignore '.git', 'bin/jvm.sh', 'bin/git-changelog', '.sdkman/candidates'")
    }

}

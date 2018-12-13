package com.github.rahulsom.punto.commands

import org.junit.Rule
import org.springframework.boot.test.OutputCapture
import spock.lang.Specification

class ConfigSpec extends Specification {
    @Rule
    OutputCapture capture = new OutputCapture()

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

        then:
        capture.toString().contains("userHome '${System.getProperty('user.home')}'")
        capture.toString().contains("puntoHome '${System.getProperty('user.home')}/.punto'")
    }

    void 'print userHome correctly'() {
        given:
        def configFile = new File('/tmp/foo.yaml')
        configFile.text = 'userHome: /tmp/loc1'

        when:
        App.main('config', '-c', configFile.absolutePath)

        then:
        capture.toString().contains("userHome '/tmp/loc1'")
        capture.toString().contains("puntoHome '${System.getProperty('user.home')}/.punto'")
    }

    void 'print puntoHome correctly'() {
        given:
        def configFile = new File('/tmp/foo.yaml')
        configFile.text = 'puntoHome: /tmp/loc2'

        when:
        App.main('config', '-c', configFile.absolutePath)

        then:
        capture.toString().contains("userHome '${System.getProperty('user.home')}'")
        capture.toString().contains("puntoHome '/tmp/loc2'")
    }

    void 'print repos correctly'() {
        given:
        def configFile = new File('/tmp/foo.yaml')
        configFile.text = '''\
            repositories:
              - mode: git
                repo: https://github.com/mathiasbynens/dotfiles.git
                include:
                  - '**/*\'
                  - '!**/bin/*\'
                  - '!**/foo/*.sh\'
                into: \'\'
              - mode: github
                repo: rahulsom/dotfiles
                branch: demo
              - mode: gist
                repo: 9def705d16b8995ebdefe731d5d19e5a
                into: bin
              - mode: github
                repo: rahulsom/dotfiles'''

        when:
        App.main('config', '-c', configFile.absolutePath)

        then:
        capture.toString().contains("git('https://github.com/mathiasbynens/dotfiles.git', into: '') {\n" +
                "    include '**/*', '!**/bin/*', '!**/foo/*.sh'\n" +
                "}")
        capture.toString().contains("github 'rahulsom/dotfiles', branch: 'demo'")
        capture.toString().contains("gist '9def705d16b8995ebdefe731d5d19e5a', into: 'bin'")
        capture.toString().contains("github 'rahulsom/dotfiles'")
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

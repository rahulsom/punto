package com.github.rahulsom.punto.commands

import com.github.rahulsom.punto.VersionProvider
import org.junit.Rule
import org.springframework.boot.test.OutputCapture
import spock.lang.Specification

class AppSpec extends Specification {

    @Rule
    OutputCapture capture = new OutputCapture()

    void 'application shows help if no args are passed'() {
        when:
        App.main()
        def output = capture.toString()

        then:
        output.contains('Usage: punto')
    }

    void 'application offers help when -h is passed'() {
        when:
        App.main("-h")
        def output = capture.toString()
        new File('build/output/help.txt').text = output

        then:
        output.contains('Usage: punto [-hV]')
        output.contains('Manages dotfiles.')
        output.contains('Show this help message and exit.')
        output.contains('Print version information and exit.')
    }

    void 'application prints version when -V is passed'() {
        when:
        App.main("-V")
        def output = capture.toString()
        new File('build/output/version.txt').text = output

        then:
        output.trim() == new VersionProvider().version[0]
    }

    void 'application errors when -badOption is passed'() {
        when:
        App.main("-badOption")
        def output = capture.toString()

        then:
        output.contains('Unknown option: -badOption')
        output.contains('Usage: punto [-hV]')
        output.contains('Manages dotfiles.')
        output.contains('Show this help message and exit.')
        output.contains('Print version information and exit.')
    }

    void 'application offers help when --help is passed'() {
        when:
        App.main("--help")
        def output = capture.toString()

        then:
        output.contains('Usage: punto [-hV]')
        output.contains('Manages dotfiles.')
        output.contains('Show this help message and exit.')
        output.contains('Print version information and exit.')
    }
}

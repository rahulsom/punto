package com.github.rahulsom.punto.commands

import org.junit.Rule
import org.springframework.boot.test.OutputCapture
import spock.lang.Specification

class AppSpec extends Specification {

    @Rule
    OutputCapture capture = new OutputCapture()

    void 'application has a greeting'() {
        when:
        App.main()

        then:
        capture.toString().contains('Running')
    }

    void 'application offers help when -h is passed'() {
        when:
        App.main("-h")

        then:
        capture.toString().contains('Usage: punto [-hV]')
        capture.toString().contains('Manages dotfiles.')
        capture.toString().contains('Show this help message and exit.')
        capture.toString().contains('Print version information and exit.')
    }

    void 'application errors when -badOption is passed'() {
        when:
        App.main("-badOption")

        then:
        capture.toString().contains('Unknown option: -badOption')
        capture.toString().contains('Usage: punto [-hV]')
        capture.toString().contains('Manages dotfiles.')
        capture.toString().contains('Show this help message and exit.')
        capture.toString().contains('Print version information and exit.')
    }

    void 'application offers help when --help is passed'() {
        when:
        App.main("--help")

        then:
        capture.toString().contains('Usage: punto [-hV]')
        capture.toString().contains('Manages dotfiles.')
        capture.toString().contains('Show this help message and exit.')
        capture.toString().contains('Print version information and exit.')
    }
}

package com.github.rahulsom.punto

import spock.lang.Specification

class AppSpec extends Specification {
    void 'application has a greeting'() {
        given:
        def classUnderTest = new App()

        expect:
        classUnderTest.greeting != null
    }
}

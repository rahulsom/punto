package com.github.rahulsom.punto.config

import spock.lang.Specification
import spock.lang.Unroll


class ConfigParserSpec extends Specification {
    @Unroll
    void 'parsing /#input/ says userHome is #expected'() {
        expect:
        ConfigParser.userHome.parse(input).userHome == expected

        where:
        input               | expected
        'userHome("Foo")'   | 'Foo'
        "userHome('Foo')"   | 'Foo'
        " userHome 'Foo'"   | 'Foo'
        " userHome('Foo')"  | 'Foo'
        "  userHome 'Foo'"  | 'Foo'
        "  userHome('Foo')" | 'Foo'
        "userHome 'Foo'"    | 'Foo'
        'userHome "Foo"'    | 'Foo'
        "userHome ('Foo')"  | 'Foo'
        'userHome ("Foo")'  | 'Foo'
        "userHome  'Foo'"   | 'Foo'
        'userHome  "Foo"'   | 'Foo'
        "userHome  ('Foo')" | 'Foo'
        'userHome  ("Foo")' | 'Foo'
    }

    @Unroll
    void 'parsing /#input/ says repo=#repo, branch=#branch, into=#into'() {
        when:
        def parsed = ConfigParser.git.parse(input)

        then:
        with(parsed) {
            it.mode == RepoType.git
            it.repo == repo
            it.branch == branch
            it.into == into
        }

        where:
        input                                     | repo  | branch | into
        "git 'Foo'"                               | 'Foo' | null   | null
        'git "Foo"'                               | 'Foo' | null   | null
    }

    @Unroll
    void "mapped params are parsed"() {
        expect:
        with(ConfigParser.mappedRepoParams.parse(input)) {
            it.into == into
            it.branch == branch
        }

        where:
        input                              | into  | branch
        ',into:"Foo"'                      | "Foo" | null
        ', into:"Foo"'                     | "Foo" | null
        ', into :"Foo"'                    | "Foo" | null
        ',  into  :  "Foo"'                | "Foo" | null
        ', into: "Foo"'                    | "Foo" | null
        ',branch:"Foo"'                    | null  | "Foo"
        ', branch:"Foo"'                   | null  | "Foo"
        ', branch: "Foo"'                  | null  | "Foo"
        ', branch: "Foo", into: "Bar"'     | "Bar" | "Foo"
        ', into: "Foo", branch: "Bar"'     | "Foo" | "Bar"
        ', into : "Foo" ,  branch : "Bar"' | "Foo" | "Bar"
    }

    @Unroll
    void 'parsing /#input/ says ignore is #expected'() {
        expect:
        ConfigParser.ignore.parse(input).ignore.toSet() == expected.toSet()

        where:
        input                          | expected
        'ignore("Foo")'                | ['Foo']
        'ignore("Foo", "Bar")'         | ['Foo', 'Bar']
        'ignore(\n    "Foo", "Bar"\n)' | ['Foo', 'Bar']
        "ignore 'Foo'"                 | ['Foo']
        "ignore 'Foo','Bar'"           | ['Foo', 'Bar']
        "ignore 'Foo', 'Bar'"          | ['Foo', 'Bar']
        "ignore 'Foo',\n'Bar'"         | ['Foo', 'Bar']
        "ignore 'Foo',\n  'Bar'"       | ['Foo', 'Bar']
        " ignore 'Foo','Bar'"          | ['Foo', 'Bar']
        " ignore 'Foo', 'Bar'"         | ['Foo', 'Bar']
        " ignore 'Foo',\n'Bar'"        | ['Foo', 'Bar']
        " ignore 'Foo',\n  'Bar'"      | ['Foo', 'Bar']
        "  ignore 'Foo','Bar'"         | ['Foo', 'Bar']
        "  ignore 'Foo', 'Bar'"        | ['Foo', 'Bar']
        "  ignore 'Foo',\n'Bar'"       | ['Foo', 'Bar']
        "  ignore 'Foo',\n  'Bar'"     | ['Foo', 'Bar']
    }

    void 'parse real file'() {
        when:
        def puntoConfig = ConfigParser.puntoConfig.parse(this.class.getResourceAsStream('/sample.groovy').text)

        then:
        puntoConfig.userHome == '/foo'
        puntoConfig.puntoHome == '/bar/.punto'
        puntoConfig.ignore.toSet() == ['foo', 'foo1', 'foo2', 'foo3', 'foo4', 'foo5', 'foo6', 'foo7',].toSet()
    }
}

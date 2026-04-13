package com.daluobai.jenkinslib.vars

import com.daluobai.jenkinslib.api.CodeupApi
import groovy.lang.ExpandoMetaClass
import groovy.lang.GroovyShell
import groovy.lang.GroovySystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
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

        Map result = (Map) script.invokeMethod('call', [[token: 'pt-token', organizationId: 'org-id']] as Object[])

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

        Map result = (Map) script.invokeMethod('call', [[token: 'pt-token', organizationId: 'org-id']] as Object[])

        assertEquals(2, result.scannedRepositories)
        assertEquals(1, result.dispatched)
        assertEquals(1, result.failed.size())
        assertEquals('repo-b', result.failed[0].repositoryName)
        assertEquals(1, dispatchedConfigs.size())
    }

    @Test
    void dispatchCodeupRepositoriesRejectsUnsupportedRemoteJenkinsfileAndContinues() {
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

        Map result = (Map) script.invokeMethod('call', [[token: 'pt-token', organizationId: 'org-id']] as Object[])

        assertEquals(2, result.scannedRepositories)
        assertEquals(2, result.scannedFiles)
        assertEquals(1, result.dispatched)
        assertEquals(1, result.rejected.size())
        assertEquals('repo-b', result.rejected[0].repositoryName)
        assertEquals(1, dispatchedConfigs.size())
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

        Map result = (Map) script.invokeMethod('call', [[token: 'pt-token', organizationId: 'org-id', dryRun: true]] as Object[])

        assertEquals(1, result.scannedRepositories)
        assertEquals(1, result.scannedFiles)
        assertEquals(0, result.dispatched)
        assertEquals(0, dispatchCount)
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

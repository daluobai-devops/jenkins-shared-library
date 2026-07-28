package com.daluobai.jenkinslib.steps

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

class StepsBuildNpmTest {

    @Test
    void buildChecksOutSourceWithJenkinsChangelogBeforeRunningNpmBuild() {
        FakeSteps steps = new FakeSteps()
        StepsBuildNpm stepsBuildNpm = new StepsBuildNpm(steps)
        stepsBuildNpm.stepsGit = new FakeStepsGit()

        stepsBuildNpm.build([
                DEFAULT_CONFIG : [
                        docker: [
                                registry: [
                                        domain: 'registry.example.com'
                                ]
                        ]
                ],
                SHARE_PARAM    : [:],
                DEPLOY_PIPELINE: [
                        stepsBuildNpm: [
                                gitUrl          : 'git@example.com:team/web.git',
                                gitBranch       : 'main',
                                buildCMD        : 'npm ci && npm run build',
                                dockerBuildImage: 'registry.example.com/build-npm:20',
                                cacheNodeModules: true
                        ],
                        stepsStorage : [
                                archiveType: 'TAR'
                        ]
                ]
        ])

        assertEquals(1, steps.checkoutCalls.size())
        Map checkoutCall = steps.checkoutCalls[0]
        assertTrue(checkoutCall.changelog)
        assertFalse(checkoutCall.poll)
        assertEquals([[$class: 'CleanBeforeCheckout']], checkoutCall.scm.extensions)
        assertEquals([[name: 'main']], checkoutCall.scm.branches)
        assertEquals([[url: 'git@example.com:team/web.git', credentialsId: 'ssh-git']], checkoutCall.scm.userRemoteConfigs)
        assertTrue(steps.dirCalls.contains('/workspace/code/code'))
        assertTrue(steps.shScripts.any { it.contains('npm ci && npm run build') })
        assertFalse(steps.shScripts.any { it.contains('git clone') })
    }

    @Test
    void buildSkipsSshSetupForHttpsAndCreatesFlatZipArchive() {
        FakeSteps steps = new FakeSteps()
        StepsBuildNpm stepsBuildNpm = new StepsBuildNpm(steps)
        FakeStepsGit fakeStepsGit = new FakeStepsGit()
        stepsBuildNpm.stepsGit = fakeStepsGit

        stepsBuildNpm.build([
                DEFAULT_CONFIG : [docker: [registry: [domain: 'docker.io']]],
                SHARE_PARAM    : [:],
                DEPLOY_PIPELINE: [
                        stepsBuildNpm: [
                                gitUrl: 'https://example.com/team/web.git', gitBranch: 'main',
                                buildCMD: 'npm ci && npm run build', cacheNodeModules: false
                        ],
                        stepsStorage : [archiveType: 'ZIP']
                ]
        ])

        assertEquals(0, fakeStepsGit.saveKeyCalls)
        assertEquals(0, fakeStepsGit.keyscanCalls)
        assertTrue(steps.shScripts.any { it.contains('zip -r /workspace/package/app.zip .') })
        assertFalse(steps.shScripts.any { it.contains('app.zip ./dist') })
    }

    @Test
    void buildRunsAndPackagesFromConfiguredSourceDirectory() {
        FakeSteps steps = new FakeSteps()
        StepsBuildNpm build = new StepsBuildNpm(steps)
        build.stepsGit = new FakeStepsGit()

        build.build([
                DEFAULT_CONFIG: [docker: [registry: [domain: 'docker.io']]], SHARE_PARAM: [:],
                DEPLOY_PIPELINE: [
                        stepsBuildNpm: [gitUrl: 'https://example/app.git', gitBranch: 'abc123',
                                        sourceDirectory: 'frontend/app', buildCMD: 'npm run build'],
                        stepsStorage: [archiveType: 'TAR']
                ]
        ])

        assertTrue(steps.shScripts.any { it.contains("cd '/workspace/code/code/frontend/app'") })
        assertTrue(steps.shScripts.any { it.contains("-C '/workspace/code/code/frontend/app/dist' .") })
    }

    static class FakeSteps {
        Map env = [WORKSPACE: '/workspace']
        Map currentBuild = [projectName: 'web-job']
        FakeDocker docker = new FakeDocker()
        List<String> shScripts = []
        List<Map> checkoutCalls = []
        List<String> dirCalls = []

        void sh(String script) {
            shScripts.add(script)
        }

        void withDockerRegistry(Map args, Closure body) {
            body.call()
        }

        void dir(String path, Closure body) {
            dirCalls.add(path)
            body.call()
        }

        Map checkout(Map args) {
            checkoutCalls.add(args)
            return [:]
        }
    }

    static class FakeDocker {
        FakeDockerImage image(String imageName) {
            return new FakeDockerImage(imageName)
        }
    }

    static class FakeDockerImage {
        String imageName

        FakeDockerImage(String imageName) {
            this.imageName = imageName
        }

        void pull() {
        }

        void inside(String args, Closure body) {
            body.call()
        }
    }

    static class FakeStepsGit {
        int saveKeyCalls = 0
        int keyscanCalls = 0

        void saveJenkinsSSHKey(String credentialsId, String path) {
            saveKeyCalls++
        }

        void sshKeyscan(String gitUrl, String filePath) {
            keyscanCalls++
        }
    }
}

package com.daluobai.jenkinslib.delivery

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class JenkinsArtifactStoreTest {
    @Test
    void rejectsDuplicateIdentityAcrossStoreInstancesInSameBuild() {
        def steps = new Expando(env: [JOB_NAME: 'delivery', BUILD_NUMBER: '42'])
        new JenkinsArtifactStore(steps).put('test/app/execution-1', [path: 'package/app.jar'])

        IllegalStateException failure = assertThrows(IllegalStateException.class) {
            new JenkinsArtifactStore(steps).put('test/app/execution-1', [path: 'package/app.jar'])
        }

        assertTrue(failure.message.contains('已存在'))
    }

    @Test
    void usesNamespacedPhysicalStashForEachIdentity() {
        List<Map> stashes = []
        def steps = new Expando(env: [JOB_NAME: 'delivery', BUILD_NUMBER: '42'])
        steps.stash = { Map args -> stashes.add(args) }

        JenkinsArtifactStore store = new JenkinsArtifactStore(steps)
        store.put('test/app-a/execution-1', [path: 'package/app.jar', storageTarget: 'JENKINS_STASH'])
        store.put('test/app-b/execution-1', [path: 'package/app.jar', storageTarget: 'JENKINS_STASH'])

        assertTrue(stashes*.name.toSet().size() == 2)
        assertTrue(stashes.every { it.name ==~ /delivery-[a-f0-9]{64}/ })
    }
}

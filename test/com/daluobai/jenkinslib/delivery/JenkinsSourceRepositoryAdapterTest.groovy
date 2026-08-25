package com.daluobai.jenkinslib.delivery

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

class JenkinsSourceRepositoryAdapterTest {

    @Test
    void directoryExistsQuotesTheCompleteGitObjectName() {
        RecordingSteps steps = new RecordingSteps()
        JenkinsSourceRepositoryAdapter adapter = new JenkinsSourceRepositoryAdapter(steps)

        boolean exists = adapter.directoryExists([
                repository: 'git@example.com:team/app.git',
                directory : 'aurora-web/aurora-admin'
        ], '97e400d9417e0b7b4e16c8c0d078431e7c575a83')

        assertTrue(exists)
        assertTrue(steps.shellCalls.size() == 1)
        String script = steps.shellCalls[0].script
        assertTrue(script.contains("cat-file -e 'FETCH_HEAD:aurora-web/aurora-admin'"))
        assertFalse(script.contains('cat-file -e "FETCH_HEAD:\'aurora-web/aurora-admin\'"'))
    }

    private static class RecordingSteps {
        final List<Map> shellCalls = []

        Object sh(Map arguments) {
            shellCalls.add(arguments)
            return 0
        }
    }
}

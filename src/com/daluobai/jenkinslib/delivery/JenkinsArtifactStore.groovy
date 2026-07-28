package com.daluobai.jenkinslib.delivery

import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class JenkinsArtifactStore implements ArtifactStore {
    private final def steps
    private final Set<String> identities = [] as Set<String>

    JenkinsArtifactStore(def steps) {
        this.steps = steps
    }

    @Override
    Map put(String identity, Map artifact) {
        Set<String> buildIdentities = readBuildIdentities()
        if (buildIdentities.contains(identity) || !identities.add(identity)) {
            throw new IllegalStateException("存储产物身份已存在: ${identity}")
        }
        buildIdentities.add(identity)
        steps.env.DELIVERY_ARTIFACT_IDENTITIES = JsonOutput.toJson(buildIdentities.sort())
        String safeIdentity = MessageDigest.getInstance('SHA-256')
                .digest(identity.getBytes(StandardCharsets.UTF_8)).encodeHex().toString()
        String storageTarget = artifact.storageTarget?.toString()
        if (storageTarget == 'JENKINS_STASH') {
            steps.stash(name: "delivery-${safeIdentity}", includes: artifact.path.toString(), useDefaultExcludes: false)
        } else if (storageTarget in ['JENKINS', 'JENKINS_ARCHIVE']) {
            String archiveRoot = "delivery-artifacts/${safeIdentity}"
            steps.sh("mkdir -p '${archiveRoot}' && cp -r ${artifact.path} '${archiveRoot}/'")
            steps.archiveArtifacts(artifacts: "${archiveRoot}/**", fingerprint: true, allowEmptyArchive: false)
        }
        Map stored = new LinkedHashMap(artifact)
        stored.identity = identity
        stored.reference = "jenkins://${steps.env.JOB_NAME}/${steps.env.BUILD_NUMBER}/${identity}".toString()
        return stored
    }

    private Set<String> readBuildIdentities() {
        String serialized = steps.env.DELIVERY_ARTIFACT_IDENTITIES?.toString()
        if (!serialized) {
            return [] as Set<String>
        }
        try {
            return (new JsonSlurperClassic().parseText(serialized) as Collection)
                    .collect { it.toString() } as Set<String>
        } catch (Throwable failure) {
            throw new IllegalStateException('Jenkins Build Record中的产物身份记录无效', failure)
        }
    }
}

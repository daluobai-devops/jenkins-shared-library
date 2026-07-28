package com.daluobai.jenkinslib.delivery

class InMemoryArtifactStore implements ArtifactStore {
    final Map<String, Map> artifacts = [:]
    RuntimeException failure

    @Override
    Map put(String identity, Map artifact) {
        if (failure != null) {
            throw failure
        }
        if (artifacts.containsKey(identity)) {
            throw new IllegalStateException("存储产物身份已存在: ${identity}")
        }
        Map stored = new LinkedHashMap(artifact)
        stored.identity = identity
        stored.reference = "memory://${identity}".toString()
        artifacts[identity] = stored
        return stored
    }
}

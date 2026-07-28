package com.daluobai.jenkinslib.delivery

interface ArtifactStore extends Serializable {
    Map put(String identity, Map artifact)
}

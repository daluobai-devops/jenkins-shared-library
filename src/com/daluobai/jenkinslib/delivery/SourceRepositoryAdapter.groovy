package com.daluobai.jenkinslib.delivery

interface SourceRepositoryAdapter extends Serializable {
    String resolveRevision(Map source)

    boolean directoryExists(Map source, String revision)

    void checkout(Map source, String revision)
}

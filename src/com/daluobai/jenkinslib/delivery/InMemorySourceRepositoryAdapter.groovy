package com.daluobai.jenkinslib.delivery

class InMemorySourceRepositoryAdapter implements SourceRepositoryAdapter {
    final Map<String, Map<String, String>> revisions
    final Map<String, Collection<String>> directories
    final List<String> checkedOutRevisions = []

    InMemorySourceRepositoryAdapter(Map revisions = [:], Map directories = [:]) {
        this.revisions = revisions
        this.directories = directories
    }

    @Override
    String resolveRevision(Map source) {
        String repository = source.repository?.toString()
        String reference = source.reference?.toString()
        String revision = revisions[repository]?.get(reference)
        if (!revision) {
            throw new IllegalArgumentException("无法解析源码引用: ${repository}@${reference}")
        }
        return revision
    }

    @Override
    boolean directoryExists(Map source, String revision) {
        String key = "${source.repository}@${revision}"
        return directories[key]?.contains(source.directory?.toString()) == true
    }

    @Override
    void checkout(Map source, String revision) {
        checkedOutRevisions.add(revision)
    }
}

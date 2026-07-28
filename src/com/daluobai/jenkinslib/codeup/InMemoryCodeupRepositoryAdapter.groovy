package com.daluobai.jenkinslib.codeup

class InMemoryCodeupRepositoryAdapter implements CodeupRepositoryAdapter {
    final List<Map<String, Object>> repositories
    final Map<String, List<Map<String, Object>>> filesByRepositoryId
    final Map<String, Map> reads
    final Map<String, Integer> readCounts = [:].withDefault { 0 }

    InMemoryCodeupRepositoryAdapter(List<Map<String, Object>> repositories = [],
                                    Map<String, List<Map<String, Object>>> filesByRepositoryId = [:],
                                    Map<String, Map> reads = [:]) {
        this.repositories = repositories
        this.filesByRepositoryId = filesByRepositoryId
        this.reads = reads
    }

    @Override
    List<Map<String, Object>> listRepositories(Map connection) {
        return repositories.collect { new LinkedHashMap(it) }
    }

    @Override
    List<Map<String, Object>> listFiles(Map connection, Map repository, String ref) {
        return (filesByRepositoryId[repository.id?.toString()] ?: []).collect { new LinkedHashMap(it) }
    }

    @Override
    Map readFile(Map connection, Map repository, String path, String ref) {
        String key = "${repository.id}:${path}".toString()
        readCounts[key] = readCounts[key] + 1
        return new LinkedHashMap(reads[key] ?: [status: 'WITHDRAWN', revision: ref])
    }
}

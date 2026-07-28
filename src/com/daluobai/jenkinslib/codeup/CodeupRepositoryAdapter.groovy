package com.daluobai.jenkinslib.codeup

interface CodeupRepositoryAdapter extends Serializable {
    List<Map<String, Object>> listRepositories(Map connection)

    List<Map<String, Object>> listFiles(Map connection, Map repository, String ref)

    Map readFile(Map connection, Map repository, String path, String ref)
}

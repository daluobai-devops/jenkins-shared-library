package com.daluobai.jenkinslib.codeup

import com.daluobai.jenkinslib.api.CodeupApi

class ProductionCodeupRepositoryAdapter implements CodeupRepositoryAdapter {
    private final CodeupApi api

    ProductionCodeupRepositoryAdapter(def steps) {
        this.api = new CodeupApi(steps)
    }

    @Override
    List<Map<String, Object>> listRepositories(Map connection) {
        return api.listRepositories(connection.domain.toString(), connection.token.toString(), connection.organizationId.toString())
    }

    @Override
    List<Map<String, Object>> listFiles(Map connection, Map repository, String ref) {
        return api.listFiles(
                connection.domain.toString(), connection.token.toString(), repository.id.toString(), '', ref,
                'RECURSIVE', connection.organizationId.toString()
        )
    }

    @Override
    Map readFile(Map connection, Map repository, String path, String ref) {
        Map record = api.getFileRecord(
                connection.domain.toString(), connection.token.toString(), repository.id.toString(), path, ref,
                connection.organizationId.toString()
        )
        if (record.exists != true) {
            return [status: 'WITHDRAWN', revision: null]
        }
        if (record.content == null) {
            return [status: 'FAILED', reason: '配置内容读取为空', revision: record.revision]
        }
        if (!record.revision?.toString()?.trim()) {
            return [status: 'FAILED', reason: 'Codeup文件响应缺少实际Commit SHA', revision: null]
        }
        return [status: 'FOUND', content: record.content, revision: record.revision]
    }
}

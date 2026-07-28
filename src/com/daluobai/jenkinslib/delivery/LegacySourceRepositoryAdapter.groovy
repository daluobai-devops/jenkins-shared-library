package com.daluobai.jenkinslib.delivery

/**
 * 旧入口继续由历史构建策略完成源码检出；此 adapter 只把旧引用带入统一模型。
 */
class LegacySourceRepositoryAdapter implements SourceRepositoryAdapter {
    @Override
    String resolveRevision(Map source) {
        return source.reference?.toString() ?: 'legacy'
    }

    @Override
    boolean directoryExists(Map source, String revision) {
        return true
    }

    @Override
    void checkout(Map source, String revision) {
        // 历史构建 adapter 使用统一配置中的固定引用自行检出。
    }
}

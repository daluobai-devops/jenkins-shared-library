---
status: accepted
---

# Build from the unified source working copy

当前统一交付入口只检出一份固定到已解析源码版本的源码工作副本，Maven 和 NPM 构建都直接消费该副本，不再由旧构建器执行第二次检出。旧入口继续保持原有自检出行为；统一入口通过独立适配边界复用构建能力，避免重复网络操作、凭据不一致及两个工作副本内容偏离，代价是需要显式适配旧构建器对 `code/code` 目录的历史假设。

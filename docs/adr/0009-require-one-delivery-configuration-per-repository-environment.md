---
status: superseded by ADR-0015
---

# Require one delivery configuration per repository and environment

当前领域模型规定一个源码仓库对应一个应用，并且一个仓库在一个目标环境中最多匹配一份仓库交付配置：零份表示正常跳过，一份表示执行，多份属于配置歧义并拒绝该仓库。该选择以明确应用边界和消除递归扫描歧义为优先，暂不支持 monorepo 中多个可交付应用；未来如需支持，必须先引入明确的可交付单元模型。

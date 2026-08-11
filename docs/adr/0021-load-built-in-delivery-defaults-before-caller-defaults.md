---
status: accepted
---

# Load built-in delivery defaults before caller defaults

当前统一交付入口自动加载独立的统一结构默认配置文件，并按照“共享库默认配置 < 调用方默认覆盖 < 扩展配置 < 主交付配置 < 执行覆盖参数”的顺序深度合并为有效交付配置。保留调用方已有的 `defaults` 输入并赋予其覆盖内置默认值的语义，可以兼容现有调用方式；独立默认文件不复用旧入口的 `config.json`，避免旧结构进入统一实现。

内置文件缺失、格式错误或根节点不是对象时立即终止配置解析。为保持返回契约兼容，现有四项 `configurationSources` 保持不变，内置文件路径记录在 `configurationProvenance.sharedLibraryDefaults`。

内置文件只保存跨应用、跨环境稳定的基础值，首版包括公共镜像仓库、Git 源码凭据别名 `ssh-git` 和保留但暂未接入部署执行的节点凭据别名 `ssh-jenkins`；它不包含应用标识、目标环境、部署节点、目标路径、端口或交付阶段开关。

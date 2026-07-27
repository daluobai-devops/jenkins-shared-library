# Use Jenkins build records for delivery history

Jenkins Build Record 是当前应用交付历史的唯一事实来源，不另建交付数据库。每次执行至少记录应用标识、目标环境、源码仓库与引用、实际源码 Commit SHA、配置来源、启用阶段、产物引用、部署节点和交付结果。仓库分发 Job 还必须为每个候选可交付单元记录“仓库稳定标识 + 配置文件相对路径”形成的单元标识、实际配置 Commit SHA 和独立处理结果；配置与源码分别记录其引用和已解析版本。直接交付 Job 不生成额外单元标识，由 Jenkins Job 全名和 Build Number 标识执行。该选择复用现有 Jenkins 审计能力并降低系统复杂度，接受交付历史的可用性与 Jenkins 记录保留策略绑定。

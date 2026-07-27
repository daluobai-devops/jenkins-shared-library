# Authorize remote command values in the environment job

仓库交付配置中的构建和部署命令必须由环境交付 Job 通过精确值白名单授权；生产环境禁止绕过命令字段校验。仓库维护者可以选择已批准命令，但不能在 Jenkins 节点与凭据上下文中引入任意 Shell。该边界保留配置驱动交付的灵活性，同时阻止远端仓库把配置文件变成未经授权的代码执行入口。

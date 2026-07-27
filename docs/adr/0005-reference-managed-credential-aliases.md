# Reference managed credentials by alias

仓库交付配置和 Job 内联配置只能保存 Jenkins 凭据别名或 ID，不得保存密码、Token、私钥等真实凭据；真实凭据统一由 Jenkins Credentials 管理并在执行时解析。仓库分发 Job 自身访问 Codeup 或 GitLab 的 API Token 也只属于该 Job，不下放到被扫描仓库。该边界使交付配置可以版本化和分发，同时避免源码仓库获得或泄露执行环境凭据。

# Parse repository Jenkinsfiles as restricted declarations

仓库分发 Job 不把远端 Jenkinsfile 作为任意 Jenkins Pipeline 执行，而是将其解析为受限交付声明：只接受受支持的配置结构和一个白名单共享库步骤调用，并继续执行既有的方法、命令和凭据授权校验。该选择牺牲仓库自定义 Pipeline 逻辑的灵活性，以防扫描整个代码托管空间时让远端仓库获得分发 Job 的任意代码执行权限。

import com.daluobai.jenkinslib.api.CodeupApi

/**
 * 通过 vars 暴露 Codeup API 给 Jenkinsfile 使用
 */
def call() {
    return new CodeupApi(this)
}

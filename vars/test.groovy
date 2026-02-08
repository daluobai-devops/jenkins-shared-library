import com.daluobai.jenkinslib.HutoolProbe

def call(Map customConfig) {
    // 通过 vars 暴露给 Jenkinsfile 使用
    return HutoolProbe.run()
}

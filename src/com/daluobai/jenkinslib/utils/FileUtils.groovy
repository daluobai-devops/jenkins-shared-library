package com.daluobai.jenkinslib.utils

import com.daluobai.jenkinslib.utils.IoUtils
import com.daluobai.jenkinslib.utils.AssertUtils
import com.daluobai.jenkinslib.utils.StrUtils
import com.daluobai.jenkinslib.utils.HttpUtils
import com.daluobai.jenkinslib.constant.EFileReadType

import java.nio.charset.Charset
/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title 
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 * @create 2023/4/25 12:10
 */
class FileUtils implements Serializable {

    def steps

    FileUtils(steps) { this.steps = steps }

    /**
     * 从完整路径读取文件
     * @param fileFullPath
     * @return
     */
    def readStringFromFullPath(String fileFullPath) {
        AssertUtils.notBlank(fileFullPath, "fileFullPath为空");
        def configType = StrUtils.subBefore(fileFullPath, ":", false)
        //获取后缀
        def path = StrUtils.subAfter(fileFullPath, ":", false)
        EFileReadType extendConfigType = EFileReadType.get(configType)
        return this.readString(extendConfigType, path)
    }

/**
 * 修改文件
 * @param path 文件路径
 * @return
 */
    def readString(EFileReadType eConfigType, String path) {
        AssertUtils.notNull(eConfigType, "配置类型为空");
        AssertUtils.notBlank(path, "path为空")
        def fileString = ""
        if (eConfigType == EFileReadType.HOST_PATH) {
            def file = new File(path)
            boolean isFile = IoUtils.isFile(file)
            AssertUtils.isTrue(isFile, "配置文件不存在")
            fileString = IoUtils.readString(file, Charset.forName("utf-8"))
        } else if (eConfigType == EFileReadType.RESOURCES) {
            fileString = steps.libraryResource path
        } else if (eConfigType == EFileReadType.URL) {
            fileString = HttpUtils.get(path)
        } else {
            throw new Exception("暂不支持的配置类型")
        }
        return fileString
    }

    /**
     * 通过sh写文件
     * @param path
     * @param content
     * @return
     */
    def writeFileBySH(String path, String content) {
        //这个格式不能动，不然无法报错，content 和 EOF结束必须靠最前面
        steps.sh """
                        cat > ${path} <<-'EOF'
${content}
EOF
                        """
    }

}
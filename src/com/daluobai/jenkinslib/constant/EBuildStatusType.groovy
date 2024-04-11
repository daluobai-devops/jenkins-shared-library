package com.daluobai.jenkinslib.constant

/**
 * @author daluobai@outlook.com
 * version 1.0.0
 * @title
 * @description https://github.com/daluobai-devops/jenkins-shared-library
 *
 * @create 2023/4/25 12:10
 */
enum EBuildStatusType {

    FAILED("FAILED", "失败"),
    ABORTED("ABORTED", "终止"),
    SUCCESS("SUCCESS", "成功")
    ;
    private String code;

    private String message;

    EBuildStatusType(String code, String message) {
        this.code = code;
        this.message = message;
    }

    static EBuildStatusType get(String code) {
        EBuildStatusType[] values = values();
        for (EBuildStatusType object : values) {
            if (object.code == code) {
                return object;
            }
        }
        return null;
    }

    @Override
    public String toString(){
        return code
    }
}

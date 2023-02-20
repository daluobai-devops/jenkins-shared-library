package com.daluobai.jenkinslib.constant

enum EConfigType{

    URL("URL", "从URL获取"),
    HOST_PATH("HOST_PATH", "从宿主机目录获取"),
    RESOURCES("RESOURCES", "从resources目录获取")
    ;
    private String code;

    private String message;

    EConfigType(String code, String message) {
        this.code = code;
        this.message = message;
    }

    static EConfigType get(String code) {
        EConfigType[] values = EConfigType.values();
        for (EConfigType object : values) {
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

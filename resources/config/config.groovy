[
        "DEFAULT_CONFIG": [
                "docker": [
                        "registry": [//如果需要使用私有仓库，需要配置此项。否则删掉这个
                                     "domain"       : "docker.io",
                                     "credentialsId": "docker-secret"//在 jenkins密钥管理中的id
                        ]
                ]
        ],
        "SHARE_PARAM"    : [
                //app 名称
                "appName": "test1",
                //钉钉 token
                "dingdingToken"  : "111",
                //飞书 token
                "feishuToken"  : "3333"
        ]
]
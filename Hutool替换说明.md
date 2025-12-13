# Hutool替换说明

本项目已创建自定义工具类来替换Hutool依赖，所有工具类位于 `com.daluobai.jenkinslib.utils` 包下。

## 工具类对照表

| Hutool类 | 自定义工具类 | 说明 |
|---------|------------|------|
| `cn.hutool.core.util.StrUtil` | `StrUtils` | 字符串工具类 |
| `cn.hutool.core.lang.Assert` | `AssertUtils` | 断言工具类 |
| `cn.hutool.core.io.FileUtil` | `IoUtils` | 文件IO工具类 |
| `cn.hutool.http.HttpUtil` | `HttpUtils` | HTTP工具类 |
| `cn.hutool.http.HttpRequest` | `HttpUtils.HttpRequest` | HTTP请求类 |
| `cn.hutool.json.JSONUtil` | `JsonUtils` | JSON工具类 |
| `cn.hutool.json.JSONObject` | `JsonUtils.JSONObject` | JSON对象类 |
| `cn.hutool.core.util.ObjectUtil` | `ObjUtils` | 对象工具类 |
| `cn.hutool.core.date.DateUtil` | `DateUtils` | 日期工具类 |
| `cn.hutool.core.util.NumberUtil` | `NumberUtils` | 数字工具类 |

## 替换步骤

### 1. 删除Hutool依赖

在 `build.gradle` 中移除Hutool依赖：
```groovy
// 删除这一行
implementation 'cn.hutool:hutool-all:5.8.42'
```

### 2. 移除@Grab注解

在所有文件中移除Hutool的@Grab注解：
```groovy
// 删除这一行
@Grab('cn.hutool:hutool-all:5.8.42')
```

### 3. 替换import语句

按照以下规则替换import语句：

#### 字符串工具类
```groovy
// 旧的
import cn.hutool.core.util.StrUtil

// 新的
import com.daluobai.jenkinslib.utils.StrUtils
```

#### 断言工具类
```groovy
// 旧的
import cn.hutool.core.lang.Assert

// 新的
import com.daluobai.jenkinslib.utils.AssertUtils as Assert
// 或者直接替换类名
import com.daluobai.jenkinslib.utils.AssertUtils
```

#### 文件工具类
```groovy
// 旧的
import cn.hutool.core.io.FileUtil

// 新的
import com.daluobai.jenkinslib.utils.IoUtils
```

#### HTTP工具类
```groovy
// 旧的
import cn.hutool.http.HttpUtil
import cn.hutool.http.HttpRequest

// 新的
import com.daluobai.jenkinslib.utils.HttpUtils
```

#### JSON工具类
```groovy
// 旧的
import cn.hutool.json.JSONUtil
import cn.hutool.json.JSONObject

// 新的
import com.daluobai.jenkinslib.utils.JsonUtils
```

#### 对象工具类
```groovy
// 旧的
import cn.hutool.core.util.ObjectUtil

// 新的
import com.daluobai.jenkinslib.utils.ObjUtils
```

#### 日期工具类
```groovy
// 旧的
import cn.hutool.core.date.DateUtil

// 新的
import com.daluobai.jenkinslib.utils.DateUtils
```

#### 数字工具类
```groovy
// 旧的
import cn.hutool.core.util.NumberUtil

// 新的
import com.daluobai.jenkinslib.utils.NumberUtils
```

### 4. 替换方法调用

大部分方法名保持一致，只需要替换类名即可：

```groovy
// 旧的
StrUtil.isBlank(str)
Assert.notNull(obj, "对象为空")
FileUtil.readString(file, charset)
HttpUtil.get(url)
JSONUtil.parseObj(jsonStr)
ObjectUtil.isNotEmpty(obj)
DateUtil.format(date)
NumberUtil.parseInt(str)

// 新的
StrUtils.isBlank(str)
AssertUtils.notNull(obj, "对象为空")
IoUtils.readString(file, charset)
HttpUtils.get(url)
JsonUtils.parseObj(jsonStr)
ObjUtils.isNotEmpty(obj)
DateUtils.format(date)
NumberUtils.parseInt(str)
```

#### 特殊替换

**FileUtil -> IoUtils**
```groovy
// 旧的
FileUtil.isFile(file)

// 新的
IoUtils.isFile(file)
```

**ObjectUtil -> ObjUtils**
```groovy
// 旧的
ObjectUtil.isNull(obj)

// 新的
ObjUtils.isNull(obj)
```

**JSONObject使用**
```groovy
// 旧的
cn.hutool.json.JSONObject json = JSONUtil.parseObj(str)
String value = json.getStr("key")

// 新的
JsonUtils.JSONObject json = JsonUtils.parseObj(str)
String value = json.getStr("key")
```

**HttpRequest链式调用**
```groovy
// 旧的
String response = HttpRequest.post(url)
    .contentType("application/json;charset=utf-8")
    .body(jsonStr)
    .execute()
    .body()

// 新的
String response = HttpUtils.HttpRequest.post(url)
    .contentType("application/json;charset=utf-8")
    .body(jsonStr)
    .execute()
    .body()
```

## 完整示例

### 替换前（使用Hutool）

```groovy
package com.daluobai.jenkinslib.utils

@Grab('cn.hutool:hutool-all:5.8.42')
import cn.hutool.core.lang.Assert
import cn.hutool.core.io.FileUtil
import cn.hutool.core.util.StrUtil
import cn.hutool.http.HttpUtil

class ConfigUtils implements Serializable {
    def readConfig(String path) {
        Assert.notBlank(path, "path为空")
        def configType = StrUtil.subBefore(path, ":", false)
        
        if (FileUtil.isFile(new File(path))) {
            def content = HttpUtil.get(url)
            return content
        }
    }
}
```

### 替换后（使用自定义工具类）

```groovy
package com.daluobai.jenkinslib.utils

import com.daluobai.jenkinslib.utils.AssertUtils
import com.daluobai.jenkinslib.utils.IoUtils
import com.daluobai.jenkinslib.utils.StrUtils
import com.daluobai.jenkinslib.utils.HttpUtils

class ConfigUtils implements Serializable {
    def readConfig(String path) {
        AssertUtils.notBlank(path, "path为空")
        def configType = StrUtils.subBefore(path, ":", false)
        
        if (IoUtils.isFile(new File(path))) {
            def content = HttpUtils.get(url)
            return content
        }
    }
}
```

## 注意事项

1. **所有工具类都是静态方法**，可以直接通过类名调用
2. **IoUtils替换了FileUtil**，注意类名的变化
3. **ObjUtils替换了ObjectUtil**，注意类名的变化
4. **JsonUtils.JSONObject替换了cn.hutool.json.JSONObject**
5. **AssertUtils可以使用别名**：`import com.daluobai.jenkinslib.utils.AssertUtils as Assert`
6. **所有工具类都实现了Serializable接口**，支持在Jenkins Pipeline中使用
7. **保留了Groovy的简洁语法**，如默认参数、闭包等特性

## API兼容性

本工具类库保持了与Hutool相同的API设计，大部分情况下只需要替换import语句即可，代码逻辑无需修改。

如果遇到某个Hutool方法在自定义工具类中找不到，请检查：
1. 方法名是否有变化
2. 参数顺序是否一致
3. 返回值类型是否匹配

如需添加新的工具方法，请参考Hutool的实现方式，保持API风格一致。

package com.daluobai.jenkinslib;

import cn.hutool.core.util.StrUtil;

import java.io.Serializable;

class HutoolProbe implements Serializable {

  static String run() {
    // 只要能编译/运行到这里，就证明 hutool 在 classpath 里
    return "Hutool OK, isBlank(' ')=" + StrUtil.isBlank(" ");
  }
}

package com.daluobai.jenkinslib.steps

/**
 * @title Test
 * @description <TODO description class purpose>
 * @author wuzhao
 *  version 1.0.0
 * @create 2023/7/9 22:55
 */
class Test implements ITest {
    @Override
    def call(def steps) {
        steps.echo "test111111"
    }
}

package com.daluobai.jenkinslib.lib.hutool.core.lang.generator;

import com.daluobai.jenkinslib.lib.hutool.core.lang.ObjectId;
import com.daluobai.jenkinslib.lib.hutool.core.lang.generator.Generator;

/**
 * ObjectId生成器
 *
 * @author looly
 * @since 5.4.3
 */
public class ObjectIdGenerator implements Generator<String> {
	@Override
	public String next() {
		return ObjectId.next();
	}
}

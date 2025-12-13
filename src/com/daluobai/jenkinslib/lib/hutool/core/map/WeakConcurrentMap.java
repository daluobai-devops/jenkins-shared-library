package com.daluobai.jenkinslib.lib.hutool.core.map;

import com.daluobai.jenkinslib.lib.hutool.core.map.ReferenceConcurrentMap;
import com.daluobai.jenkinslib.lib.hutool.core.map.SafeConcurrentHashMap;
import com.daluobai.jenkinslib.lib.hutool.core.util.ReferenceUtil;

import java.lang.ref.Reference;
import java.util.concurrent.ConcurrentMap;

/**
 * 线程安全的WeakMap实现<br>
 * 参考：jdk.management.resource.internal.WeakKeyConcurrentHashMap
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author looly
 * @since 5.8.0
 * @deprecated 请使用{@link com.daluobai.jenkinslib.lib.hutool.core.map.reference.WeakKeyConcurrentMap}
 */
public class WeakConcurrentMap<K, V> extends ReferenceConcurrentMap<K, V> {

	/**
	 * 构造
	 */
	public WeakConcurrentMap() {
		this(new SafeConcurrentHashMap<>());
	}

	/**
	 * 构造
	 *
	 * @param raw {@link ConcurrentMap}实现
	 */
	public WeakConcurrentMap(ConcurrentMap<Reference<K>, V> raw) {
		super(raw, ReferenceUtil.ReferenceType.WEAK);
	}
}

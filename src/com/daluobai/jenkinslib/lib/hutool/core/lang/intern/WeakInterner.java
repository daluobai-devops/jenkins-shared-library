package com.daluobai.jenkinslib.lib.hutool.core.lang.intern;

import com.daluobai.jenkinslib.lib.hutool.core.lang.intern.Interner;
import com.daluobai.jenkinslib.lib.hutool.core.map.reference.WeakKeyValueConcurrentMap;

import java.lang.ref.WeakReference;

/**
 * 使用WeakHashMap(线程安全)存储对象的规范化对象，注意此对象需单例使用！<br>
 *
 * @author looly
 * @since 5.4.3
 */
public class WeakInterner<T> implements Interner<T> {

	private final WeakKeyValueConcurrentMap<T, WeakReference<T>> cache = new WeakKeyValueConcurrentMap<>();

	public T intern(T sample) {
		if (sample == null) {
			return null;
		}
		T val;
		do {
			val = this.cache.computeIfAbsent(sample, WeakReference::new).get();
		} while (val == null);
		return val;
	}
}

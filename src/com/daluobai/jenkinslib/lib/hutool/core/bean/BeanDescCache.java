package com.daluobai.jenkinslib.lib.hutool.core.bean;

import com.daluobai.jenkinslib.lib.hutool.core.bean.BeanDesc;
import com.daluobai.jenkinslib.lib.hutool.core.lang.func.Func0;
import com.daluobai.jenkinslib.lib.hutool.core.map.reference.WeakKeyValueConcurrentMap;

/**
 * Bean属性缓存<br>
 * 缓存用于防止多次反射造成的性能问题
 *
 * @author Looly
 */
public enum BeanDescCache {
	INSTANCE;

	private final WeakKeyValueConcurrentMap<Class<?>, com.daluobai.jenkinslib.lib.hutool.core.bean.BeanDesc> bdCache = new WeakKeyValueConcurrentMap<>();

	/**
	 * 获得属性名和{@link com.daluobai.jenkinslib.lib.hutool.core.bean.BeanDesc}Map映射
	 *
	 * @param beanClass Bean的类
	 * @param supplier  对象不存在时创建对象的函数
	 * @return 属性名和{@link com.daluobai.jenkinslib.lib.hutool.core.bean.BeanDesc}映射
	 * @since 5.4.2
	 */
	public com.daluobai.jenkinslib.lib.hutool.core.bean.BeanDesc getBeanDesc(Class<?> beanClass, Func0<BeanDesc> supplier) {
		return bdCache.computeIfAbsent(beanClass, (key)->supplier.callWithRuntimeException());
	}

	/**
	 * 清空全局的Bean属性缓存
	 *
	 * @since 5.7.21
	 */
	public void clear() {
		this.bdCache.clear();
	}
}

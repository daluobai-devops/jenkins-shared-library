package com.daluobai.jenkinslib.lib.hutool.core.clone;

import com.daluobai.jenkinslib.lib.hutool.core.clone.CloneRuntimeException;
import com.daluobai.jenkinslib.lib.hutool.core.clone.Cloneable;

/**
 * 克隆支持类，提供默认的克隆方法
 * @author Looly
 *
 * @param <T> 继承类的类型
 */
public class CloneSupport<T> implements Cloneable<T> {

	@SuppressWarnings("unchecked")
	@Override
	public T clone() {
		try {
			return (T) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new CloneRuntimeException(e);
		}
	}

}

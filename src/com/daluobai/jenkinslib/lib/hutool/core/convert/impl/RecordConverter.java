package com.daluobai.jenkinslib.lib.hutool.core.convert.impl;

import com.daluobai.jenkinslib.lib.hutool.core.bean.BeanUtil;
import com.daluobai.jenkinslib.lib.hutool.core.bean.RecordUtil;
import com.daluobai.jenkinslib.lib.hutool.core.bean.copier.ValueProvider;
import com.daluobai.jenkinslib.lib.hutool.core.bean.copier.provider.BeanValueProvider;
import com.daluobai.jenkinslib.lib.hutool.core.bean.copier.provider.MapValueProvider;
import com.daluobai.jenkinslib.lib.hutool.core.convert.ConvertException;
import com.daluobai.jenkinslib.lib.hutool.core.convert.Converter;

import java.util.Map;

/**
 * Record转换器
 *
 * @author looly
 */
public class RecordConverter implements Converter<Object> {

	private final Class<?> recordClass;

	/**
	 * 构造
	 * @param recordClass Record类
	 */
	public RecordConverter(Class<?> recordClass) {
		this.recordClass = recordClass;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object convert(Object value, Object defaultValue) throws IllegalArgumentException {
		ValueProvider<String> valueProvider = null;
		if (value instanceof ValueProvider) {
			valueProvider = (ValueProvider<String>) value;
		} else if (value instanceof Map) {
			valueProvider = new MapValueProvider((Map<String, ?>) value);
		} else if (BeanUtil.isReadableBean(value.getClass())) {
			valueProvider = new BeanValueProvider(value, false, false);
		}

		if (null != valueProvider) {
			return RecordUtil.newInstance(recordClass, valueProvider);
		}

		throw new ConvertException("Unsupported source type: [{}] to [{}]", value.getClass(), recordClass);
	}
}

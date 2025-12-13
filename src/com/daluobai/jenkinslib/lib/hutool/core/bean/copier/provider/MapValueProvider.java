package com.daluobai.jenkinslib.lib.hutool.core.bean.copier.provider;

import com.daluobai.jenkinslib.lib.hutool.core.bean.copier.ValueProvider;
import com.daluobai.jenkinslib.lib.hutool.core.convert.Convert;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Map值提供者
 *
 * @author Looly
 * @since 5.8.40
 */
@SuppressWarnings("rawtypes")
public class MapValueProvider implements ValueProvider<String> {

	private final Map map;

	/**
	 * 构造
	 *
	 * @param map map
	 */
	public MapValueProvider(final Map map) {
		this.map = map;
	}

	@Override
	public Object value(String key, Type valueType) {
		return Convert.convert(valueType, map.get(key));
	}

	@Override
	public boolean containsKey(String key) {
		return map.containsKey(key);
	}
}

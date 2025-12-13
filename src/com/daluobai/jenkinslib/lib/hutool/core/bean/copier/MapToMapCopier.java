package com.daluobai.jenkinslib.lib.hutool.core.bean.copier;

import com.daluobai.jenkinslib.lib.hutool.core.bean.copier.AbsCopier;
import com.daluobai.jenkinslib.lib.hutool.core.bean.copier.CopyOptions;
import com.daluobai.jenkinslib.lib.hutool.core.util.TypeUtil;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Map属性拷贝到Map中的拷贝器
 *
 * @since 5.8.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MapToMapCopier extends AbsCopier<Map, Map> {

	// 提前获取目标值真实类型
	private final Type[] targetTypeArguments;

	/**
	 * 构造
	 *
	 * @param source      来源Map
	 * @param target      目标Bean对象
	 * @param targetType  目标泛型类型
	 * @param copyOptions 拷贝选项
	 */
	public MapToMapCopier(Map source, Map target, Type targetType, CopyOptions copyOptions) {
		super(source, target, copyOptions);
		targetTypeArguments = TypeUtil.getTypeArguments(targetType);
	}

	@Override
	public Map copy() {
		this.source.forEach((sKey, sValue) -> {
			if (null == sKey) {
				return;
			}

			if(sKey instanceof String){
				sKey = copyOptions.editFieldName((String) sKey);
				// 对key做转换，转换后为null的跳过
				if (null == sKey) {
					return;
				}
			}

			// 忽略不需要拷贝的 key,
			if (false == copyOptions.testKeyFilter(sKey)) {
				return;
			}

			final Object targetValue = target.get(sKey);
			// 非覆盖模式下，如果目标值存在，则跳过
			if (false == copyOptions.override && null != targetValue) {
				return;
			}

			// 尝试转换源值
			if (null != targetTypeArguments) {
				//sValue = Convert.convertWithCheck(typeArguments[1], sValue, null, this.copyOptions.ignoreError);
				sValue = this.copyOptions.convertField(targetTypeArguments[1], sValue);
			}

			// 自定义值
			sValue = copyOptions.editFieldValue(sKey.toString(), sValue);

			// 忽略空值
			if (true == copyOptions.ignoreNullValue && sValue == null) {
				return;
			}

			// 目标赋值
			target.put(sKey, sValue);
		});
		return this.target;
	}
}

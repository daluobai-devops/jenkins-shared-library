package com.daluobai.jenkinslib.lib.hutool.core.bean.copier;

import com.daluobai.jenkinslib.lib.hutool.core.bean.BeanUtil;
import com.daluobai.jenkinslib.lib.hutool.core.bean.PropDesc;
import com.daluobai.jenkinslib.lib.hutool.core.bean.copier.AbsCopier;
import com.daluobai.jenkinslib.lib.hutool.core.bean.copier.CopyOptions;
import com.daluobai.jenkinslib.lib.hutool.core.lang.Assert;
import com.daluobai.jenkinslib.lib.hutool.core.util.TypeUtil;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Bean属性拷贝到Map中的拷贝器
 *
 * @since 5.8.0
 */
@SuppressWarnings("rawtypes")
public class BeanToMapCopier extends AbsCopier<Object, Map> {

	// 提前获取目标值真实类型
	private final Type[] targetTypeArguments;

	/**
	 * 构造
	 *
	 * @param source      来源Map
	 * @param target      目标Map对象
	 * @param targetType  目标泛型类型
	 * @param copyOptions 拷贝选项
	 */
	public BeanToMapCopier(Object source, Map target, Type targetType, CopyOptions copyOptions) {
		super(source, target, copyOptions);
		this.targetTypeArguments = TypeUtil.getTypeArguments(targetType);
	}

	@Override
	public Map copy() {
		Class<?> actualEditable = source.getClass();
		if (null != copyOptions.editable) {
			// 检查限制类是否为target的父类或接口
			Assert.isTrue(copyOptions.editable.isInstance(source),
					"Source class [{}] not assignable to Editable class [{}]", actualEditable.getName(), copyOptions.editable.getName());
			actualEditable = copyOptions.editable;
		}

		final Map<String, PropDesc> sourcePropDescMap = BeanUtil.getBeanDesc(actualEditable).getPropMap(copyOptions.ignoreCase);
		sourcePropDescMap.forEach((sFieldName, sDesc) -> {
			if (null == sFieldName || false == sDesc.isReadable(copyOptions.transientSupport)) {
				// 字段空或不可读，跳过
				return;
			}

			sFieldName = copyOptions.editFieldName(sFieldName);
			// 对key做转换，转换后为null的跳过
			if (null == sFieldName) {
				return;
			}

			// 忽略不需要拷贝的 key,
			if (false == copyOptions.testKeyFilter(sFieldName)) {
				return;
			}

			// 检查源对象属性是否过滤属性
			Object sValue = sDesc.getValue(this.source);
			if (false == copyOptions.testPropertyFilter(sDesc.getField(), sValue)) {
				return;
			}

			// 尝试转换源值
			if(null != targetTypeArguments && targetTypeArguments.length > 1){
				//sValue = Convert.convertWithCheck(typeArguments[1], sValue, null, this.copyOptions.ignoreError);
				sValue = this.copyOptions.convertField(targetTypeArguments[1], sValue);
			}

			// 自定义值
			sValue = copyOptions.editFieldValue(sFieldName, sValue);

			// 目标赋值
			if(null != sValue || false == copyOptions.ignoreNullValue){
				//noinspection unchecked
				target.put(sFieldName, sValue);
			}
		});
		return this.target;
	}
}

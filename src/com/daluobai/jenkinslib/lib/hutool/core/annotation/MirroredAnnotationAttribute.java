package com.daluobai.jenkinslib.lib.hutool.core.annotation;

import com.daluobai.jenkinslib.lib.hutool.core.annotation.AbstractWrappedAnnotationAttribute;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.MirrorLinkAnnotationPostProcessor;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType;
import com.daluobai.jenkinslib.lib.hutool.core.lang.Assert;

/**
 * 表示存在对应镜像属性的注解属性，当获取值时将根据{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType#MIRROR_FOR}的规则进行处理
 *
 * @author huangchengxing
 * @see MirrorLinkAnnotationPostProcessor
 * @see RelationType#MIRROR_FOR
 */
public class MirroredAnnotationAttribute extends AbstractWrappedAnnotationAttribute {

	public MirroredAnnotationAttribute(com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute origin, AnnotationAttribute linked) {
		super(origin, linked);
	}

	@Override
	public Object getValue() {
		final boolean originIsDefault = original.isValueEquivalentToDefaultValue();
		final boolean targetIsDefault = linked.isValueEquivalentToDefaultValue();
		final Object originValue = original.getValue();
		final Object targetValue = linked.getValue();

		// 都为默认值，或都为非默认值时，两方法的返回值必须相等
		if (originIsDefault == targetIsDefault) {
			Assert.equals(
				originValue, targetValue,
				"the values of attributes [{}] and [{}] that mirror each other are different: [{}] <==> [{}]",
				original.getAttribute(), linked.getAttribute(), originValue, targetValue
			);
			return originValue;
		}

		// 两者有一者不为默认值时，优先返回非默认值
		return originIsDefault ? targetValue : originValue;
	}

	/**
	 * 当{@link #original}与{@link #linked}都为默认值时返回{@code true}
	 *
	 * @return 是否
	 */
	@Override
	public boolean isValueEquivalentToDefaultValue() {
		return original.isValueEquivalentToDefaultValue() && linked.isValueEquivalentToDefaultValue();
	}
}

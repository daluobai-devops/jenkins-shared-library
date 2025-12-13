package com.daluobai.jenkinslib.lib.hutool.core.annotation;

import com.daluobai.jenkinslib.lib.hutool.core.annotation.AbstractWrappedAnnotationAttribute;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.AliasAnnotationPostProcessor;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.AliasLinkAnnotationPostProcessor;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType;

/**
 * 表示一个被指定了强制别名的注解属性。
 * 当调用{@link #getValue()}时，总是返回{@link #linked}的值
 *
 * @author huangchengxing
 * @see AliasAnnotationPostProcessor
 * @see AliasLinkAnnotationPostProcessor
 * @see com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType#ALIAS_FOR
 * @see RelationType#FORCE_ALIAS_FOR
 */
public class ForceAliasedAnnotationAttribute extends AbstractWrappedAnnotationAttribute {

	protected ForceAliasedAnnotationAttribute(com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute origin, com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute linked) {
		super(origin, linked);
	}

	/**
	 * 总是返回{@link #linked}的{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute#getValue()}的返回值
	 *
	 * @return {@link #linked}的{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute#getValue()}的返回值
	 */
	@Override
	public Object getValue() {
		return linked.getValue();
	}

	/**
	 * 总是返回{@link #linked}的{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute#isValueEquivalentToDefaultValue()}的返回值
	 *
	 * @return {@link #linked}的{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute#isValueEquivalentToDefaultValue()}的返回值
	 */
	@Override
	public boolean isValueEquivalentToDefaultValue() {
		return linked.isValueEquivalentToDefaultValue();
	}

	/**
	 * 总是返回{@link #linked}的{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute#getAttributeType()}的返回值
	 *
	 * @return {@link #linked}的{@link AnnotationAttribute#getAttributeType()}的返回值
	 */
	@Override
	public Class<?> getAttributeType() {
		return linked.getAttributeType();
	}

}

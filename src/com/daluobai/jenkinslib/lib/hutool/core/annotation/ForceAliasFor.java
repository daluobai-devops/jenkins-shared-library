package com.daluobai.jenkinslib.lib.hutool.core.annotation;

import com.daluobai.jenkinslib.lib.hutool.core.annotation.Alias;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.AliasFor;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.Link;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.MirrorFor;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType;

import java.lang.annotation.*;

/**
 * <p>{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.Link}的子注解。表示“原始属性”将强制作为“关联属性”的别名。效果等同于在“原始属性”上添加{@link Alias}注解，
 * 任何情况下，获取“关联属性”的值都将直接返回“原始属性”的值
 * <b>注意，该注解与{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.Link}、{@link AliasFor}或{@link MirrorFor}一起使用时，将只有被声明在最上面的注解会生效</b>
 *
 * @author huangchengxing
 * @see com.daluobai.jenkinslib.lib.hutool.core.annotation.Link
 * @see com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType#FORCE_ALIAS_FOR
 */
@com.daluobai.jenkinslib.lib.hutool.core.annotation.Link(type = com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType.FORCE_ALIAS_FOR)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface ForceAliasFor {

	/**
	 * 产生关联的注解类型，当不指定时，默认指注释的属性所在的类
	 *
	 * @return 关联注解类型
	 */
	@com.daluobai.jenkinslib.lib.hutool.core.annotation.Link(annotation = com.daluobai.jenkinslib.lib.hutool.core.annotation.Link.class, attribute = "annotation", type = com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType.FORCE_ALIAS_FOR)
	Class<? extends Annotation> annotation() default Annotation.class;

	/**
	 * {@link #annotation()}指定注解中关联的属性
	 *
	 * @return 关联的属性
	 */
	@com.daluobai.jenkinslib.lib.hutool.core.annotation.Link(annotation = Link.class, attribute = "attribute", type = RelationType.FORCE_ALIAS_FOR)
	String attribute() default "";
}

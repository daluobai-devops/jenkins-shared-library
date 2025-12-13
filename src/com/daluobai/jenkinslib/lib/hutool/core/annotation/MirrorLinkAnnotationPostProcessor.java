package com.daluobai.jenkinslib.lib.hutool.core.annotation;

import com.daluobai.jenkinslib.lib.hutool.core.annotation.AbstractLinkAnnotationPostProcessor;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationSynthesizer;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.Link;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.SynthesizedAnnotation;
import com.daluobai.jenkinslib.lib.hutool.core.lang.Assert;
import com.daluobai.jenkinslib.lib.hutool.core.text.CharSequenceUtil;
import com.daluobai.jenkinslib.lib.hutool.core.util.ObjectUtil;

/**
 * <p>用于处理注解对象中带有{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.Link}注解，且{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.Link#type()}为{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType#MIRROR_FOR}的属性。<br>
 * 当该处理器执行完毕后，原始合成注解中被{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.Link}注解的属性与{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.Link}注解指向的目标注解的属性，
 * 都将会被被包装并替换为{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute}。
 *
 * @author huangchengxing
 * @see com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType#MIRROR_FOR
 * @see com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute
 */
public class MirrorLinkAnnotationPostProcessor extends AbstractLinkAnnotationPostProcessor {

	private static final com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType[] PROCESSED_RELATION_TYPES = new com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType[]{ com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType.MIRROR_FOR };

	@Override
	public int order() {
		return Integer.MIN_VALUE + 1;
	}

	/**
	 * 该处理器只处理{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.Link#type()}类型为{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType#MIRROR_FOR}的注解属性
	 *
	 * @return 仅有{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType#MIRROR_FOR}数组
	 */
	@Override
	protected com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType[] processTypes() {
		return PROCESSED_RELATION_TYPES;
	}

	/**
	 * 将存在镜像关系的合成注解属性分别包装为{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute}对象，
	 * 并使用包装后{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute}替换在它们对应合成注解实例中的{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute}
	 *
	 * @param synthesizer        注解合成器
	 * @param annotation         {@code originalAttribute}上的{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.Link}注解对象
	 * @param originalAnnotation 当前正在处理的{@link com.daluobai.jenkinslib.lib.hutool.core.annotation.SynthesizedAnnotation}对象
	 * @param originalAttribute  {@code originalAnnotation}上的待处理的属性
	 * @param linkedAnnotation   {@link com.daluobai.jenkinslib.lib.hutool.core.annotation.Link}指向的关联注解对象
	 * @param linkedAttribute    {@link com.daluobai.jenkinslib.lib.hutool.core.annotation.Link}指向的{@code originalAnnotation}中的关联属性，该参数可能为空
	 */
	@Override
	protected void processLinkedAttribute(
            AnnotationSynthesizer synthesizer, com.daluobai.jenkinslib.lib.hutool.core.annotation.Link annotation,
            com.daluobai.jenkinslib.lib.hutool.core.annotation.SynthesizedAnnotation originalAnnotation, com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute originalAttribute,
            SynthesizedAnnotation linkedAnnotation, com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute linkedAttribute) {

		// 镜像属性必然成对出现，因此此处必定存在三种情况：
		// 1.两属性都不为镜像属性，此时继续进行后续处理；
		// 2.两属性都为镜像属性，并且指向对方，此时无需后续处理；
		// 3.两属性仅有任意一属性为镜像属性，此时镜像属性必然未指向当前原始属性，此时应该抛出异常；
		if (originalAttribute instanceof com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute
			|| linkedAttribute instanceof com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute) {
			checkMirrored(originalAttribute, linkedAttribute);
			return;
		}

		// 校验镜像关系
		checkMirrorRelation(annotation, originalAttribute, linkedAttribute);
		// 包装这一对镜像属性，并替换原注解中的对应属性
		final com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute mirroredOriginalAttribute = new com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute(originalAttribute, linkedAttribute);
		originalAnnotation.setAttribute(originalAttribute.getAttributeName(), mirroredOriginalAttribute);
		final com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute mirroredTargetAttribute = new com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute(linkedAttribute, originalAttribute);
		linkedAnnotation.setAttribute(annotation.attribute(), mirroredTargetAttribute);
	}

	/**
	 * 检查映射关系是否正确
	 */
	private void checkMirrored(com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute original, com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute mirror) {
		final boolean originalAttributeMirrored = original instanceof com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute;
		final boolean mirrorAttributeMirrored = mirror instanceof com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute;

		// 校验通过
		final boolean passed = originalAttributeMirrored && mirrorAttributeMirrored
			&& ObjectUtil.equals(((com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute)original).getLinked(), ((com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute)mirror).getOriginal());
		if (passed) {
			return;
		}

		// 校验失败，拼装异常信息用于抛出异常
		String errorMsg;
		// 原始字段已经跟其他字段形成镜像
		if (originalAttributeMirrored && !mirrorAttributeMirrored) {
			errorMsg = CharSequenceUtil.format(
				"attribute [{}] cannot mirror for [{}], because it's already mirrored for [{}]",
				original.getAttribute(), mirror.getAttribute(), ((com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute)original).getLinked()
			);
		}
		// 镜像字段已经跟其他字段形成镜像
		else if (!originalAttributeMirrored && mirrorAttributeMirrored) {
			errorMsg = CharSequenceUtil.format(
				"attribute [{}] cannot mirror for [{}], because it's already mirrored for [{}]",
				mirror.getAttribute(), original.getAttribute(), ((com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute)mirror).getLinked()
			);
		}
		// 两者都形成了镜像，但是都未指向对方，理论上不会存在该情况
		else {
			errorMsg = CharSequenceUtil.format(
				"attribute [{}] cannot mirror for [{}], because [{}] already mirrored for [{}] and  [{}] already mirrored for [{}]",
				mirror.getAttribute(), original.getAttribute(),
				mirror.getAttribute(), ((com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute)mirror).getLinked(),
				original.getAttribute(), ((MirroredAnnotationAttribute)original).getLinked()
			);
		}

		throw new IllegalArgumentException(errorMsg);
	}

	/**
	 * 基本校验
	 */
	private void checkMirrorRelation(com.daluobai.jenkinslib.lib.hutool.core.annotation.Link annotation, com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute original, AnnotationAttribute mirror) {
		// 镜像属性必须存在
		checkLinkedAttributeNotNull(original, mirror, annotation);
		// 镜像属性返回值必须一致
		checkAttributeType(original, mirror);
		// 镜像属性上必须存在对应的注解
		final Link mirrorAttributeAnnotation = getLinkAnnotation(mirror, com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType.MIRROR_FOR);
		Assert.isTrue(
			ObjectUtil.isNotNull(mirrorAttributeAnnotation) && com.daluobai.jenkinslib.lib.hutool.core.annotation.RelationType.MIRROR_FOR.equals(mirrorAttributeAnnotation.type()),
			"mirror attribute [{}] of original attribute [{}] must marked by @Link, and also @LinkType.type() must is [{}]",
			mirror.getAttribute(), original.getAttribute(), RelationType.MIRROR_FOR
		);
		checkLinkedSelf(original, mirror);
	}

}

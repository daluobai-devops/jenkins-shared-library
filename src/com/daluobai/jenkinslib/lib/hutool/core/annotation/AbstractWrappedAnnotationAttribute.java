package com.daluobai.jenkinslib.lib.hutool.core.annotation;

import com.daluobai.jenkinslib.lib.hutool.core.annotation.AliasedAnnotationAttribute;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.ForceAliasedAnnotationAttribute;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.MirroredAnnotationAttribute;
import com.daluobai.jenkinslib.lib.hutool.core.annotation.WrappedAnnotationAttribute;
import com.daluobai.jenkinslib.lib.hutool.core.lang.Assert;
import com.daluobai.jenkinslib.lib.hutool.core.util.ObjectUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * {@link com.daluobai.jenkinslib.lib.hutool.core.annotation.WrappedAnnotationAttribute}的基本实现
 *
 * @author huangchengxing
 * @see ForceAliasedAnnotationAttribute
 * @see AliasedAnnotationAttribute
 * @see MirroredAnnotationAttribute
 */
public abstract class AbstractWrappedAnnotationAttribute implements com.daluobai.jenkinslib.lib.hutool.core.annotation.WrappedAnnotationAttribute {

	protected final com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute original;
	protected final com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute linked;

	protected AbstractWrappedAnnotationAttribute(com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute original, com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute linked) {
		Assert.notNull(original, "target must not null");
		Assert.notNull(linked, "linked must not null");
		this.original = original;
		this.linked = linked;
	}

	@Override
	public com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute getOriginal() {
		return original;
	}

	@Override
	public com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute getLinked() {
		return linked;
	}

	@Override
	public com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute getNonWrappedOriginal() {
		com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute curr = null;
		com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute next = original;
		while (next != null) {
			curr = next;
			next = next.isWrapped() ? ((com.daluobai.jenkinslib.lib.hutool.core.annotation.WrappedAnnotationAttribute)curr).getOriginal() : null;
		}
		return curr;
	}

	@Override
	public Collection<com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute> getAllLinkedNonWrappedAttributes() {
		List<com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute> leafAttributes = new ArrayList<>();
		collectLeafAttribute(this, leafAttributes);
		return leafAttributes;
	}

	private void collectLeafAttribute(com.daluobai.jenkinslib.lib.hutool.core.annotation.AnnotationAttribute curr, List<AnnotationAttribute> leafAttributes) {
		if (ObjectUtil.isNull(curr)) {
			return;
		}
		if (!curr.isWrapped()) {
			leafAttributes.add(curr);
			return;
		}
		com.daluobai.jenkinslib.lib.hutool.core.annotation.WrappedAnnotationAttribute wrappedAttribute = (WrappedAnnotationAttribute)curr;
		collectLeafAttribute(wrappedAttribute.getOriginal(), leafAttributes);
		collectLeafAttribute(wrappedAttribute.getLinked(), leafAttributes);
	}

}

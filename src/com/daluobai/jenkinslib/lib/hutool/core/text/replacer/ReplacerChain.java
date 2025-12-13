package com.daluobai.jenkinslib.lib.hutool.core.text.replacer;

import com.daluobai.jenkinslib.lib.hutool.core.lang.Chain;
import com.daluobai.jenkinslib.lib.hutool.core.text.StrBuilder;
import com.daluobai.jenkinslib.lib.hutool.core.text.replacer.StrReplacer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 字符串替换链，用于组合多个字符串替换逻辑
 *
 * @author looly
 * @since 4.1.5
 */
public class ReplacerChain extends com.daluobai.jenkinslib.lib.hutool.core.text.replacer.StrReplacer implements Chain<com.daluobai.jenkinslib.lib.hutool.core.text.replacer.StrReplacer, ReplacerChain> {
	private static final long serialVersionUID = 1L;

	private final List<com.daluobai.jenkinslib.lib.hutool.core.text.replacer.StrReplacer> replacers = new LinkedList<>();

	/**
	 * 构造
	 *
	 * @param strReplacers 字符串替换器
	 */
	public ReplacerChain(com.daluobai.jenkinslib.lib.hutool.core.text.replacer.StrReplacer... strReplacers) {
		for (com.daluobai.jenkinslib.lib.hutool.core.text.replacer.StrReplacer strReplacer : strReplacers) {
			addChain(strReplacer);
		}
	}

	@SuppressWarnings("NullableProblems")
	@Override
	public Iterator<com.daluobai.jenkinslib.lib.hutool.core.text.replacer.StrReplacer> iterator() {
		return replacers.iterator();
	}

	@Override
	public ReplacerChain addChain(com.daluobai.jenkinslib.lib.hutool.core.text.replacer.StrReplacer element) {
		replacers.add(element);
		return this;
	}

	@Override
	protected int replace(CharSequence str, int pos, StrBuilder out) {
		int consumed = 0;
		for (StrReplacer strReplacer : replacers) {
			consumed = strReplacer.replace(str, pos, out);
			if (0 != consumed) {
				return consumed;
			}
		}
		return consumed;
	}

}

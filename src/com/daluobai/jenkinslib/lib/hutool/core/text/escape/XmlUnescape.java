package com.daluobai.jenkinslib.lib.hutool.core.text.escape;

import com.daluobai.jenkinslib.lib.hutool.core.text.escape.InternalEscapeUtil;
import com.daluobai.jenkinslib.lib.hutool.core.text.escape.NumericEntityUnescaper;
import com.daluobai.jenkinslib.lib.hutool.core.text.escape.XmlEscape;
import com.daluobai.jenkinslib.lib.hutool.core.text.replacer.LookupReplacer;
import com.daluobai.jenkinslib.lib.hutool.core.text.replacer.ReplacerChain;

/**
 * XML的UNESCAPE
 *
 * @author looly
 * @since 5.7.2
 */
public class XmlUnescape extends ReplacerChain {
	private static final long serialVersionUID = 1L;

	protected static final String[][] BASIC_UNESCAPE  = InternalEscapeUtil.invert(XmlEscape.BASIC_ESCAPE);

	/**
	 * 构造
	 */
	public XmlUnescape() {
		addChain(new LookupReplacer(BASIC_UNESCAPE));
		addChain(new NumericEntityUnescaper());
	}
}

package cc.alcina.framework.entity;

import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.parsers.DOMParser;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;

public class DomParserUtils {
	public static LooseContext.Key CONTEXT_DO_NOT_BALANCE_TAGS = LooseContext
			.key(DomParserUtils.class, "CONTEXT_DO_NOT_BALANCE_TAGS");

	public static DOMParser createDOMParser(boolean lowercaseTags) {
		DOMParser parser = new DOMParser();
		boolean balanceTags = !CONTEXT_DO_NOT_BALANCE_TAGS.is();
		try {
			parser.setFeature(
					"http://cyberneko.org/html/features/scanner/fix-mswindows-refs",
					true);
			parser.setFeature(
					"http://cyberneko.org/html/features/scanner/ignore-specified-charset",
					true);
			if (lowercaseTags) {
				parser.setProperty(
						"http://cyberneko.org/html/properties/names/elems",
						"lower");
			}
			if (!balanceTags) {
				parser.setFeature(
						"http://cyberneko.org/html/features/balance-tags",
						false);
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		return parser;
	}
}

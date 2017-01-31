package cc.alcina.framework.entity.parser.token;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Text;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.parser.token.ParserContext.TextRange;

public class TokenParserHelper<T extends ParserToken, C extends ParserContext, S extends AbstractParserSlice<T>> {
	private static final int MAX_CHARS_PATTERN_HELPER = 2000;

	protected T token;

	public TokenParserHelper(T token) {
		this.token = token;
	}

	public S currentRangeAsSliceAndIncrementOffset(C context, int trimFromEnd,
			T token) {
		TextRange currentTextRange = context.getCurrentTextRange();
		Text last = (Text) CommonUtils.last(currentTextRange.texts);
		Text text = (Text) currentTextRange.texts.get(0);
		S slice = (S) token.createSlice(context,
				new XmlUtils.DOMLocation(text, 0, 0),
				new XmlUtils.DOMLocation(last,
						Math.max(1,
								last.getTextContent().length() - trimFromEnd),
						0),
				0);
		context.startOffset += currentTextRange.textContent.length();
		return slice;
	}

	public S extractSubstringAndMatch(C context) {
		String visibleSubstring = context
				.getVisibleSubstring(token.matchesEmphasisTypes(), token);
		if (visibleSubstring == null) {
			return null;
		}
		return (S) token.match(context, visibleSubstring);
	}

	public S match0(C context, String visibleSubstring) {
		Pattern pattern = token.getPattern(context);
		if (pattern == null) {
			return null;
		}
		if (visibleSubstring.length() < MAX_CHARS_PATTERN_HELPER
				&& visibleSubstring.contains(". . ")) {
			// help the patterns
			visibleSubstring = visibleSubstring.replace(". . ", "xxx ");
		}
		Matcher m = pattern.matcher(visibleSubstring);
		if (m.find()) {
			return sliceFromMatcher(context, visibleSubstring, m);
		} else {
			return null;
		}
	}

	public S sliceFromMatcher(C context, String visibleSubstring, Matcher m) {
		if (token.skipMatchingWhitespace(context, visibleSubstring,
				m.start())) {
			return null;
		}
		XmlUtils.DOMLocation start = XmlUtils.locationOfTextIndex(
				context.allTexts, m.start() + context.startOffset);
		XmlUtils.DOMLocation end = XmlUtils.locationOfTextIndex(
				context.allTexts, m.end() + context.startOffset);
		context.startOffset += m.end();
		return (S) token.createSlice(context, start, end, m.start());
	}

	public S matchWithFollowCheck(C context) {
		if (!token.canFollow(context)) {
			return null;
		}
		return extractSubstringAndMatch(context);
	}
}

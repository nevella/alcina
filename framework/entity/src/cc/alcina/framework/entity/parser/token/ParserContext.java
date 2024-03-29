package cc.alcina.framework.entity.parser.token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.html.HTMLAnchorElement;
import org.w3c.dom.ranges.DocumentRange;
import org.w3c.dom.ranges.Range;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.StringPair;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.XmlUtils;

public class ParserContext<T extends ParserToken, S extends AbstractParserSlice<T>> {
	public static final String LONG_BLANK_STRING = "        ";

	// will never match. Note symmetry
	public static final String LONG_BLANK_STRING_REPLACE = " **^^** ";

	public static String replaceHyphens(String content) {
		content = content.replace("-", "\u2011");
		return content;
	}

	public String content = "";

	public boolean reverseItalicBehaviour = false;

	public List<Text> nonEmphasisTexts = new ArrayList<Text>();

	public List<Text> allTexts = new ArrayList<Text>();

	public List<Text> emphasisedTexts = new ArrayList<Text>();

	public List<Text> superscriptTexts = new ArrayList<Text>();

	public List<Text> boldTexts = new ArrayList<Text>();

	public SortedMap<IntPair, Element> aTagContents = new TreeMap<IntPair, Element>();

	List<IntPair> boldRanges = new ArrayList<IntPair>();

	Map<Text, String> normalisedTextContents = new HashMap<Text, String>();

	public int startOffset;

	public List<S> matched = new ArrayList<S>();

	public Multimap<Class, List<T>> matchedByType = new Multimap<Class, List<T>>();

	public CountingMap<T> tokenCounts = new CountingMap<T>();

	List<TextRange> textRanges;

	public StringBuilder sourceExplanation = new StringBuilder();

	private String currentBlocklikeContent;

	public S provisionalBestMatch;

	public T lastBlockMatch;

	public boolean exit;

	private Element commonContainer;

	public void addMatchedToken(S slice) {
		if (slice != null) {
			matched.add(slice);
			T t = slice.getToken();
			tokenCounts.add(t);
			matchedByType.add(t.getCategory(), t);
		}
	}

	public void addText(Text t, boolean emphasised, boolean bold,
			boolean superscript) {
		String textContent = t.getTextContent();
		if (superscript) {
			superscriptTexts.add(t);
		} else if (emphasised) {
			emphasisedTexts.add(t);
		} else {
			nonEmphasisTexts.add(t);
		}
		if (bold) {
			boldTexts.add(t);
		}
		allTexts.add(t);
		String qnText = TokenParserUtils.quickNormalisePunctuation(textContent);
		normalisedTextContents.put(t, qnText);
		Element a = XmlUtils.getAncestorWithTagName(t, "A");
		if (a != null) {
			IntPair pair = new IntPair(content.length(),
					content.length() + qnText.length());
			if (!aTagContents.isEmpty()) {
				IntPair lastPair = aTagContents.lastKey();
				Element lastA = aTagContents.get(lastPair);
				if (lastA == a) {
					aTagContents.remove(lastPair);
					pair = pair.union(lastPair);
				}
			}
			aTagContents.put(pair, a);
		}
		content += qnText;
	}

	public boolean allBold() {
		ArrayList<Text> list = new ArrayList<Text>(allTexts);
		list.removeAll(boldTexts);
		for (Text text : list) {
			if (SEUtilities.normalizeWhitespaceAndTrim(text.getTextContent())
					.length() > 0) {
				return false;
			}
		}
		return true;
	}

	public boolean allEmphasised() {
		ArrayList<Text> list = new ArrayList<Text>(allTexts);
		list.removeAll(emphasisedTexts);
		for (Text text : list) {
			if (SEUtilities.normalizeWhitespaceAndTrim(text.getTextContent())
					.length() > 0) {
				return false;
			}
		}
		return true;
	}

	public ParserContextChecker checker() {
		return new ParserContextChecker();
	}

	protected boolean checkLongBlankString() {
		return true;
	}

	public void clearNodes() {
		allTexts.clear();
		nonEmphasisTexts.clear();
		boldRanges.clear();
		boldTexts.clear();
		emphasisedTexts.clear();
		superscriptTexts.clear();
		normalisedTextContents.clear();
		textRanges.clear();
		startOffset = 0;
		content = "";
		currentBlocklikeContent = "";
		commonContainer = null;
	}

	public boolean containsAtLeastOneOfTokens(Set<T> tokens) {
		for (S s : matched) {
			if (tokens.contains(s.getToken())) {
				return true;
			}
		}
		return false;
	}

	public boolean containsBold(int start, int end) {
		return IntPair.containedInRanges(boldRanges,
				new IntPair(startOffset + start, startOffset + end));
	}

	public boolean containsOnly(Set<T> tokens) {
		for (S s : matched) {
			if (!tokens.contains(s.getToken())) {
				return false;
			}
		}
		return true;
	}

	public boolean containsToken(T token) {
		for (S s : matched) {
			if (s.getToken() == token) {
				return true;
			}
		}
		return false;
	}

	public boolean containsTokenBefore(T token, S slice) {
		for (S s : matched) {
			if (s == slice) {
				return false;
			}
			if (s.getToken() == token) {
				return true;
			}
		}
		return false;
	}

	public int endOffsetOfSlice(AbstractParserSlice slice) {
		return startOffsetOfSlice(slice) + slice.contents().length();
	}

	public List<HTMLAnchorElement> getAnchors() {
		List<HTMLAnchorElement> anchors = new ArrayList<HTMLAnchorElement>();
		if (allTexts.size() == 0 || getCurrentTextRange() == null) {
			return anchors;
		}
		for (Text t : getCurrentTextRange().texts) {
			if (t.getParentNode() instanceof HTMLAnchorElement) {
				anchors.add((HTMLAnchorElement) t.getParentNode());
			}
		}
		return anchors;
	}

	public Element getCommonContainer() {
		if (commonContainer == null && allTexts.size() > 0) {
			int size = allTexts.size();
			Text t1 = CommonUtils.first(allTexts);
			if (SEUtilities.normalizeWhitespaceAndTrim(t1.getTextContent())
					.isEmpty() && size > 1) {
				t1 = allTexts.get(1);
			}
			Text t2 = CommonUtils.last(allTexts);
			if (SEUtilities.normalizeWhitespaceAndTrim(t2.getTextContent())
					.isEmpty() && size > 0
					&& allTexts.indexOf(t1) <= allTexts.size() - 2) {
				t2 = allTexts.get(allTexts.size() - 2);
			}
			Range r1 = ((DocumentRange) t1.getOwnerDocument()).createRange();
			r1.setStartBefore(t1);
			r1.setEndAfter(t2);
			Node node = r1.getCommonAncestorContainer();
			r1.detach();
			if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
				commonContainer = (Element) node;
			} else {
				commonContainer = null;
			}
		}
		return commonContainer;
	}

	public Optional<DomNode> getContainingNode(int absoluteOffset) {
		int offset = 0;
		for (Text text : allTexts) {
			int length = text.getLength();
			IntPair textRange = new IntPair(offset, offset + length);
			if (textRange.containsExEnd(absoluteOffset)) {
				return Optional.of(DomNode.from(text));
			}
			offset += length;
		}
		return Optional.empty();
	}

	public Optional<DomNode>
			getContainingNode(IntPair containingRangeRelativeToStartOffset) {
		containingRangeRelativeToStartOffset = containingRangeRelativeToStartOffset
				.shiftRight(startOffset);
		int offset = 0;
		for (Text text : allTexts) {
			int length = text.getLength();
			IntPair textRange = new IntPair(offset, offset + length);
			if (containingRangeRelativeToStartOffset.contains(textRange)) {
				return Optional.of(DomNode.from(text));
			}
			offset += length;
		}
		return Optional.empty();
	}

	public Optional<DomNode> getContainingNode(Matcher matcher) {
		return getContainingNode(new IntPair(matcher.start(), matcher.end()));
	}

	public String getCurrentBlocklikeContent() {
		if (currentBlocklikeContent == null) {
			if (!allTexts.isEmpty()) {
				currentBlocklikeContent = XmlUtils
						.getSurroundingBlockTuple(allTexts.get(0)).getContent();
			}
		}
		return currentBlocklikeContent == null ? "" : currentBlocklikeContent;
	}

	public TextRange getCurrentTextRange() {
		for (Iterator<TextRange> itr = textRanges.iterator(); itr.hasNext();) {
			TextRange tr = itr.next();
			int length = tr.textContent.length();
			if (tr.offset + length > startOffset) {
				return tr;
			}
		}
		return null;
	}

	public int getCurrentTextRangeEnd() {
		TextRange currentTextRange = getCurrentTextRange();
		return currentTextRange == null ? -1
				: currentTextRange.offset
						+ currentTextRange.textContent.length();
	}

	public int getCurrentTextRangeStart() {
		TextRange currentTextRange = getCurrentTextRange();
		return currentTextRange == null ? -1 : currentTextRange.offset;
	}

	public T getLastNonIgnorableToken() {
		Iterator<T> itr = reversedNonIgnoreableIterator();
		return itr.hasNext() ? itr.next() : null;
	}

	public int getTextIndex(Node node) {
		Text stop = XmlUtils.getFirstTextChildOrSelf(node);
		int index = 0;
		for (int i = 0; i < allTexts.size(); i++) {
			Text t = allTexts.get(i);
			if (t == stop) {
				break;
			}
			index += t.getLength();
		}
		return index;
	}

	/*
	 * Some length checks here to deal with rare parsing issues for large text
	 * blocks
	 */
	public String getVisibleSubstring(MatchesEmphasisTypes type, T token) {
		String result = null;
		if (type == MatchesEmphasisTypes.BOTH) {
			if (startOffset > content.length()) {
				return "";
			}
			result = content.substring(startOffset);
		} else {
			TextRange tr = getCurrentTextRange();
			if (tr == null) {
				return null;
			}
			boolean inVisible = type == MatchesEmphasisTypes.SINGLE
					|| (type == MatchesEmphasisTypes.EMPHASIS
							^ (!tr.emphasised));
			if (!inVisible) {
				return null;
			}
			int offset = startOffset - tr.offset;
			if (offset > tr.textContent.length()) {
				return "";
			}
			result = content.substring(startOffset,
					tr.offset + tr.textContent.length());
		}
		return result;
	}

	public boolean had(T token) {
		return tokenCounts.containsKey(token);
	}

	public boolean isStrictCategoryChecking() {
		return false;
	}

	public S lastMatched() {
		return CommonUtils.last(matched);
	}

	public boolean lastTextWasEmphasis() {
		return !CommonUtils.isNullOrEmpty(textRanges)
				&& CommonUtils.last(textRanges).emphasised;
	}

	public T lastToken() {
		S last = CommonUtils.last(matched);
		return last == null ? null : last.getToken();
	}

	public <V extends T> V lastTokenOfType(Class<? extends T> clazz) {
		return (V) CommonUtils.last(matchedByType.getAndEnsure(clazz));
	}

	public boolean matches(T[] tokens) {
		int j = 0;
		int matchedTokens = 0;
		for (int i = 0; i < matched.size(); i++) {
			T match = matched.get(i).getToken();
			if (match.isIgnoreable(this)) {
				continue;
			}
			if (j == tokens.length) {
				return false;
			}
			if (match != tokens[j++]) {
				return false;
			}
			matchedTokens++;
		}
		return tokens.length == matchedTokens;
	}

	public boolean moveToNextRange() {
		TextRange tr = getCurrentTextRange();
		if (tr != null && tr != CommonUtils.last(textRanges)) {
			startOffset = textRanges.get(textRanges.indexOf(tr) + 1).offset;
			return true;
		}
		return false;
	}

	public void normaliseContent() {
		content = TokenParserUtils.quickNormalisePunctuation(content);
		if (checkLongBlankString()) {
			int idx = content.indexOf(ParserContext.LONG_BLANK_STRING);
			// slightly hacky but works;
			// (long spaces plays havoc with some regexes)
			if (idx != -1 && content.length() > 200) {
				StringBuilder sb = new StringBuilder();
				char[] val = content.toCharArray();
				int wsCount = 0;
				// because of WhitespaceNormalisationText windows, need to
				// replace in reverse
				// content =
				// content.replace(ParserContext.LONG_BLANK_STRING,
				// ParserContext.LONG_BLANK_STRING_REPLACE);
				/*
				 * reversed replace of same-length (8char) strings
				 */
				for (idx = content.length() - 1; idx >= 0; idx--) {
					char c = val[idx];
					if (c == ' ') {
						wsCount++;
					} else {
						wsCount = 0;
					}
					if (wsCount == 8) {
						for (int idx2 = 0; idx2 < 8; idx2++) {
							val[idx + idx2] = ParserContext.LONG_BLANK_STRING_REPLACE
									.charAt(idx2);
						}
						wsCount = 0;
					}
				}
				content = new String(val);
			}
		}
	}

	public void removeToken(T token) {
		for (Iterator<S> itr = matched.iterator(); itr.hasNext();) {
			S next = itr.next();
			if (next.getToken() == token) {
				itr.remove();
			}
		}
	}

	public void resetSequence() {
		sourceExplanation.setLength(0);
		currentBlocklikeContent = null;
	}

	public Iterator<T> reversedNonIgnoreableIterator() {
		return new ReversedNonIgnoreableIterator(this);
	}

	public void rewindToStartOfFirstSlice() {
		startOffset = startOffsetOfSlice(matched.get(0));
		matched.clear();
		matchedByType.clear();
		tokenCounts.clear();
		resetSequence();
	}

	public S sliceForToken(T token) {
		List<S> slices = slicesForToken(token);
		return slices.isEmpty() ? null : slices.get(0);
	}

	public List<S> slicesForToken(T token) {
		List<S> result = new ArrayList<S>();
		for (S s : matched) {
			if (s.getToken() == token) {
				result.add(s);
			}
		}
		return result;
	}

	public int startOffsetOfSlice(AbstractParserSlice slice) {
		Text firstText = slice.getFirstText();
		int offset = 0;
		textRangeLoop: for (TextRange tr : textRanges) {
			for (Text t : tr.texts) {
				if (t != firstText) {
					offset += t.getLength();
				} else {
					break textRangeLoop;
				}
			}
		}
		offset += slice.start.characterOffset;
		return offset;
	}

	public void textsToRanges() {
		textRanges = new ArrayList<TextRange>();
		TextRange textRange = null;
		boolean emphasis = false;
		boolean superscript = false;
		int offset = 0;
		for (Text t : allTexts) {
			String ntc = normalisedTextContents.get(t);
			boolean newEmphasis = emphasisedTexts.contains(t);
			boolean newSuperscript = superscriptTexts.contains(t);
			if (ntc.trim().length() != 0 && textRange != null) {
				if (newEmphasis != emphasis) {
					textRange = null;
				}
			}
			if (textRange != null) {
				if (newSuperscript != superscript) {
					textRange = null;
				}
			}
			if (textRange == null) {
				emphasis = newEmphasis;
				superscript = newSuperscript;
				textRange = new TextRange();
				textRange.emphasised = emphasis;
				textRange.superscript = superscript;
				textRange.offset = offset;
				textRanges.add(textRange);
			}
			textRange.textContent += ntc;
			textRange.texts.add(t);
			if (boldTexts.contains(t)) {
				boldRanges.add(new IntPair(offset, offset + ntc.length()));
			}
			offset += ntc.length();
		}
		startOffset = 0;
	}

	@Override
	public String toString() {
		return String.format("Matched: %s\n" + "Content: %s\n" + "", matched,
				content);
	}

	public class ParserContextChecker {
		private String[] tags;

		private T[] tokens;

		private List<StringPair> attributes = new ArrayList<StringPair>();

		private Class<? extends T> tokenCategory;

		public ParserContextChecker attribute(String key, String value) {
			attributes.add(new StringPair(key, value));
			return this;
		}

		public boolean check() {
			T lastToken = getLastNonIgnorableToken();
			T lastTokenWithCategory = lastTokenWithCategory();
			boolean matchedCategory = false;
			if (tokenCategory != null && lastTokenWithCategory != null) {
				if (tokenCategory.isAssignableFrom(
						lastTokenWithCategory.getCategory())) {
					matchedCategory = true;
				} else {
					if (isStrictCategoryChecking()) {
						return false;
					}
				}
			}
			if (tokenCategory != null && tokens == null && !matchedCategory) {
				return false;
			}
			if (tokens != null
					&& (!matchedCategory || isStrictCategoryChecking())) {
				boolean matched = false;
				for (T token : tokens) {
					if (lastToken == token) {
						matched = true;
						break;
					}
				}
				if (!matched) {
					return false;
				}
			}
			if (tags != null) {
				Element container = getCommonContainer();
				if (container == null) {
					return false;
				}
				boolean matched = false;
				for (String tag : tags) {
					if (XmlUtils.hasAncestorWithTagName(container, tag)) {
						matched = true;
						break;
					}
				}
				if (!matched) {
					return false;
				}
			}
			if (attributes.isEmpty()) {
				Element container = getCommonContainer();
				if (container == null) {
					return false;
				}
				for (StringPair pair : attributes) {
					if (!container.getAttribute(pair.s1).equals(pair.s2)) {
						return false;
					}
				}
			}
			return true;
		}

		private T lastTokenWithCategory() {
			for (int idx = matched.size() - 1; idx >= 0; idx--) {
				S slice = matched.get(idx);
				T token = slice.getToken();
				if (token.getCategory() != null) {
					return token;
				}
			}
			return null;
		}

		public ParserContextChecker tags(String... tags) {
			this.tags = tags;
			return this;
		}

		public ParserContextChecker tokenCategory(Class<? extends T> clazz) {
			tokenCategory = clazz;
			return this;
		}

		public ParserContextChecker tokens(T... tokens) {
			this.tokens = tokens;
			return this;
		}
	}

	private class ReversedNonIgnoreableIterator implements Iterator<T> {
		int cursor = matched.size() - 1;

		T value = null;

		private ParserContext context;

		private boolean finished;

		public ReversedNonIgnoreableIterator(ParserContext context) {
			this.context = context;
			peek();
		}

		@Override
		public boolean hasNext() {
			return !finished;
		}

		@Override
		public T next() {
			if (finished) {
				throw new NoSuchElementException();
			}
			T value = this.value;
			peek();
			return value;
		}

		private void peek() {
			while (cursor >= 0) {
				value = matched.get(cursor--).getToken();
				if (value.isIgnoreable(context)) {
					continue;
				} else {
					return;
				}
			}
			finished = true;
		}
	}

	// a group of texts of the same style
	public class TextRange {
		public boolean superscript;

		public boolean emphasised;

		public List<Text> texts = new ArrayList<Text>();

		public String textContent = "";

		public int offset;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ParserContext.TextRange) {
				return texts.equals(((TextRange) obj).texts);
			}
			return false;
		}

		public void extendMatch(AbstractParserSlice slice, String string) {
			int offset = string.length();
			for (Text text : texts) {
				int length = text.getLength();
				if (offset <= length) {
					slice.extend(text, offset);
					return;
				}
				offset -= text.getLength();
			}
			throw new IllegalArgumentException();
		}

		@Override
		public int hashCode() {
			return texts.hashCode();
		}

		@Override
		public String toString() {
			return String.format("(%s) (%s%s) %s", offset,
					(emphasised ? "emph" : "not-emph"),
					(superscript ? ":super" : ""), textContent);
		}
	}
}

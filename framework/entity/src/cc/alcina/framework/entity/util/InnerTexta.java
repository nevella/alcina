package cc.alcina.framework.entity.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.XmlUtils;

public class InnerTexta {
	private static final char[] TAB_MARKER_ARR = new char[] { 9000, 9001, 9009,
			10455, 39877 };

	private static final char[] NL_MARKER_ARR = new char[] { 9000, 9001, 9009,
			10455, 39876 };

	public static final int SHOW_LIST_ITEMS = 1;

	public static final int SHOW_NEWLINES = 2;

	public static final int PRESERVE_WHITESPACE = 4;

	public static final String NL_MARKER = new String(NL_MARKER_ARR);

	private static final String tabMarker = new String(TAB_MARKER_ARR);

	private static final String HTML_LISTS = ",UL,OL,";

	public static final int SHOW_NEWLINES_AS_DOUBLE = 8;

	public static final int PRESERVE_TRAILING_NEWLINES = 16;
	
	public static final int INCLUDE_INVISIBLE_ELEMENTS =32;

	public static boolean isListElement(Element e) {
		return HTML_LISTS.contains("," + e.getTagName() + ",");
	}

	private List<Integer> newLineInsertLocations;

	private Predicate<Element> blockElementMatcher = XmlUtils::isBlockHTMLElement;

	public Predicate<Element> getBlockElementMatcher() {
		return this.blockElementMatcher;
	}

	public List<Integer> getNewLineInsertLocations() {
		return this.newLineInsertLocations;
	}

	public String innerText(Node n) {
		return innerText(n, 0);
	}

	public String innerText(Node n, int flags) {
		newLineInsertLocations = new ArrayList<Integer>();
		Document doc = n.getNodeType() == Node.DOCUMENT_NODE ? (Document) n
				: n.getOwnerDocument();
		TreeWalker walker = ((DocumentTraversal) doc).createTreeWalker(n,
				NodeFilter.SHOW_TEXT | NodeFilter.SHOW_ELEMENT, null, true);
		StringBuilder result = new StringBuilder();
		Node n2 = null;
		Stack<Element> listParentStack = new Stack<Element>();
		Map<Element, Integer> listIndicies = new HashMap<Element, Integer>();
		boolean showNls = (flags & SHOW_NEWLINES) != 0;
		boolean showLis = (flags & SHOW_LIST_ITEMS) != 0;
		boolean showInvisible = (flags & INCLUDE_INVISIBLE_ELEMENTS) != 0;
		boolean finished = false;
		while (!finished && (n2 = walker.nextNode()) != null) {
			if (n2.getNodeType() == Node.ELEMENT_NODE) {
				Element elt = (Element) n2;
				String tag = elt.getTagName().toLowerCase();
				if (blockElementMatcher.test(elt)) {
					markNewline(result, showNls);
				}
				if (isListElement(elt)) {
					popForNonParent(elt, listParentStack);
					listParentStack.push(elt);
					listIndicies.put(elt, 1);
				}
				if (tag.equals("li")) {
					popForNonParent(elt, listParentStack);
					if (!listParentStack.isEmpty() && showLis) {
						Element listElt = listParentStack.peek();
						for (int i = 1; i < listParentStack.size(); i++) {
							result.append(tabMarker);
						}
						Integer listIndex = listIndicies.get(listElt);
						try {
							listIndex = Integer
									.parseInt(elt.getAttribute("value"));
						} catch (Exception e) {
						}
						result.append(
								(listElt.getTagName().equalsIgnoreCase("UL")
										? "*"
										: listIndex + ".") + " ");
						listIndicies.put(listElt, listIndex + 1);
					}
				}
				if (XmlUtils.isInvisibleContentElement(elt)&&!showInvisible) {
					boolean first = true;
					while (true) {
						n2 = walker.nextNode();
						if (n2 == null && first) {
							finished = true;
						}
						if (n2 == null || !XmlUtils.isAncestorOf(elt, n2)) {
							walker.previousNode();
							break;
						}
						first = false;
					}
				}
			} else {
				Node prSib = n2.getPreviousSibling();
				if (prSib != null && prSib.getNodeType() == Node.ELEMENT_NODE
						&& XmlUtils.isBlockHTMLElement((Element) prSib)) {
					markNewline(result, showNls);
				}
				result.append(n2.getNodeValue());
			}
		}
		String s = result.toString();
		if (flags == PRESERVE_WHITESPACE) {
			return s;
		}
		s = SEUtilities.normalizeWhitespace(s).trim();
		if (flags == 0) {
			return s;
		}
		s = expandNewlinesAndTabs(s, (flags & PRESERVE_TRAILING_NEWLINES) != 0);
		if ((flags & SHOW_NEWLINES_AS_DOUBLE) != 0) {
			s = s.replace("\n", "\n\n");
		}
		return s;
	}

	public void setBlockElementMatcher(Predicate<Element> blockElementMatcher) {
		this.blockElementMatcher = blockElementMatcher;
	}

	public void test() {
		String repl = String.format("%sword 1%s%sline 2 %s %s", NL_MARKER,
				NL_MARKER, NL_MARKER, NL_MARKER, NL_MARKER);
		String expandNewlinesAndTabs = expandNewlinesAndTabs(repl, false);
		System.out.println(expandNewlinesAndTabs);
	}

	private String expandNewlinesAndTabs(String s, boolean preserveTrailing) {
		// s = s.replace(nlMarker, "\n");
		// s = s.replace(tabMarker, "\t");
		// s = s.replaceAll("\\s*\\n+\\s*", "\n");
		int len = s.length();
		boolean doReplace = false;
		for (int i = 0; i < len; i++) {
			if (i + 5 <= len) {
				if (s.charAt(i) == NL_MARKER_ARR[0]
						&& s.charAt(i + 1) == NL_MARKER_ARR[1]
						&& s.charAt(i + 2) == NL_MARKER_ARR[2]
						&& s.charAt(i + 3) == NL_MARKER_ARR[3]) {
					if (s.charAt(i + 4) == NL_MARKER_ARR[4]
							|| s.charAt(i + 4) == TAB_MARKER_ARR[4]) {
						doReplace = true;
						break;
					}
				}
			}
		}
		if (!doReplace) {
			return s;
		}
		StringBuilder sb = new StringBuilder(s.length());
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			if (i + 5 <= len) {
				if (c == NL_MARKER_ARR[0] && s.charAt(i + 1) == NL_MARKER_ARR[1]
						&& s.charAt(i + 2) == NL_MARKER_ARR[2]
						&& s.charAt(i + 3) == NL_MARKER_ARR[3]) {
					if (s.charAt(i + 4) == NL_MARKER_ARR[4]) {
						i += 4;
						sb.append('\n');
						continue;
					}
					if (s.charAt(i + 4) == TAB_MARKER_ARR[4]) {
						i += 4;
						sb.append('\t');
						continue;
					}
				}
			}
			sb.append(c);
		}
		s = sb.toString();
		len = s.length();
		sb.delete(0, len);
		int idx = -1;
		boolean foundNl = false;
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			boolean ws = true;
			;
			switch (c) {
			case '\n':
				foundNl = true;
				// fallthrough OK
			case '\u0009':
			case '\u000B':
			case '\f':
			case '\r':
			case '\u00A0':
			case ' ':
				if (idx == -1) {
					idx = i;
				}
				break;
			default:
				ws = false;
			}
			if (idx != -1 && (!ws || i == len - 1)) {
				if (foundNl) {
					sb.append('\n');
					foundNl = false;
				} else {
					sb.append(s.substring(idx, i + (ws ? 1 : 0)));
				}
				idx = -1;
			}
			if (!ws) {
				sb.append(c);
			}
		}
		// Matcher matcher = Pattern.compile("\\n*(.*?)\\n*",
		// Pattern.DOTALL)
		// .matcher(s);
		// matcher.matches();
		// s = matcher.group(1);
		if (preserveTrailing) {
			return s;
		}
		s = sb.toString();
		len = s.length();
		idx = 0;
		int idy = s.length();
		for (; idx < len; idx++) {
			char c = s.charAt(idx);
			if (c != '\n') {
				break;
			}
		}
		for (; idy > 0; idy--) {
			char c = s.charAt(idy - 1);
			if (c != '\n') {
				break;
			}
		}
		if (idx == 0 && idy == s.length()) {
			return s;
		}
		if (idx > idy) {
			return "";
		}
		return s.substring(idx, idy);
	}

	@SuppressWarnings("unused")
	// optimised (no regex) version replaces this one
	private String expandNewlinesAndTabs1(String s) {
		s = s.replace(NL_MARKER, "\n");
		s = s.replace(tabMarker, "\t");
		s = s.replaceAll("\\s*\\n+\\s*", "\n");
		Matcher matcher = Pattern.compile("\\n*(.*?)\\n*", Pattern.DOTALL)
				.matcher(s);
		matcher.matches();
		s = matcher.group(1);
		return s;
	}

	private void markNewline(StringBuilder result, boolean showNls) {
		newLineInsertLocations.add(result.length());
		if (showNls) {
			result.append(NL_MARKER);
		}
	}

	private void popForNonParent(Element elt, Stack<Element> listParentStack) {
		while (!listParentStack.isEmpty()) {
			if (XmlUtils.isAncestorOf(listParentStack.peek(), elt)) {
				return;
			}
			listParentStack.pop();
		}
	}
}
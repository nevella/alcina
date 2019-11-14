package cc.alcina.framework.entity.parser.token;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.ranges.Range;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.XmlUtils.DOMLocation;

public class AbstractParserSlice<T extends ParserToken> {
	public static Range createRange(AbstractParserSlice first,
			AbstractParserSlice last) {
		return XmlUtils.DOMLocation.createRange(first.start, last.end);
	}

	public XmlUtils.DOMLocation start;

	public XmlUtils.DOMLocation end;

	public T token;

	/*
	 * Used to find the best slice (with the lowest start offset in the 'run' -
	 * i.e. the visible substring)
	 * 
	 * When matched, the startOffset of the context is moved to after the *end*
	 * of the match
	 */
	public int startOffsetInRun;

	private String cachedContents = null;

	private String overrideText;

	public AbstractParserSlice(Node node, T token) {
		this.start = new XmlUtils.DOMLocation(node, 0, 0);
		if (node.getNodeType() == Node.TEXT_NODE) {
			this.end = new XmlUtils.DOMLocation(node,
					node.getTextContent().length(), 0);
		} else {
			this.end = new XmlUtils.DOMLocation(node, 0, 0);
		}
		this.token = token;
	}

	public AbstractParserSlice(XmlUtils.DOMLocation start,
			XmlUtils.DOMLocation end, T token, int startOffsetInRun) {
		assert start != null : "start is null";
		this.start = start;
		this.end = end;
		this.token = token;
		this.startOffsetInRun = startOffsetInRun;
	}

	public String cleanedContents() {
		if (cachedContents == null) {
			cachedContents = SEUtilities.normalizeWhitespace(contents()).trim();
		}
		return cachedContents;
	}

	public String contents() {
		if (overrideText != null) {
			return overrideText;
		}
		Range r = createRange(this, this);
		String s = r.toString();
		r.detach();
		return s;
	}

	@Override
	public boolean equals(Object obj) {
		if (useObjectHashAndEquality()) {
			return super.equals(obj);
		}
		if (obj instanceof AbstractParserSlice) {
			AbstractParserSlice slice = (AbstractParserSlice) obj;
			return start.equals(slice.start) && end.equals(slice.end);
		}
		return super.equals(obj);
	}

	public void extend(Node n) {
		if (n instanceof Text) {
			Text text = (Text) n;
			this.end = new XmlUtils.DOMLocation(text, text.getLength(), 0);
		} else {
			this.end = new XmlUtils.DOMLocation(n, 0, 0);
		}
	}

	public void extend(Text text, String content) {
		this.end = new XmlUtils.DOMLocation(text, content.length(), 0);
	}

	public String extractTextForCitable() {
		String text = contents().trim();
		if (text.endsWith(".")) {
			text = text.substring(0, text.length() - 1);
		}
		return TokenParserUtils.quickNormalisePunctuation(text);
	}

	public Element getContainingBlock() {
		return XmlUtils.getContainingBlock(start.node);
	}

	public Element getContainingElement() {
		return XmlUtils.getContainingElement(start.node);
	}

	public Text getFirstText() {
		Node n = start.node;
		if (n.getNodeType() == Node.TEXT_NODE) {
			return (Text) n;
		}
		TreeWalker tw = ((DocumentTraversal) n.getOwnerDocument())
				.createTreeWalker(n.getOwnerDocument().getDocumentElement(),
						NodeFilter.SHOW_TEXT, null, true);
		tw.setCurrentNode(n);
		return (Text) tw.nextNode();
	}

	public String getOverrideText() {
		return overrideText;
	}

	public T getToken() {
		return this.token;
	}

	@Override
	public int hashCode() {
		return useObjectHashAndEquality() ? super.hashCode()
				: start.hashCode() ^ end.hashCode();
	}

	public String normalisedContents() {
		return TokenParserUtils.quickNormalisePunctuation(
				SEUtilities.normalizeWhitespaceAndTrim(contents()));
	}

	public void setOverrideText(String overrideText) {
		this.overrideText = overrideText;
	}

	@Override
	public String toString() {
		return token + ":" + contents();
	}

	public void trimTo(int length) {
		Document ownerDocument = start.node.getOwnerDocument();
		TreeWalker itr = ((DocumentTraversal) ownerDocument).createTreeWalker(
				ownerDocument.getDocumentElement(), NodeFilter.SHOW_TEXT, null,
				true);
		itr.setCurrentNode(getFirstText());
		StringBuilder content = new StringBuilder();
		Node n;
		while ((n = itr.getCurrentNode()) != null) {
			content.append(n.getNodeValue());
			if (content.length() >= length) {
				end = new DOMLocation(n, start.characterOffset
						+ n.getNodeValue().length() + length - content.length(),
						end.nodeIndex);
				// note - looks like DOMLocation.nodeIndex ain't that crucial
				// (since that info's implicit in the node)
				// may want to drop it?
				break;
			}
			itr.nextNode();
		}
	}

	public void walkToHighestNodeAtOffset() {
		if (start.characterOffset != 0) {
			return;
		}
		Node n = start.node;
		if (n.getNodeType() != Node.TEXT_NODE) {
			return;
		}
		TreeWalker tw = ((DocumentTraversal) n.getOwnerDocument())
				.createTreeWalker(n.getOwnerDocument().getDocumentElement(),
						NodeFilter.SHOW_TEXT | NodeFilter.SHOW_ELEMENT, null,
						true);
		tw.setCurrentNode(n);
		while (true) {
			n = tw.previousNode();
			if (n == null || n.getNodeType() == Node.TEXT_NODE
					|| n.getNodeName().equalsIgnoreCase("BODY")) {
				return;
			}
			start = new DOMLocation(n, start.characterOffset, start.nodeIndex);
		}
	}

	protected boolean useObjectHashAndEquality() {
		return true;
	}
}

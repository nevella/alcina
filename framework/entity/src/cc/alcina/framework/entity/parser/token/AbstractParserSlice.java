package cc.alcina.framework.entity.parser.token;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.html.HTMLAnchorElement;
import org.w3c.dom.ranges.Range;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.XmlUtils;

public class AbstractParserSlice<T extends ParserToken> {
	public static Range createRange(AbstractParserSlice first,
			AbstractParserSlice last) {
		return XmlUtils.DOMLocation.createRange(first.start, last.end);
	}

	public XmlUtils.DOMLocation start;

	public XmlUtils.DOMLocation end;

	public T token;

	public int startOffsetInRun;

	public HTMLAnchorElement anchor;

	

	private String cachedContents = null;

	private String overrideText;

	public AbstractParserSlice(XmlUtils.DOMLocation start, XmlUtils.DOMLocation end, T token,
			int startOffsetInRun) {
		assert start != null : "start is null";
		this.start = start;
		this.end = end;
		this.token = token;
		this.startOffsetInRun = startOffsetInRun;
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
				end.node = n;
				end.characterOffset = start.characterOffset
						+ n.getNodeValue().length() + length - content.length();
				break;
			}
			itr.nextNode();
		}
	}

	public AbstractParserSlice(Node node, T token) {
		this.start = new XmlUtils.DOMLocation(node, 0, 0);
		this.end = new XmlUtils.DOMLocation(node, 0, 0);
		this.token = token;
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

	public void extend(Node n) {
		if (n instanceof Text) {
			Text text = (Text) n;
			this.end = new XmlUtils.DOMLocation(text, text.getLength(), 0);
		} else {
			this.end = new XmlUtils.DOMLocation(n, 0, 0);
		}
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

	public String normalisedContents() {
		return TokenParserUtils.quickNormalisePunctuation(SEUtilities
				.normalizeWhitespaceAndTrim(contents()));
	}

	public void setOverrideText(String overrideText) {
		this.overrideText = overrideText;
	}

	@Override
	public String toString() {
		return token + ":" + contents();
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
			start.node = n;
		}
	}
}

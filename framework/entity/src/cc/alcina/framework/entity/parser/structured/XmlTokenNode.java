package cc.alcina.framework.entity.parser.structured;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.parser.structured.node.XmlNode;

public class XmlTokenNode {
	public XmlTokenNode(XmlNode node, XmlToken token) {
		this.sourceNode = node;
		this.token = token;
	}

	public List<XmlTokenNode> additionalSources = new ArrayList<>();

	public XmlNode sourceNode;

	public XmlNode targetNode;

	public XmlToken token;

	@Override
	public String toString() {
		return String.format("%s:\n%s\n%s", token, sourceNode,
				targetNode == null ? "" : targetNode);
	}

	public String sourceTextContent() {
		StringBuilder sb = new StringBuilder();
		sb.append(selfSourceTextContent());
		additionalSources.stream()
				.forEach(add -> sb.append(add.selfSourceTextContent()));
		return sb.toString();
	}

	private String selfSourceTextContent() {
		if (sourceNode == null) {
			return null;
		}
		String tokenContent = token.textContent(sourceNode);
		if (tokenContent != null) {
			return tokenContent;
		}
		return sourceNode.textContent();
	}

	public String normalisedAndTrimmed() {
		return SEUtilities.normalizeWhitespaceAndTrim(sourceTextContent());
	}
}

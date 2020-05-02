package cc.alcina.framework.entity.parser.structured;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.xml.XmlNode;
import cc.alcina.framework.entity.SEUtilities;

public class XmlStructuralJoin {
	public List<XmlStructuralJoin> additionalSources = new ArrayList<>();

	public XmlNode sourceNode;

	public XmlNode targetNode;

	public XmlToken token;

	private XmlTokenContext nodeContext;

	public XmlStructuralJoin(XmlNode node, XmlToken token) {
		this.sourceNode = node;
		this.token = token;
	}

	public XmlStructuralJoin copy() {
		return new XmlStructuralJoin(sourceNode, token);
	}

	public <T extends XmlTokenContext> T
			nodeContext(Supplier<T> newNodeContextSupplier) {
		if (nodeContext == null) {
			nodeContext = newNodeContextSupplier.get();
		}
		return (T) nodeContext;
	}

	public String normalisedAndTrimmed() {
		return SEUtilities.normalizeWhitespaceAndTrim(sourceTextContent());
	}

	public String selfSourceTextContent() {
		if (sourceNode == null) {
			return null;
		}
		String tokenContent = token.textContent(sourceNode);
		if (tokenContent != null) {
			return tokenContent;
		}
		return sourceNode.textContent();
	}

	public String sourceTextContent() {
		StringBuilder sb = new StringBuilder();
		sb.append(selfSourceTextContent());
		additionalSources.stream()
				.forEach(add -> sb.append(add.selfSourceTextContent()));
		return sb.toString();
	}

	@Override
	public String toString() {
		return String.format("%s:\ni:%s\no:%s", token, sourceNode,
				targetNode == null ? "" : targetNode);
	}
}

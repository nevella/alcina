package cc.alcina.framework.entity.parser.structured;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.parser.structured.node.XmlNode;

public class XmlStructuralJoin {
	public XmlStructuralJoin(XmlNode node, XmlToken token) {
		this.sourceNode = node;
		this.token = token;
	}

	public List<XmlStructuralJoin> additionalSources = new ArrayList<>();

	public XmlNode sourceNode;

	public XmlNode targetNode;

	public XmlToken token;
	
	private XmlTokenContext nodeContext;
	
	public <T extends XmlTokenContext> T nodeContext(Supplier<T> supplier){
		if(nodeContext==null){
			nodeContext=supplier.get();
		}
		return (T) nodeContext;
	}
	
	@Override
	public String toString() {
		return String.format("%s:\ni:%s\no:%s", token, sourceNode,
				targetNode == null ? "" : targetNode);
	}

	public String sourceTextContent() {
		StringBuilder sb = new StringBuilder();
		sb.append(selfSourceTextContent());
		additionalSources.stream()
				.forEach(add -> sb.append(add.selfSourceTextContent()));
		return sb.toString();
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

	public String normalisedAndTrimmed() {
		return SEUtilities.normalizeWhitespaceAndTrim(sourceTextContent());
	}

	public XmlStructuralJoin copy() {
		return new XmlStructuralJoin(sourceNode, token);
	}
}

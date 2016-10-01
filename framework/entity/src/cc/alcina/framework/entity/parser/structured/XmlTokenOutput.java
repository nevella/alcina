package cc.alcina.framework.entity.parser.structured;

import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.parser.structured.node.XmlDoc;
import cc.alcina.framework.entity.parser.structured.node.XmlNode;

public class XmlTokenOutput {
	private XmlDoc outDoc;

	private XmlNode writeCursor;

	public StructuredTokenParserContext context;

	public XmlTokenOutput(XmlDoc outDoc) {
		this.outDoc = outDoc;
		writeCursor = outDoc.root();
	}

	public void close(XmlTokenNode outNode, String tag) {
		if (!writeCursor.tagIs(tag)) {
			System.out.println(XmlUtils.prettyPrintWithDOM3LS(outDoc.domDoc()));
			throw new RuntimeException(
					String.format("closing unmatched tag : %s -> %s",
							writeCursor.name(), tag));
		}
		writeCursor.close = outNode;
		outNode.targetNode = writeCursor;
		writeCursor = writeCursor.parent();
	}

	public void open(XmlTokenNode outNode, String tag) {
		open(outNode, tag, new StringMap());
	}

	public void open(XmlTokenNode outNode, String tag, StringMap attrs) {
		writeCursor = writeCursor.builder().tag(tag).attrs(attrs).append();
		writeCursor.open = outNode;
		outNode.targetNode = writeCursor;
		context.targetNodeMapped(outNode);
	}

	XmlTokenNode getOutCursor() {
		return writeCursor.open;
	}

	public XmlNode getOutputNode() {
		return writeCursor;
	}

	public void tag(XmlTokenNode node, String tag) {
		open(node, tag);
		close(node, tag);
	}

	public void text(String text) {
		writeCursor.builder().text(text).append();
	}

	public String toXml() {
		return XmlUtils.streamXML(
				outDoc.domDoc().getDocumentElement().getFirstChild());
	}

	public void ensureOpen(XmlTokenNode outNode, String tag) {
		if (!writeCursor.ancestors().orSelf().has(tag)) {
			open(outNode, tag);
		}
	}

	public void ensureClosed(XmlTokenNode outNode, String tag) {
		if (writeCursor.tagIs(tag)) {
			close(outNode, tag);
		}
	}

	public void pi(String name, String data) {
		writeCursor.builder().processingInstruction().tag(name).text(data)
				.append();
	}
}

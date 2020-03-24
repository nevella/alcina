package cc.alcina.framework.entity.parser.structured;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.parser.structured.node.XmlDoc;
import cc.alcina.framework.entity.parser.structured.node.XmlNode;

public class XmlTokenOutput {
	private XmlDoc outDoc;

	private XmlNode writeCursor;

	public StructuredTokenParserContext context;

	private XmlNode lastTextNode;

	public boolean debug = false;

	public XmlTokenOutput(XmlDoc outDoc) {
		this.outDoc = outDoc;
		writeCursor = outDoc.root();
	}

	public void close(XmlStructuralJoin join, String tag) {
		close(join, tag, null, null);
	}

	public void close(XmlStructuralJoin join, String tag, String className,
			ClosedPatchHandler closedPatchHandler) {
		debug("close - %s %s", tag, join.hashCode());
		boolean invalidClose = !writeCursor.tagIs(tag);
		invalidClose |= (className != null
				&& !writeCursor.attrIs("class", className));
		if (invalidClose) {
			if (closedPatchHandler != null
					&& closedPatchHandler.permitInvalidClose(join, tag)) {
				return;
			}
			Ax.out("Node stack: closing unmatched tag : %s -> %s",
					nameAndClass(), tag);
			writeCursor.ancestors().orSelf().list()
					.forEach(n -> Ax.out(n.name()));
			outDoc.logPretty();
			System.err.println("see /tmp/log/log.xml for details");
			throw new RuntimeException(String.format(
					"closing unmatched tag : %s -> %s", nameAndClass(), tag));
		}
		writeCursor.close = join;
		join.targetNode = writeCursor;
		writeCursor = writeCursor.parent();
	}

	public void closeClass(XmlStructuralJoin join, String tag,
			String className) {
		close(join, tag, className, null);
	}

	public void debug(String template, Object... args) {
		if (!debug) {
			return;
		}
		FormatBuilder fb = new FormatBuilder();
		fb.indent(writeCursor.ancestors().orSelf().list().size() * 2);
		fb.format(template, args);
		Ax.out(fb);
	}

	public boolean ensureClosed(XmlStructuralJoin join, String tag) {
		return ensureClosedClass(join, tag, null);
	}

	public boolean ensureClosedClass(XmlStructuralJoin join, String tag,
			String className) {
		if (writeCursor.tagIs(tag) && (className == null
				|| writeCursor.attrIs("class", className))) {
			close(join, tag);
			return true;
		} else {
			return false;
		}
	}

	public void ensureOpen(XmlStructuralJoin join, String tag) {
		ensureOpenClass(join, tag, null);
	}

	public void ensureOpenClass(XmlStructuralJoin join, String tag,
			String className) {
		if (!isOpen(tag, className)) {
			open(join, tag);
		}
	}

	public boolean isOpen(String tag, String className) {
		return writeCursor.ancestors().orSelf().list().stream().anyMatch(c -> c
				.tagIs(tag)
				&& (className == null || c.attrIs("class", className)));
	}

	public XmlNode getLastTextNode() {
		return this.lastTextNode;
	}

	public XmlDoc getOutDoc() {
		return this.outDoc;
	}

	public XmlNode getOutputNode() {
		return writeCursor;
	}

	public void open(XmlStructuralJoin join, String tag) {
		open(join, tag, new StringMap());
	}

	public void open(XmlStructuralJoin join, String tag, StringMap attrs) {
		debug("open - %s - %s - %s", tag, join.hashCode(), attrs);
		writeCursor = writeCursor.builder().tag(tag).attrs(attrs).append();
		writeCursor.open = join;
		join.targetNode = writeCursor;
		context.targetNodeMapped(join);
	}

	public void openClass(XmlStructuralJoin join, String tag,
			String className) {
		open(join, tag, StringMap.property("class", className));
	}

	public void pi(String name, String data) {
		writeCursor.builder().processingInstruction().tag(name).text(data)
				.append();
	}

	public void tag(XmlStructuralJoin node, String tag) {
		open(node, tag);
		close(node, tag);
	}

	public void text(String text) {
		if (text.isEmpty()) {
			return;
		}
		debug("text - %s",
				CommonUtils.trimToWsCharsMiddle(text, 80).replace("\n", "\\n"));
		this.lastTextNode = writeCursor.builder().text(text).append();
	}

	public String toXml() {
		return XmlUtils.streamXML(
				outDoc.domDoc().getDocumentElement().getFirstChild());
	}

	public void writeXml(String xmlString) {
		XmlDoc insert = new XmlDoc(xmlString);
		XmlNode documentElementNode = insert.getDocumentElementNode();
		if (documentElementNode.tagIs("strip")) {
			documentElementNode.children.nodes()
					.forEach(writeCursor.children::importFrom);
		} else {
			writeCursor.children.importFrom(documentElementNode);
		}
	}

	protected String nameAndClass() {
		return writeCursor.has("class")
				? Ax.format("%s.%s", writeCursor.name(),
						writeCursor.getClassName())
				: writeCursor.name();
	}

	XmlStructuralJoin getOutCursor() {
		return writeCursor.open;
	}
}

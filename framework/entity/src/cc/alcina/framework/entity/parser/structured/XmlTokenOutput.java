package cc.alcina.framework.entity.parser.structured;

import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeDebugSupport;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.XmlUtils;

public class XmlTokenOutput implements DomNodeDebugSupport {
	private DomDoc outDoc;

	private DomNode writeCursor;

	public StructuredTokenParserContext context;

	private DomNode lastTextNode;

	public boolean debug = false;

	private CachingMap<DomNode, DomNodeDebugInfo> debugMap = new CachingMap<>(
			DomNodeDebugInfo::new);

	public XmlTokenOutput(DomDoc outDoc) {
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
		debugMap.get(writeCursor).close = join;
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

	public DomNode getLastTextNode() {
		return this.lastTextNode;
	}

	public DomDoc getOutDoc() {
		return this.outDoc;
	}

	public DomNode getOutputNode() {
		return writeCursor;
	}

	public boolean isOpen(String tag, String className) {
		return writeCursor.ancestors().orSelf().list().stream()
				.anyMatch(c -> c.tagIs(tag)
						&& (className == null || c.attrIs("class", className)));
	}

	public void open(XmlStructuralJoin join, String tag) {
		open(join, tag, new StringMap());
	}

	public void open(XmlStructuralJoin join, String tag, StringMap attrs) {
		debug("open - %s - %s - %s", tag, join.hashCode(), attrs);
		writeCursor = writeCursor.builder().tag(tag).attrs(attrs).append();
		debugMap.get(writeCursor).open = join;
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

	@Override
	public String shortRepresentation(DomNode node) {
		String out = "";
		if (node.isElement()) {
			DomNodeDebugInfo debugInfo = debugMap.get(node);
			if (debugInfo.close != null && debugInfo.open != null) {
				out = String.format("<%s />", node.name());
			} else if (debugInfo.close != null) {
				out = String.format("</%s>", node.name());
			} else {
				out = String.format("<%s>", node.name());
			}
		}
		return out;
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
		DomDoc insert = new DomDoc(xmlString);
		DomNode documentElementNode = insert.getDocumentElementNode();
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
		return debugMap.get(writeCursor).open;
	}

	public static class DomNodeDebugInfo {
		DomNode node;

		XmlStructuralJoin open;

		XmlStructuralJoin close;

		public DomNodeDebugInfo(DomNode node) {
			this.node = node;
		}
	}
}

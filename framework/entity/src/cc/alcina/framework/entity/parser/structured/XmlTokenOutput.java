package cc.alcina.framework.entity.parser.structured;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
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
        close(join, tag, null);
    }

    public void close(XmlStructuralJoin join, String tag,
            ClosedPatchHandler closedPatchHandler) {
        if (debug) {
            System.out.format("close - %s %s\n", tag, join.hashCode());
        }
        if (!writeCursor.tagIs(tag)) {
            if (closedPatchHandler != null
                    && closedPatchHandler.permitInvalidClose(join, tag)) {
                return;
            }
            Ax.out("Node stack: closing unmatched tag : %s -> %s",
                    writeCursor.name(), tag);
            writeCursor.ancestors().list().forEach(n -> Ax.out(n.name()));
            outDoc.logPretty();
            System.err.println("see /tmp/log/log.xml for details");
            throw new RuntimeException(
                    String.format("closing unmatched tag : %s -> %s",
                            writeCursor.name(), tag));
        }
        writeCursor.close = join;
        join.targetNode = writeCursor;
        writeCursor = writeCursor.parent();
    }

    public void ensureClosed(XmlStructuralJoin join, String tag) {
        if (writeCursor.tagIs(tag)) {
            close(join, tag);
        }
    }

    public void ensureOpen(XmlStructuralJoin join, String tag) {
        if (!writeCursor.ancestors().orSelf().has(tag)) {
            open(join, tag);
        }
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
        if (debug) {
            System.out.format("open - %s - %s - %s\n", tag, join.hashCode(),
                    attrs);
        }
        writeCursor = writeCursor.builder().tag(tag).attrs(attrs).append();
        writeCursor.open = join;
        join.targetNode = writeCursor;
        context.targetNodeMapped(join);
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
        if (debug) {
            System.out.format("text - %s \n",
                    CommonUtils.trimToWsCharsMiddle(text, 80));
        }
        if (text.matches(".*s able to.*understand.*")) {
            int debug = 3;
        }
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

    XmlStructuralJoin getOutCursor() {
        return writeCursor.open;
    }
}

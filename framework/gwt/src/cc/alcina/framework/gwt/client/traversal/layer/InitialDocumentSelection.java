package cc.alcina.framework.gwt.client.traversal.layer;

import cc.alcina.framework.gwt.client.dom.DomDocument;
import cc.alcina.framework.common.client.process.TreeProcess;

public abstract class InitialDocumentSelection extends DocumentSelection {
	public InitialDocumentSelection(TreeProcess.Node parentNode,
			String markup) {
		this(parentNode, DomDocument.from(markup));
	}

	public InitialDocumentSelection(TreeProcess.Node parentNode,
			DomDocument document) {
		super(parentNode, document);
	}
}
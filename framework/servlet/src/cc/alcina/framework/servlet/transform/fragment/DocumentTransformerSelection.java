package cc.alcina.framework.servlet.transform.fragment;

import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentRoot;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;

/**
 * The root of the transformer selection tree, provides access to resolved
 * DomNode styles
 *
 * 
 *
 */
public class DocumentTransformerSelection
		extends AbstractSelection<DocumentTransformerPeer>
		implements LayoutTokens.RootToken.HasFragmentDocumentRoot,
		StyledDocument.AccessorSelection {
	public DocumentTransformerSelection(Node parentNode,
			DocumentTransformerPeer peer) {
		super(parentNode, peer, peer.getClass().getSimpleName());
	}

	public <T extends DocumentTransformerPeer> T peer() {
		return (T) get();
	}

	@Override
	public FragmentDocumentRoot provideRoot() {
		return peer().provideRoot();
	}

	@Override
	public StyledDocument getStyledDocument() {
		return peer().transformer.styledDocument;
	}
}
